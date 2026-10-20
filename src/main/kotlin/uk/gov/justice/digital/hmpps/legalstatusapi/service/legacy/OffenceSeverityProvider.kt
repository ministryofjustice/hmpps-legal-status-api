package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import mu.KotlinLogging
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.legalstatusapi.client.ManageOffencesApiClient

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

@Component
class ManageOffencesOffenceSeverityProvider(
  private val manageOffencesApiClient: ManageOffencesApiClient,
) : OffenceSeverityProvider {

  override fun getSeverities(offenceCodes: Collection<String>): Map<String, Int> {
    if (offenceCodes.isEmpty()) return emptyMap()

    // Sorted so that the request is deterministic
    val distinctOffenceCodes = offenceCodes.distinct().sorted()
    val severities = manageOffencesApiClient.getOffenceSeverities(distinctOffenceCodes)
      .mapNotNull { rating ->
        rating.severityRating.toIntOrNull()
          ?.let { rating.offenceCode to it }
          .also { if (it == null) log.warn { "Ignoring non-numeric severity [${rating.severityRating}] for offence [${rating.offenceCode}]" } }
      }
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
}
