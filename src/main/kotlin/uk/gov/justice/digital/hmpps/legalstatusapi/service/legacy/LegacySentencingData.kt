package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

/**
 * The sentencing data for a prisoner's current booking, in the shape needed by the NOMIS legacy Imprisonment Status
 * calculation. Step 2 of the calculation maps the RaS charges into this model.
 *
 * TODO - Confirm that the RaS charges endpoint filters the data in the same way as the NOMIS queries:
 *   - only charges and sentences from active court cases (NOMIS OFFENDER_CASES.CASE_STATUS = 'A')
 *   - only active sentences (NOMIS OFFENDER_SENTENCES.SENTENCE_STATUS = 'A')
 *   - all charges on those cases, regardless of charge status (NOMIS does not filter on OFFENDER_CHARGES.CHARGE_STATUS)
 *
 * @property sentences The prisoner's sentences, one per sentenced charge.
 * @property charges Every charge on the prisoner's active court cases, whether sentenced or not. NOMIS uses all of them
 * when the prisoner has no mapped sentences.
 */
data class LegacySentencingData(
  val sentences: List<LegacySentence> = emptyList(),
  val charges: List<LegacyCharge> = emptyList(),
)

/**
 * @property offenceCode The offence code, used to look up the offence severity.
 * @property resultCode The charge's court outcome code (NOMIS OFFENDER_CHARGES.RESULT_CODE_1), used to look up the
 * Imprisonment Status Mapping when the prisoner has no mapped sentences.
 */
data class LegacyCharge(
  val offenceCode: String,
  val resultCode: String? = null,
)
