package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

/**
 * Simple data class modelling a NOMIS Offence, encapsulating just the fields that are needed to calculate an
 * Imprisonment Status.
 *
 * TODO - we might need extra properties in this class. TBC
 */
data class LegacyOffence(
  val offenceCode: String,
  val outcomeCode: String,
)
