package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

/**
 * Why we did not calculate a prisoner's Imprisonment Status. It is recorded in
 * IMPRISONMENT_STATUS_MISMATCH.NOT_CALCULATED_REASON, so that the row is not taken as a real mismatch.
 */
enum class NotCalculatedReason {

  /**
   * The Remand and Sentencing API returned an empty list of charges for the prisoner.
   *
   * ASSUMPTION (IS-181): an empty list of charges is never valid. A prisoner in custody always has at least one charge,
   * so an empty response means that the RaS data is not complete yet. See [LegacyDataService.calculate] for what could
   * make this assumption wrong.
   */
  NO_RAS_CHARGES,
}
