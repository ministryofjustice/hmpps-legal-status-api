package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataAccessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.legalstatusapi.client.personlocation.PeriodOfCustodyNotFoundException
import uk.gov.justice.digital.hmpps.legalstatusapi.client.personlocation.PersonLocationApiClient
import uk.gov.justice.digital.hmpps.legalstatusapi.client.prisonersearch.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.legalstatusapi.client.remandandsentencing.RemandAndSentencingApiClient
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.entity.ImprisonmentStatusCalculatedAtEntity
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.repository.ImprisonmentStatusCalculatedAtRepository
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.Sentence.LegacySentence
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.Sentence.RasSentence
import uk.gov.justice.digital.hmpps.personlocation.resource.model.PeriodOfCustody
import uk.gov.justice.digital.hmpps.prisonersearch.resource.model.Prisoner
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonCharge
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonSentence.Status.ACTIVE
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.SentenceTypeDetails
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private val log = KotlinLogging.logger {}

/**
 * Returned by [LegacyDataService.calculateImprisonmentStatus] when the Imprisonment Status was not calculated.
 */
const val NOT_CALCULATED = "NOT_CALCULATED"

/**
 * Service class for handling NOMIS legacy data operations.
 */
@Service
class LegacyDataService(
  @param:Value("\${imprisonment-status.duration-before-recalculation-required:10s}") private val durationBeforeRecalculationRequired: Duration,
  private val imprisonmentStatusCalculatedAtRepository: ImprisonmentStatusCalculatedAtRepository,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val personLocationApiClient: PersonLocationApiClient,
  private val remandAndSentencingApiClient: RemandAndSentencingApiClient,
  private val sentenceTypeDetailsProvider: SentenceTypeDetailsProvider,
  private val clock: Clock,
  private val imprisonmentStatusCalculator: ImprisonmentStatusCalculator,
  private val jsonMapper: JsonMapper,
  private val transactionTemplate: TransactionTemplate,
) {

  /**
   * Calculates the NOMIS legacy Imprisonment Status for the prisoner identified by their prison number.
   *
   * @param prisonNumber The prison number of the prisoner.
   * @return The calculated Imprisonment Status's NOMIS code, or [NOT_CALCULATED] if the status was not calculated (see
   *   step 3.1 of [calculate]).
   */
  fun calculateImprisonmentStatus(prisonNumber: String): String = calculate(prisonNumber).calculation?.imprisonmentStatus?.nomisCode ?: NOT_CALCULATED

  /**
   * Calculates the NOMIS legacy Imprisonment Status for the prisoner identified by their prison number, and returns it
   * with the inputs used and the Imprisonment Status that NOMIS holds.
   *
   * Steps:
   * 1. Retrieve the prisoner from Prisoner Search, for the Imprisonment Status that NOMIS holds and the bookingId
   * 2. Get the prisoner's latest period of custody from the Person Location API, and select the start of the date
   *    range for RaS from it (see [chargesWindowStart]). If there is no period of custody, throw
   *    [PeriodOfCustodyNotFoundException]
   * 3. Get the prisoner's sentence(s) and offence(s) from RaS, using the prisonNumber to identify the prisoner along
   *    with a date range comprised of the date from step 2 (if any) to current date
   *    1. If RaS returns no charges, do not calculate: release the lock record and return a result with no
   *       calculation (see [NotCalculatedReason.NO_RAS_CHARGES] and the assumption below)
   * 4. Choose the sentence that determines the status: lowest status rank, then longest, then most severe offence
   *    1. If there are no sentences (or none has a mapping), choose the main offence instead: lowest status rank (from
   *       its outcome code), then most severe offence
   * 5. Use the sentenceCategory and sentenceCalcType, or the offence's outcomeCode, to look up the Imprisonment Status
   *    Mapping
   * 6. Return the Imprisonment Status from the Imprisonment Status Mapping record (or UNKNOWN)
   *
   * Steps 4 to 6 are implemented by [ImprisonmentStatusCalculator].
   *
   * ASSUMPTION (IS-181): an empty list of charges from RaS is never valid. A prisoner in custody always has at least
   * one charge, so an empty response means that the RaS data is not complete yet (for example, RaS has not processed
   * all the events yet). It does not mean that the prisoner has no charges. Thus, in step 3.1 we do not calculate, as
   * the calculation would give UNKNOWN and a false mismatch with NOMIS.
   * This assumption should be challenged if one of these turns out to give an empty list for a prisoner who is correctly in custody:
   * - The RaS `fromDate` window leaves out charges dated before it.
   * - RaS returns only charges on ACTIVE court cases (its default `statuses`).
   * - People held without criminal charges (e.g. NOMIS statuses DET, CIVIL, A_CFINE, CIV_RMD) may have no charge in
   *   RaS. This has not been checked.
   * If the assumption is wrong, change step 3.1 so that it calculates again (the calculator gives UNKNOWN for no charges).
   *
   * @param prisonNumber The prison number of the prisoner.
   * @return The calculation, its inputs, and the NOMIS Imprisonment Status. The calculation is null if RaS returned no
   *   charges (step 3.1).
   * @throws DataAccessException subclasses when creating or updating the Imprisonment Status Calculated At,
   *   typically caused by multiple threads/consumers trying to update the record at the same time.
   * @throws RecentImprisonmentStatusExistsException
   */
  fun calculate(prisonNumber: String): LegacyImprisonmentStatusResult {
    log.info { "Starting Imprisonment Status calculation for [$prisonNumber]" }

    // Optimistic locking on the prisoner's ImprisonmentStatusCalculatedAtEntity record
    // We need to keep state of the previous calculatedAt value so that in the catch for the main functionality of this
    // method we can revert it back if we need to.
    val previousCalculatedAt: Instant? = transactionTemplate.execute {
      imprisonmentStatusCalculatedAtRepository.findByIdOrNull(prisonNumber)
        ?.also {
          // Prisoner has an ImprisonmentStatusCalculatedAtEntity - check when it was last calculated
          if (it.isWithin(durationBeforeRecalculationRequired)) {
            // Imprisonment Status was calculated recently, within the calculation TTL
            throw RecentImprisonmentStatusExistsException(prisonNumber, it.calculatedAt)
          }
          // Update when it was last calculated
          val previousCalculatedAt = it.calculatedAt
          it.calculatedAt = Instant.now(clock)
          imprisonmentStatusCalculatedAtRepository.save(it)
          return@execute previousCalculatedAt // return the previous calculatedAt value
        }

      // The prisoner does not yet have an ImprisonmentStatusCalculatedAtEntity. Create a new one
      imprisonmentStatusCalculatedAtRepository.save(
        ImprisonmentStatusCalculatedAtEntity(
          prisonNumber = prisonNumber,
          calculatedAt = Instant.now(clock),
        ),
      )
      return@execute null // return null as the previous version of the record
    }
    log.debug { "Optimistic lock acquired for Imprisonment Status calculation for [$prisonNumber]" }

    val result = try {
      calculateWhileLocked(prisonNumber)
    } catch (e: Exception) {
      // An exception was thrown during the calculation. Revert the previous state of the ImprisonmentStatusCalculatedAtEntity
      releaseCalculatedAt(prisonNumber, previousCalculatedAt)
      throw e // rethrow the exception
    }

    if (result.calculation == null) {
      // Step 3.1 - the status was not calculated, so release the lock record. The RaS data may be complete for the next
      // event, which must not be skipped as a recent calculation.
      // This is done outside the try, so that the catch cannot release the lock record a second time.
      releaseCalculatedAt(prisonNumber, previousCalculatedAt)
      log.info { "Released the Imprisonment Status Calculated At lock for [$prisonNumber] as the Imprisonment Status was not calculated" }
    }

    return result
  }

  /**
   * Steps 1 to 6 of [calculate], done while the prisoner's ImprisonmentStatusCalculatedAtEntity is locked.
   */
  private fun calculateWhileLocked(prisonNumber: String): LegacyImprisonmentStatusResult {
    // Step 1
    val prisoner = getPrisoner(prisonNumber)

    // Step 2
    val periodOfCustody = getLatestPeriodOfCustody(prisonNumber)
    val chargesWindowStart = periodOfCustody?.let { chargesWindowStart(it) }
      .also {
        log.info { "Selected charges window start (RaS fromDate) [${it ?: "none"}] for prisoner [$prisonNumber] (${if (it == null) "no previous period of custody" else "day after the previous period of custody concluded"})" }
      }

    // Step 3
    val prisonerCharges = remandAndSentencingApiClient.getPrisonerCharges(
      prisonNumber = prisonNumber,
      fromDate = chargesWindowStart,
      toDate = LocalDate.now(clock),
    )

    // Step 3.1 - see the ASSUMPTION (IS-181) in the KDoc: an empty list of charges is never valid, so do not calculate
    if (prisonerCharges.charges.isEmpty()) {
      log.warn { "RaS returned no charges for prisoner [$prisonNumber] (fromDate [${chargesWindowStart ?: "none"}], toDate [${LocalDate.now(clock)}], bookingId [${prisoner.bookingId}]). Imprisonment Status not calculated. NOMIS Imprisonment Status is [${prisoner.imprisonmentStatus}]" }
      return LegacyImprisonmentStatusResult(
        prisonNumber = prisonNumber,
        nomisImprisonmentStatus = prisoner.imprisonmentStatus,
        receptionDate = prisoner.receptionDate,
        previousPeriodConcludedAt = periodOfCustody?.previousPeriod?.concludedAt,
        chargesWindowStart = chargesWindowStart,
        sentencingData = LegacySentencingData(sentences = emptyList(), offences = emptyList()),
        calculation = null,
        rasData = rasData(prisonerCharges.rawJson, emptyMap()),
        calculatedAt = Instant.now(clock),
        bookingId = prisoner.bookingId,
      )
    }

    // The sentence types looked up, recorded so that they can be stored with a mismatched calculation
    val sentenceTypeDetailsByUuid = mutableMapOf<UUID, SentenceTypeDetails>()
    val sentences = prisonerCharges.charges.mapNotNull { it.sentence(sentenceTypeDetailsByUuid) }

    // The charges array contains charges, each with a single outcome. If a charge has multiple outcomes
    // there will be multiple charges with the same chargeUuid, one for each of the outcomes.
    // To get the list of offences we need to group all charges by the chargeUuid.
    // For each chargeUuid we will then have a list of charges.
    // The NOMIS algorithm for Imprisonment Status uses the first outcome for each. In order to use the same outcome
    // as NOMIS, we need to sort the outcomes by `appearanceDate` and `appearanceCreatedAt` and then select the first.
    // This will give us the outcome code that we need for the Imprisonment Status mapping.
    // NOTE: Selecting the first outcome per charge is current NOMIS behaviour. If there is a decision to change this
    // logic to use most recent outcome per charge this is where we would do it, but it would be a deviation from the
    // current NOMIS algorithm.
    val offences = prisonerCharges.charges
      .groupBy { it.chargeUuid }
      .mapValues { (_, list) ->
        list.sortedWith(
          compareBy<PersonCharge, LocalDate>(reverseOrder()) { it.appearanceDate }
            .thenBy(reverseOrder()) { it.appearanceCreatedAt },
        )
          .first()
      }
      .values
      .map { it.offence() }

    val sentencingData = LegacySentencingData(
      sentences = sentences,
      offences = offences,
    )

    // Steps 4 to 6
    val calculation = imprisonmentStatusCalculator.calculate(sentencingData)

    return LegacyImprisonmentStatusResult(
      prisonNumber = prisonNumber,
      nomisImprisonmentStatus = prisoner.imprisonmentStatus,
      receptionDate = prisoner.receptionDate,
      previousPeriodConcludedAt = periodOfCustody?.previousPeriod?.concludedAt,
      chargesWindowStart = chargesWindowStart,
      sentencingData = sentencingData,
      calculation = calculation,
      rasData = rasData(prisonerCharges.rawJson, sentenceTypeDetailsByUuid),
      calculatedAt = Instant.now(clock),
      bookingId = prisoner.bookingId,
    )
  }

  /**
   * Reverts the prisoner's ImprisonmentStatusCalculatedAtEntity to its state before [calculate] updated it, so that the
   * next event for the prisoner is not skipped as a recent calculation.
   *
   * @param previousCalculatedAt The calculatedAt before the update, or null if [calculate] created the record.
   */
  private fun releaseCalculatedAt(prisonNumber: String, previousCalculatedAt: Instant?) {
    transactionTemplate.execute {
      if (previousCalculatedAt != null) {
        // Get the current record and set it's calculatedAt date back to the previous value
        imprisonmentStatusCalculatedAtRepository.getReferenceById(prisonNumber).also {
          it.calculatedAt = previousCalculatedAt
          imprisonmentStatusCalculatedAtRepository.save(it)
        }
      } else {
        // There was no previous record so we should delete the record created earlier
        imprisonmentStatusCalculatedAtRepository.deleteById(prisonNumber)
      }
    }
  }

  private fun getPrisoner(prisonNumber: String): Prisoner = prisonerSearchApiClient.getPrisoner(prisonNumber)
    .also {
      log.info { "Retrieved prisoner [$prisonNumber] from Prisoner Search API" }
    }

  private fun getLatestPeriodOfCustody(prisonNumber: String): PeriodOfCustody? = try {
    personLocationApiClient.getLatestPeriodOfCustody(prisonNumber)
      .also {
        log.info { "Retrieved latest period of custody for prisoner [$prisonNumber] from Person Location API" }
      }
  } catch (e: PeriodOfCustodyNotFoundException) {
    log.info { "No period of custody returned for prisoner [$prisonNumber] from Person Location API" }
    null
  }

  // The raw charges response is read as a tree rather than into our models, so that no fields are lost
  private fun rasData(rawPrisonerCharges: String, sentenceTypeDetailsByUuid: Map<UUID, SentenceTypeDetails>): String = jsonMapper.writeValueAsString(
    mapOf(
      "personCharges" to jsonMapper.readTree(rawPrisonerCharges),
      "sentenceTypeDetails" to sentenceTypeDetailsByUuid,
    ),
  )

  /**
   * Returns a [LegacySentence] or null from a Ras [PersonCharge]
   *
   * If the Charge has a ACTIVE Sentence, the primary strategy should be to create a LegacySentence by looking up the sentenceType
   * with the [SentenceTypeDetailsProvider] (which caches the RaS sentence reference data).
   * If Sentence does not have a sentenceType property, the LegacySentence should be created from the legacyData property.
   *
   * Each sentence type looked up is also put in [sentenceTypeDetailsByUuid], so that the sentence types used in the
   * calculation can be recorded.
   *
   * Either way, the sentence length is derived from the sentence's period lengths (see [SentenceLength]).
   */
  private fun PersonCharge.sentence(sentenceTypeDetailsByUuid: MutableMap<UUID, SentenceTypeDetails>): Sentence? {
    val personSentence = if (sentence?.status == ACTIVE) {
      sentence
    } else {
      return null.also { log.debug { "Ignoring null or non-active sentence" } }
    }

    val sentenceLengthDays = SentenceLength.inDays(personSentence.periodLengths)
    return personSentence.sentenceType?.sentenceTypeUuid?.let { sentenceTypeUuid ->
      sentenceTypeDetailsByUuid.getOrPut(sentenceTypeUuid) { sentenceTypeDetailsProvider.getSentenceTypeDetails(sentenceTypeUuid) }.let {
        RasSentence(
          classification = it.classification,
          legacySentenceCalcType = if (personSentence.hasRecall && personSentence.legacyData?.sentenceCalcType != null) personSentence.legacyData.sentenceCalcType else it.nomisSentenceCalcType,
          legacySentenceCategory = if (personSentence.hasRecall && personSentence.legacyData?.sentenceCategory != null) personSentence.legacyData.sentenceCategory else it.nomisCjaCode,
          offence = offence(),
          sentenceLengthDays = sentenceLengthDays,
          latestRecallType = sentence.latestRecallType,
        ).also {
          log.debug { "LegacySentence mapped from looking up RaS sentenceType" }
        }
      }
    } ?: personSentence.legacyData?.let { legacyData ->
      LegacySentence(
        sentenceCalcType = legacyData.sentenceCalcType!!,
        sentenceCategory = legacyData.sentenceCategory!!,
        offence = offence(),
        sentenceLengthDays = sentenceLengthDays,
      ).also {
        log.debug { "LegacySentence mapped using legacy data fields from RaS response" }
      }
    }
  }

  /**
   * Returns a [Offence] from a RaS [PersonCharge], sentenced or not. NOMIS uses every charge on the active court cases
   * (not just the unsentenced ones) when the prisoner has no mapped sentences (step 3.1).
   *
   * The outcome code is the NOMIS code of the DPS charge outcome or, if the outcome is not supported by DPS, the legacy
   * NOMIS outcome code.
   */
  private fun PersonCharge.offence() = Offence(
    offenceCode = offenceCode,
    outcomeCode = outcomeCode(),
  )

  // Logs where the outcome code came from, to help investigate calculations that do not match NOMIS
  private fun PersonCharge.outcomeCode(): String? {
    outcome?.let {
      log.debug { "Outcome code [${it.nomisCode}] for charge [$chargeUuid] mapped from the RaS outcome" }
      return it.nomisCode
    }
    legacyData?.nomisOutcomeCode?.let {
      log.debug { "Outcome code [$it] for charge [$chargeUuid] mapped using legacy data fields from RaS response" }
      return it
    }
    log.debug { "No outcome code for charge [$chargeUuid]" }
    return null
  }

  private fun ImprisonmentStatusCalculatedAtEntity.isWithin(threshold: Duration): Boolean = Duration.between(this.calculatedAt, Instant.now(clock)).abs().toMillis() <= threshold.toMillis()
}
