package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import mu.KotlinLogging
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.legalstatusapi.client.manageoffences.ManageOffencesApiClient
import uk.gov.justice.digital.hmpps.legalstatusapi.config.CacheConfiguration.Companion.OFFENCE_SEVERITIES

private val log = KotlinLogging.logger {}

/**
 * Provides the NOMIS severity ranking of offences. A lower number means a more severe offence.
 */
interface OffenceSeverityProvider {

  /**
   * @return A map of offence code to severity ranking. Offence codes with no known severity are not in the map.
   */
  fun getSeverities(offenceCodes: Collection<String>): Map<String, Int>
}

/**
 * Gets offence severities from Manage Offences.
 *
 * Severities are reference data, so are cached (see CacheConfiguration). They are cached per offence code rather than
 * with `@Cacheable` on [getSeverities], because that would use the whole list of a prisoner's offence codes as the key,
 * which would rarely be the same for 2 prisoners. Only offence codes that are not already cached are requested from
 * Manage Offences. Offences that Manage Offences has no severity for are cached too (as null), so that they are not
 * requested every time.
 */
@Component
class ManageOffencesOffenceSeverityProvider(
  private val manageOffencesApiClient: ManageOffencesApiClient,
  private val cacheManager: CacheManager,
) : OffenceSeverityProvider {

  override fun getSeverities(offenceCodes: Collection<String>): Map<String, Int> {
    if (offenceCodes.isEmpty()) return emptyMap()

    // Sorted so that the request is deterministic
    val distinctOffenceCodes = offenceCodes.distinct().sorted().toSet()
    val cache = cacheManager.getCache(OFFENCE_SEVERITIES)!!

    // A cached null value means that Manage Offences has no severity for the offence
    val cachedSeverities: Map<String, Int?> = distinctOffenceCodes
      .mapNotNull { offenceCode -> cache.get(offenceCode)?.let { offenceCode to it.get() as Int? } }
      .toMap()

    val uncachedOffenceCodes = distinctOffenceCodes - cachedSeverities.keys
    val requestedSeverities: Map<String, Int?> = if (uncachedOffenceCodes.isEmpty()) {
      emptyMap()
    } else {
      requestSeverities(uncachedOffenceCodes).let { severities -> uncachedOffenceCodes.associateWith { severities[it] } }
        .onEach { (offenceCode, severity) -> cache.put(offenceCode, severity) }
    }

    val severities = (cachedSeverities + requestedSeverities)
      .mapNotNull { (offenceCode, severity) -> severity?.let { offenceCode to it } }
      .toMap()

    /*
    TODO - Agree what to do about offences with no severity. Manage Offences returns nothing for some NOMIS offences,
      most notably the statute 'ZZ' ("NOMIS Legacy Migration") offences such as SX56025-029N, which NOMIS does have a
      severity for. On NOMIS T3 about 10% of active bookings have a charge for one of these offences. For now they are
      treated as having no severity, which can produce a different result to NOMIS.
     */
    (distinctOffenceCodes - severities.keys).takeIf { it.isNotEmpty() }?.also {
      log.warn { "No severity found in Manage Offences for offences $it" }
    }

    return severities
  }

  private fun requestSeverities(offenceCodes: Set<String>): Map<String, Int> = manageOffencesApiClient.getOffenceSeverities(offenceCodes)
    .mapNotNull { rating ->
      rating.severityRating.toIntOrNull()
        ?.let { rating.offenceCode to it }
        .also { if (it == null) log.warn { "Ignoring non-numeric severity [${rating.severityRating}] for offence [${rating.offenceCode}]" } }
    }
    .toMap()
}
