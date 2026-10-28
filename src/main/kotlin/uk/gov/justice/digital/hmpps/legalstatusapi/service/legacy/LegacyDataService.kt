package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import mu.KotlinLogging
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.legalstatusapi.client.prisonersearch.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.legalstatusapi.client.remandandsentencing.RemandAndSentencingApiClient
import uk.gov.justice.digital.hmpps.prisonersearch.resource.model.Prisoner
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonCharge
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.SentenceTypeDetails
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private val log = KotlinLogging.logger {}

/**
 * Service class for handling NOMIS legacy data operations.
 */
@Service
class LegacyDataService(
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val remandAndSentencingApiClient: RemandAndSentencingApiClient,
  private val clock: Clock,
  private val imprisonmentStatusCalculator: ImprisonmentStatusCalculator,
  private val jsonMapper: JsonMapper,
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
   */
  fun calculate(prisonNumber: String): LegacyImprisonmentStatusResult {
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
    val sentencingData = LegacySentencingData(
      sentences = prisonerCharges.charges.mapNotNull { it.sentence(sentenceTypeDetailsByUuid) },
      offences = prisonerCharges.charges.map { it.offence() },
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
   * If the Charge has a Sentence, the primary strategy should be to create a LegacySentence by looking up the sentenceType
   * from the RaS sentence reference data endpoint.
   * If Sentence does not have a sentenceType property, the LegacySentence should be created from the legacyData property.
   */
  private fun PersonCharge.sentence(sentenceTypeDetailsByUuid: MutableMap<UUID, SentenceTypeDetails>): LegacySentence? = sentence?.sentenceType?.sentenceTypeUuid?.let { sentenceTypeUuid ->
    sentenceTypeDetailsByUuid.getOrPut(sentenceTypeUuid) { remandAndSentencingApiClient.getSentenceTypeDetails(sentenceTypeUuid) }.let {
      LegacySentence(
        sentenceCalcType = it.nomisSentenceCalcType,
        sentenceCategory = it.nomisCjaCode,
        offence = offence(),
      ).also {
        log.debug { "LegacySentence mapped from looking up RaS sentenceType" }
      }
    }
  } ?: sentence?.legacyData?.let { legacyData ->
    LegacySentence(
      sentenceCalcType = legacyData.sentenceCalcType!!,
      sentenceCategory = legacyData.sentenceCategory!!,
      offence = offence(),
    ).also {
      log.debug { "LegacySentence mapped using legacy data fields from RaS response" }
    }
  }

  /**
   * Returns a [LegacyOffence] from a RaS [PersonCharge], sentenced or not. NOMIS uses every charge on the active court cases
   * (not just the unsentenced ones) when the prisoner has no mapped sentences (step 3.1).
   *
   * The outcome code is the NOMIS code of the DPS charge outcome or, if the outcome is not supported by DPS, the legacy
   * NOMIS outcome code.
   */
  private fun PersonCharge.offence() = LegacyOffence(
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
}
