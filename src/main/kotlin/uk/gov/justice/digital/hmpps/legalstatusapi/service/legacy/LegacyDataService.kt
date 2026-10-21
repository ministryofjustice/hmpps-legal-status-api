package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.legalstatusapi.client.prisonersearch.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.legalstatusapi.client.remandandsentencing.RemandAndSentencingApiClient
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonCharge
import java.time.Clock
import java.time.LocalDate

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
) {

  /**
   * Calculates the NOMIS legacy Imprisonment Status for the prisoner identified by their prison number.
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
   * @return The calculated Imprisonment Status.
   */
  fun calculateImprisonmentStatus(prisonNumber: String): String {
    // Step 1
    val receptionDate = getReceptionDate(prisonNumber)

    // Step 2
    val prisonerChargesFromRemandAndSentencing: List<PersonCharge> = remandAndSentencingApiClient.getPrisonerCharges(
      prisonNumber = prisonNumber,
      fromDate = receptionDate,
      toDate = LocalDate.now(clock),
    )
    val sentencingData = LegacySentencingData(
      sentences = prisonerChargesFromRemandAndSentencing.mapNotNull { it.sentence() },
      offences = prisonerChargesFromRemandAndSentencing.map { it.offence() },
    )

    // Steps 3 to 5
    // TODO - Decide whether to return ImprisonmentStatus (or ImprisonmentStatusCalculation) rather than a String
    return imprisonmentStatusCalculator.calculate(sentencingData).imprisonmentStatus.nomisCode
  }

  private fun getReceptionDate(prisonNumber: String): LocalDate? = prisonerSearchApiClient.getPrisoner(prisonNumber).receptionDate
    .also {
      log.info { "Retrieved prisoner [$prisonNumber] from Prisoner Search API" }
    }

  /**
   * Returns a [LegacySentence] or null from a Ras [Charge]
   *
   * If the Charge has a Sentence, the primary strategy should be to create a LegacySentence by looking up the sentenceType
   * from the RaS sentence reference data endpoint.
   * If Sentence does not have a sentenceType property, the LegacySentence should be created from the legacyData property.
   */
  private fun PersonCharge.sentence(): LegacySentence? = sentence?.sentenceType?.sentenceTypeUuid?.let { sentenceTypeUuid ->
    remandAndSentencingApiClient.getSentenceTypeDetails(sentenceTypeUuid).let {
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
