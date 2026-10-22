package uk.gov.justice.digital.hmpps.remandandsentencing.resource.model

import uk.gov.justice.digital.hmpps.legalstatusapi.aValidReference
import java.util.UUID

fun aValidSentenceLegacyData(
  postedDate: String = "2023-07-26T11:16:57.914099",
  sentenceCalcType: String = "Sentence Calc Type",  // Used in calculation
  sentenceCategory: String = "Sentence Category",   // Used in calculation
): SentenceLegacyData = SentenceLegacyData(
  postedDate = postedDate,
  sentenceCalcType = sentenceCalcType,
  sentenceCategory = sentenceCategory,
)