package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import java.util.UUID

/**
 * The outcome of comparing our calculated Imprisonment Status with the one NOMIS holds.
 */
sealed interface ImprisonmentStatusComparisonResult {

  data class Match(val imprisonmentStatus: String) : ImprisonmentStatusComparisonResult

  /**
   * @property mismatchId The ID of the IMPRISONMENT_STATUS_MISMATCH record, which is also the reference sent to Slack.
   */
  data class Mismatch(
    val mismatchId: UUID,
    val nomisImprisonmentStatus: String,
    val dpsImprisonmentStatus: String,
  ) : ImprisonmentStatusComparisonResult

  /**
   * Prisoner Search has no Imprisonment Status for the prisoner, so there was nothing to compare with.
   */
  data object NoNomisStatus : ImprisonmentStatusComparisonResult
}
