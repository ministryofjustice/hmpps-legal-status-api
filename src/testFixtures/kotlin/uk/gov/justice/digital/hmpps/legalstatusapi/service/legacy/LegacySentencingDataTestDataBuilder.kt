package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import java.time.LocalDate

fun aCharge(
  offenceCode: String = "TH68010",
  resultCode: String? = null,
): LegacyCharge = LegacyCharge(
  offenceCode = offenceCode,
  resultCode = resultCode,
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
  offenceCode = offenceCode,
  startDate = startDate,
  endDate = endDate,
)
