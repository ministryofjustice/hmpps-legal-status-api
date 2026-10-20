package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import java.time.LocalDate

fun anOffence(
  offenceCode: String = "TH68010",
  outcomeCode: String = "1002",
): LegacyOffence = LegacyOffence(
  offenceCode = offenceCode,
  outcomeCode = outcomeCode,
)

fun aSentence(
  sentenceCategory: String = "2003",
  sentenceCalcType: String = "ADIMP",
  offenceCode: String = "TH68010",
  startDate: LocalDate? = null,
  endDate: LocalDate? = null,
): LegacySentence = LegacySentence(
  sentenceCalcType = sentenceCalcType,
  sentenceCategory = sentenceCategory,
  offence = anOffence(offenceCode = offenceCode),
  startDate = startDate,
  endDate = endDate,
)
