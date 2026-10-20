package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

/**
 * Simple data class modelling a NOMIS Sentence, encapsulating just the fields that are needed to calculate an
 * Imprisonment Status.
 *
 * TODO - we might need extra properties in this class, such as sentence dates/duration. TBC
 */
data class LegacySentence(
  val sentenceCalcType: String,
  val sentenceCategory: String,
  val offence: LegacyOffence,
)
