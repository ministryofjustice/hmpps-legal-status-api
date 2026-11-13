package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import java.time.Instant
import java.time.LocalDate

/**
 * The result of calculating a prisoner's NOMIS legacy Imprisonment Status, along with the inputs used, so that a
 * calculation that does not match NOMIS can be recorded and reproduced.
 *
 * @property nomisImprisonmentStatus The Imprisonment Status that NOMIS holds for the prisoner, from Prisoner Search.
 * @property receptionDate The prisoner's receptionDate from Prisoner Search. It is kept for the record only, and is not
 * used in the calculation.
 * @property previousPeriodConcludedAt When the prisoner's previous period of custody concluded, from the Person Location
 * API, or null if there is no previous period of custody.
 * @property chargesWindowStart The start of the date range used to get the prisoner's charges from RaS (the RaS
 * `fromDate`), or null if no `fromDate` was used. It is selected from [previousPeriodConcludedAt]. See
 * [chargesWindowStart].
 * @property rasData The Remand and Sentencing API data used in the calculation, as JSON: the raw charges response
 * (`personCharges`) and the details of each sentence type looked up (`sentenceTypeDetails`).
 */
data class LegacyImprisonmentStatusResult(
  val prisonNumber: String,
  val nomisImprisonmentStatus: String?,
  val receptionDate: LocalDate?,
  val previousPeriodConcludedAt: Instant?,
  val chargesWindowStart: LocalDate?,
  val sentencingData: LegacySentencingData,
  val calculation: ImprisonmentStatusCalculation,
  val rasData: String,
  val calculatedAt: Instant,
  val bookingId: String?,
)
