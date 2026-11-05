package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonSentence
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.SentenceTypeDetails

fun aRasSentence(
  offence: Offence = anOffence(offenceCode = "TH68010", outcomeCode = "4565"),
  sentenceLengthDays: Long? = null,
  classification: SentenceTypeDetails.Classification = SentenceTypeDetails.Classification.STANDARD,
  legacySentenceCalcType: String = "ADIMP",
  legacySentenceCategory: String = "2003",
  latestRecallType: PersonSentence.LatestRecallType? = null,
): Sentence.RasSentence = Sentence.RasSentence(
  classification = classification,
  offence = offence,
  sentenceLengthDays = sentenceLengthDays,
  legacySentenceCategory = legacySentenceCategory,
  legacySentenceCalcType = legacySentenceCalcType,
  latestRecallType = latestRecallType,
)

fun aLegacySentence(
  sentenceCategory: String = "2003",
  sentenceCalcType: String = "ADIMP",
  offence: Offence = anOffence(offenceCode = "TH68010", outcomeCode = "4565"),
  sentenceLengthDays: Long? = null,
): Sentence.LegacySentence = Sentence.LegacySentence(
  sentenceCalcType = sentenceCalcType,
  sentenceCategory = sentenceCategory,
  offence = offence,
  sentenceLengthDays = sentenceLengthDays,
)
