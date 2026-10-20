package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import java.time.LocalDate

/**
 * Simple data class modelling a NOMIS Sentence, encapsulating just the fields that are needed to calculate an
 * Imprisonment Status.
 *
 * There is one LegacySentence per sentenced charge (RaS charge), so a sentence covering several charges appears once per
 * charge. That matches the rows considered by the NOMIS query, which joins each sentence to each of its charges.
 *
 * @property offence The offence of the sentenced charge, used to look up the offence severity.
 * @property startDate With [endDate], gives the sentence length (NOMIS END_DATE - START_DATE), used as the second
 * tie-break when choosing between sentences.
 * TODO - RaS does not return sentence start/end dates. Confirm how to get the sentence length (e.g. from the RaS
 *   sentence periodLengths, or the NOMIS sentence dates). Until then these are null, so sentence length is not used.
 */
data class LegacySentence(
  val sentenceCalcType: String,
  val sentenceCategory: String,
  val offence: LegacyOffence,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
)
