package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

/**
 * Simple data class modelling an Offence, encapsulating just the fields that are needed to calculate an
 * Imprisonment Status.
 *
 * TODO - we might need extra properties in this class. TBC
 *
 * @property offenceCode The offence code, used to look up the offence severity.
 * @property outcomeCode The code of the charge's court outcome. Used to look up the Imprisonment Status Mapping
 * when the prisoner has no mapped sentences. Null if the charge has no outcome.
 */
data class Offence(
  val offenceCode: String,
  val outcomeCode: String?,
)
