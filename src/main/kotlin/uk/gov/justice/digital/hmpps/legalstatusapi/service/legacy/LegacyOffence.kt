package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

/**
 * Simple data class modelling a NOMIS Offence, encapsulating just the fields that are needed to calculate an
 * Imprisonment Status.
 *
 * TODO - we might need extra properties in this class. TBC
 *
 * @property offenceCode The offence code, used to look up the offence severity.
 * @property outcomeCode The NOMIS code of the charge's court outcome (NOMIS OFFENDER_CHARGES.RESULT_CODE_1), used to look
 * up the Imprisonment Status Mapping when the prisoner has no mapped sentences. Null if the charge has no outcome, as
 * RESULT_CODE_1 can be in NOMIS.
 */
data class LegacyOffence(
  val offenceCode: String,
  val outcomeCode: String?,
)
