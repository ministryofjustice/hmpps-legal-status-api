package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The result of calculating a prisoner's NOMIS legacy Imprisonment Status, along with the inputs used, so that a
 * calculation that does not match NOMIS can be recorded and reproduced.
 *
 * @property nomisImprisonmentStatus The Imprisonment Status that NOMIS holds for the prisoner, from Prisoner Search.
 * @property receptionDate The prisoner's receptionDate from Prisoner Search. It is kept for the record only, and is not
 * used in the calculation.
 * @property previousPeriodConcludedAt When the prisoner's previous period of custody concluded, from the Person Location
 * API, or null if there is no previous period of custody. A local date and time, with no offset, as the API returns it.
 * @property chargesWindowStart The start of the date range used to get the prisoner's charges from RaS (the RaS
 * `fromDate`), or null if no `fromDate` was used. It is selected from [previousPeriodConcludedAt]. See
 * [chargesWindowStart].
 * @property rasData The Remand and Sentencing API data used in the calculation, as JSON: the raw charges response
 * (`personCharges`) and the details of each sentence type looked up (`sentenceTypeDetails`).
 * @property calculation The calculated Imprisonment Status, or null if it was not calculated because RaS returned no
 * charges ([NotCalculatedReason.NO_RAS_CHARGES]). ASSUMPTION (IS-181): an empty list of charges is never valid, so it
 * means that the RaS data is not complete. See [LegacyDataService.calculate].
 * @property calculatedAt When the calculation was done, or attempted if [calculation] is null.
 */
data class LegacyImprisonmentStatusResult(
  val prisonNumber: String,
  val nomisImprisonmentStatus: String?,
  val receptionDate: LocalDate?,
  val previousPeriodConcludedAt: LocalDateTime?,
  val chargesWindowStart: LocalDate?,
  val sentencingData: LegacySentencingData,
  val calculation: ImprisonmentStatusCalculation?,
  val rasData: String,
  val calculatedAt: Instant,
  val bookingId: String?,
)
