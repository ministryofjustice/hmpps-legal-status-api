package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatus
import uk.gov.justice.digital.hmpps.legalstatusapi.randomValidPrisonNumber
import java.time.Instant
import java.time.LocalDate

fun aLegacyImprisonmentStatusResult(
  prisonNumber: String = randomValidPrisonNumber(),
  nomisImprisonmentStatus: String? = "SENT03",
  receptionDate: LocalDate? = LocalDate.now().minusMonths(6),
  sentencingData: LegacySentencingData = LegacySentencingData(
    sentences = listOf(aSentence(offenceCode = "TH68010")),
    offences = listOf(anOffence(offenceCode = "TH68010"), anOffence(offenceCode = "CJ88116", outcomeCode = "4565")),
  ),
  calculation: ImprisonmentStatusCalculation = ImprisonmentStatusCalculation(ImprisonmentStatus.SENT03, "TH68010", CalculatedFrom.SENTENCE),
  rasData: String = """{"personCharges": {"charges": []}, "sentenceTypeDetails": {}}""",
  calculatedAt: Instant = Instant.now(),
): LegacyImprisonmentStatusResult = LegacyImprisonmentStatusResult(
  prisonNumber = prisonNumber,
  nomisImprisonmentStatus = nomisImprisonmentStatus,
  receptionDate = receptionDate,
  sentencingData = sentencingData,
  calculation = calculation,
  rasData = rasData,
  calculatedAt = calculatedAt,
)
