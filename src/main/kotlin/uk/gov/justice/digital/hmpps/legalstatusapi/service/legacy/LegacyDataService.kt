package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataAccessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.legalstatusapi.client.prisonersearch.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.legalstatusapi.client.remandandsentencing.RemandAndSentencingApiClient
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.entity.ImprisonmentStatusCalculatedAtEntity
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.repository.ImprisonmentStatusCalculatedAtRepository
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.Sentence.LegacySentence
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.Sentence.RasSentence
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
 * Service class for handling NOMIS legacy data operations.
 */
@Service
class LegacyDataService(
  @param:Value("\${imprisonment-status.duration-before-recalculation-required:10s}") private val durationBeforeRecalculationRequired: Duration,
  private val imprisonmentStatusCalculatedAtRepository: ImprisonmentStatusCalculatedAtRepository,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
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
   * @return The calculated Imprisonment Status's NOMIS code.
   */
  fun calculateImprisonmentStatus(prisonNumber: String): String = calculate(prisonNumber).calculation.imprisonmentStatus.nomisCode

  /**
   * Calculates the NOMIS legacy Imprisonment Status for the prisoner identified by their prison number, and returns it
   * with the inputs used and the Imprisonment Status that NOMIS holds.
   *
   * Steps:
   * 1. Retrieve the prisoner's receptionDate (which we assume at this point to be the booking start date)
   * 2. Get the prisoner's sentence(s) and offence(s) from RaS, using the prisonNumber to identify the prisoner along
   *    with a date range comprised of receptionDate to current date
   * 3. Choose the sentence that determines the status: lowest status rank, then longest, then most severe offence
   *    1. If there are no sentences (or none has a mapping), choose the main offence instead: lowest status rank (from
   *       its outcome code), then most severe offence
   * 4. Use the sentenceCategory and sentenceCalcType, or the offence's outcomeCode, to look up the Imprisonment Status
   *    Mapping
   * 5. Return the Imprisonment Status from the Imprisonment Status Mapping record (or UNKNOWN)
   *
   * Steps 3 to 5 are implemented by [ImprisonmentStatusCalculator].
   *
   * @param prisonNumber The prison number of the prisoner.
   * @return The calculation, its inputs, and the NOMIS Imprisonment Status.
   * @throws DataAccessException subclasses when creating or updating the Imprisonment Status Calculated At,
   *   typically caused by multiple threads/consumers trying to update the record at the same time.
   * @throws RecentImprisonmentStatusExistsException
   */
  fun calculate(prisonNumber: String): LegacyImprisonmentStatusResult {
    transactionTemplate.execute {
      imprisonmentStatusCalculatedAtRepository.findByIdOrNull(prisonNumber)
        ?.also {
          if (it.isWithin(durationBeforeRecalculationRequired)) {
            // Imprisonment Status was calculated recently, within the calculation TTL
            throw RecentImprisonmentStatusExistsException(prisonNumber, it.calculatedAt)
          }
          it.calculatedAt = Instant.now(clock)
          imprisonmentStatusCalculatedAtRepository.save(it)
        }
        ?: imprisonmentStatusCalculatedAtRepository.save(
          ImprisonmentStatusCalculatedAtEntity(
            prisonNumber = prisonNumber,
            calculatedAt = Instant.now(clock),
          ),
        )
    }

    // Step 1
    val prisoner = getPrisoner(prisonNumber)

    // Step 2
    val prisonerCharges = remandAndSentencingApiClient.getPrisonerCharges(
      prisonNumber = prisonNumber,
      fromDate = prisoner.receptionDate,
      toDate = LocalDate.now(clock),
    )

    // The sentence types looked up, recorded so that they can be stored with a mismatched calculation
    val sentenceTypeDetailsByUuid = mutableMapOf<UUID, SentenceTypeDetails>()
    val sentences = prisonerCharges.charges.mapNotNull { it.sentence(sentenceTypeDetailsByUuid) }

    // The charges array contains charges, each with a single outcome. If a charge has multiple outcomes
    // there will be multiple charges with the same chargeUuid, one for each of the outcomes.
    // To get the list of offences we need to group all charges by the chargeUuid.
    // For each chargeUuid we will then have a list of charges.
    // The NOMIS algorithm for Imprisonment Status uses the first outcome for each. In order to use the same outcome
    // as NOMIS, we need to sort the outcomes by `displayOrder` and then select the first. This will give us the outcome
    // code that we need for the Imprisonment Status mapping.
    // NOTE: Selecting the first outcome per charge is current NOMIS behaviour. If there is a decision to change this
    // logic to use most recent outcome per charge this is where we would do it, but it would be a deviation from the
    // current NOMIS algorithm.
    // TODO - sorting on displayOrder is not the correct approach, but is the best we have at the moment and allows
    // current mismatch case tests to pass. The RaS API will be updated with more suitable fields to sort on, at which
    // point we have IS-160 which will update this code and the fields it sorts on.
    val offences = prisonerCharges.charges
      .groupBy { it.chargeUuid }
      .mapValues { (_, list) -> list.sortedBy { it.outcome?.displayOrder }.first() }
      .values
      .map { it.offence() }

    val sentencingData = LegacySentencingData(
      sentences = sentences,
      offences = offences,
    )

    // Steps 3 to 5
    val calculation = imprisonmentStatusCalculator.calculate(sentencingData)

    return LegacyImprisonmentStatusResult(
      prisonNumber = prisonNumber,
      nomisImprisonmentStatus = prisoner.imprisonmentStatus,
      receptionDate = prisoner.receptionDate,
      sentencingData = sentencingData,
      calculation = calculation,
      rasData = rasData(prisonerCharges.rawJson, sentenceTypeDetailsByUuid),
      calculatedAt = Instant.now(clock),
      bookingId = prisoner.bookingId,
    )
  }

  private fun getPrisoner(prisonNumber: String): Prisoner = prisonerSearchApiClient.getPrisoner(prisonNumber)
    .also {
      log.info { "Retrieved prisoner [$prisonNumber] from Prisoner Search API" }
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
          legacySentenceCalcType = it.nomisSentenceCalcType,
          legacySentenceCategory = it.nomisCjaCode,
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
