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
   * We did not calculate the Imprisonment Status, so there was nothing to compare. It is recorded in
   * IMPRISONMENT_STATUS_MISMATCH, flagged with the [reason], and Slack is not notified.
   *
   * @property mismatchId The ID of the flagged IMPRISONMENT_STATUS_MISMATCH record.
   */
  data class NotCalculated(
    val mismatchId: UUID,
    val nomisImprisonmentStatus: String,
    val reason: NotCalculatedReason,
  ) : ImprisonmentStatusComparisonResult

  /**
   * Prisoner Search has no Imprisonment Status for the prisoner, so there was nothing to compare with.
   */
  data object NoNomisStatus : ImprisonmentStatusComparisonResult
}
