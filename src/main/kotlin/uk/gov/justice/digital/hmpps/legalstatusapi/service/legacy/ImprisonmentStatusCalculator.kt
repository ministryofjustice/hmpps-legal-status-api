package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import mu.KotlinLogging
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatus
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatusEntity
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.repository.ImprisonmentStatusMappingRepository
import java.time.temporal.ChronoUnit

private val log = KotlinLogging.logger {}

/**
 * Calculates the NOMIS legacy Imprisonment Status from a prisoner's sentencing data (steps 3 to 5 of the calculation).
 *
 * This is a port of the two NOMIS queries in hmpps-nomis-prisoner-api OffenderImprisonmentStatusRepository, which are
 * explained on the Confluence page "Current Imprisonment Status calculation implementation":
 * https://github.com/ministryofjustice/hmpps-nomis-prisoner-api/blob/00d1e7627a5c0fc2f31a836215f3daa6612538fc/src/main/kotlin/uk/gov/justice/digital/hmpps/nomisprisonerapi/jpa/repository/OffenderImprisonmentStatusRepository.kt
 *
 * - Step 3: getStatusAndMainOffenceViaSentenceByBookingId is used first. It considers each mapped sentence and its
 *   charge.
 * - Step 3.1: getStatusAndMainOffenceViaChargeOutcomeByBookingId is used if that gives no result (e.g. the prisoner is
 *   not sentenced). It considers every charge and its court outcome (NOMIS
 *   OFFENDER_CHARGES.RESULT_CODE_1, which is the RaS outcome code).
 *
 * Each NOMIS query filters its rows down to one, in a fixed order of precedence (e.g. lowest status rank, then longest
 * sentence, then most severe offence ...). That is the same as sorting the rows by those values and taking the first,
 * which is how it is implemented here.
 */
@Component
class ImprisonmentStatusCalculator(
  private val imprisonmentStatusMappingRepository: ImprisonmentStatusMappingRepository,
  private val offenceSeverityProvider: OffenceSeverityProvider,
) {

  fun calculate(sentencingData: LegacySentencingData): ImprisonmentStatusCalculation {
    val severities = offenceSeverityProvider.getSeverities(
      sentencingData.sentences.map { it.offence.offenceCode } + sentencingData.offences.map { it.offenceCode },
    )

    return (
      calculateFromSentences(sentencingData.sentences, severities)
        ?: calculateFromChargeOutcomes(sentencingData.offences, severities)
      ).also {
      log.info { "Calculated Imprisonment Status [${it.imprisonmentStatus}] from [${it.calculatedFrom}] with main offence [${it.mainOffenceCode}]" }
    }
  }

  /**
   * Step 3: Choose the sentence (and its main offence) that determines the Imprisonment Status, and steps 4 & 5: return
   * the Imprisonment Status mapped from its sentence category and calc type.
   *
   * @return null if no sentence has an Imprisonment Status Mapping.
   */
  private fun calculateFromSentences(sentences: List<LegacySentence>, severities: Map<String, Int>): ImprisonmentStatusCalculation? {
    val candidates = sentences.mapNotNull { sentence ->
      imprisonmentStatusMappingRepository.findBySentenceCategoryAndSentenceCalcType(sentence.sentenceCategory, sentence.sentenceCalcType)
        ?.let { SentenceCandidate(sentence, it.imprisonmentStatus, severities[sentence.offence.offenceCode]) }
        .also { if (it == null) log.info { "No Imprisonment Status Mapping for sentence with category [${sentence.sentenceCategory}] and calc type [${sentence.sentenceCalcType}]" } }
    }

    return candidates.minWithOrNull(SENTENCE_PRECEDENCE)?.let {
      ImprisonmentStatusCalculation(it.status.imprisonmentStatus, it.sentence.offence.offenceCode, CalculatedFrom.SENTENCE)
    }
  }

  /**
   * Step 3.1: Used when no sentence gives a result. Choose the main offence, and return the Imprisonment Status mapped
   * from its outcome code; or UNKNOWN if its outcome code has no mapping or there are no offences.
   */
  private fun calculateFromChargeOutcomes(offences: List<LegacyOffence>, severities: Map<String, Int>): ImprisonmentStatusCalculation {
    val outcomeCodeMappings = offences.map { it.outcomeCode }.distinct()
      .associateWith { imprisonmentStatusMappingRepository.findByOffenceResultCode(it)?.imprisonmentStatus }

    val mainOffence = offences
      .map { ChargeOutcome(it, outcomeCodeMappings[it.outcomeCode], severities[it.offenceCode]) }
      .minWithOrNull(CHARGE_OUTCOME_PRECEDENCE)

    return ImprisonmentStatusCalculation(
      imprisonmentStatus = mainOffence?.status?.imprisonmentStatus ?: ImprisonmentStatus.UNKNOWN,
      mainOffenceCode = mainOffence?.offence?.offenceCode,
      calculatedFrom = CalculatedFrom.CHARGE_OUTCOME,
    )
  }

  private data class SentenceCandidate(
    val sentence: LegacySentence,
    val status: ImprisonmentStatusEntity,
    val severity: Int?,
  ) {
    // NOMIS uses END_DATE - START_DATE, which is a number of days
    val sentenceLengthDays: Long? =
      if (sentence.startDate != null && sentence.endDate != null) ChronoUnit.DAYS.between(sentence.startDate, sentence.endDate) else null
  }

  private data class ChargeOutcome(
    val offence: LegacyOffence,
    val status: ImprisonmentStatusEntity?,
    val severity: Int?,
  )

  private companion object {
    /*
    The order of precedence in getStatusAndMainOffenceViaSentenceByBookingId:
      1. The lowest status rank (RANK_VALUE)
      2. The longest sentence. NOMIS only keeps sentences with no length when none of them have a length, which is the
         same as putting them last.
      3. The most severe offence (lowest SEVERITY_RANKING).
         Deliberate difference: NOMIS drops charges with no severity, and returns no result if none have one. Here they
         are put last instead. In NOMIS a missing severity is rare, but for us it is common because Manage Offences has no
         severity for some offences (see OffenceSeverityProvider). Dropping them would fall back to the charge outcome
         calculation and give a remand/unknown status to some sentenced prisoners.
      4. The lowest sentence sequence, then 5. the lowest charge ID (the charge created first).
         RaS gives us neither, so the first sentence in the RaS response is chosen instead (minWithOrNull returns the
         first of equal elements). These only decide between sentences with the same rank, length and severity, so only
         change the main offence, or the status where 2 statuses share a rank (e.g. SENT03 and ADIMP_ORA20).
         TODO - Confirm whether RaS can give us something to order by (e.g. the NOMIS sentence sequence / charge ID).
     */
    val SENTENCE_PRECEDENCE: Comparator<SentenceCandidate> =
      compareBy<SentenceCandidate> { it.status.rank }
        .thenBy(nullsLast(reverseOrder())) { it.sentenceLengthDays }
        .thenBy(nullsLast()) { it.severity }

    /*
    The order of precedence in getStatusAndMainOffenceViaChargeOutcomeByBookingId:
      1. The lowest status rank. Charges whose outcome code has no mapping are only chosen when no charge has one, which is
         the same as putting them last.
      2. The most severe offence (lowest SEVERITY_RANKING), with charges that have no severity last.
         Deliberate difference: when the chosen rank has charges both with and without a severity, NOMIS keeps one of each
         and so returns 2 rows (which errors in hmpps-nomis-prisoner-api). Here the charge with a severity is chosen.
      3. The lowest charge ID (the charge created first). As above, the first charge in the RaS response is chosen
         instead.
     */
    val CHARGE_OUTCOME_PRECEDENCE: Comparator<ChargeOutcome> =
      compareBy<ChargeOutcome, Int?>(nullsLast()) { it.status?.rank }
        .thenBy(nullsLast()) { it.severity }
  }
}

data class ImprisonmentStatusCalculation(
  val imprisonmentStatus: ImprisonmentStatus,
  val mainOffenceCode: String?,
  val calculatedFrom: CalculatedFrom,
)

enum class CalculatedFrom {
  SENTENCE,
  CHARGE_OUTCOME,
}
