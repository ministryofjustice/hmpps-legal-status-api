package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.legalstatusapi.client.prisonersearch.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.legalstatusapi.client.remandandsentencing.RemandAndSentencingApiClient
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.Charge
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
) {

  /**
   * Calculates the NOMIS legacy Imprisonment Status for the prisoner identified by their prison number.
   *
   * @param prisonNumber The prison number of the prisoner.
   * @return The calculated Imprisonment Status.
   */
  fun calculateImprisonmentStatus(prisonNumber: String): String {
    // Step 1
    val receptionDate = getReceptionDate(prisonNumber)

    // Step 2
    val prisonerChargesFromRemandAndSentencing: List<Charge> = remandAndSentencingApiClient.getPrisonerCharges(
      prisonNumber = prisonNumber,
      fromDate = receptionDate,
      toDate = LocalDate.now(clock),
    )
    val prisonerSentences = prisonerChargesFromRemandAndSentencing.mapNotNull { it.sentence() }
    val unSentencedOffences = prisonerChargesFromRemandAndSentencing.mapNotNull { it.offence() }

    // Step 3 goes here ...

    /*
    TODO - Implement logic to calculate imprisonment status based on NOMIS legacy data for the given prison number.

    Steps to implement:
    1. Retrieve the prisoner's receptionDate (which we assume at this point to be the booking start date)
    2. Get the prisoner's sentence(s) and offence(s) from RaS, using the prisonNumber to identify the prisoner along with a date range comprised of receptionDate to current date
    3. Find the correct sentenceCalcType & sentenceCategory from the list of sentences
      1. If there are no sentences, select the correct offenceType from the list of offences
    4. Use the sentenceCalcType and sentenceCategory, or offenceType, to lookup the correct Imprisonment Status Mapping
    5. Return the Imprisonment Status from the Imprisonment Status Mapping record
     */
    return ""
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
  private fun Charge.sentence(): LegacySentence? = sentence?.sentenceType?.sentenceTypeUuid?.let { sentenceTypeUuid ->
    remandAndSentencingApiClient.getSentenceTypeDetails(sentenceTypeUuid).let {
      LegacySentence(
        sentenceCalcType = it.nomisSentenceCalcType,
        sentenceCategory = it.nomisCjaCode,
        offence = offence()!!,
      ).also {
        log.debug { "LegacySentence mapped from looking up RaS sentenceType" }
      }
    }
  } ?: sentence?.legacyData?.let { legacyData ->
    LegacySentence(
      sentenceCalcType = legacyData.sentenceCalcType!!,
      sentenceCategory = legacyData.sentenceCategory!!,
      offence = offence()!!,
    ).also {
      log.debug { "LegacySentence mapped using legacy data fields from RaS response" }
    }
  }

  private fun Charge.offence(): LegacyOffence? = sentence?.let {
    LegacyOffence(
      offenceCode = offenceCode,
      outcomeCode = outcome?.nomisCode.also { log.debug { "LegacyOffence mapped from looking up RaS sentenceType" } }
        ?: legacyData?.nomisOutcomeCode!!.also { log.debug { "LegacyOffence mapped using legacy data fields from RaS response" } },
    )
  }
}
