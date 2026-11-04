package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import java.time.LocalDate

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
