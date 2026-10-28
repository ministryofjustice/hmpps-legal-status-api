package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import java.time.Instant
import java.time.LocalDate

/**
 * The result of calculating a prisoner's NOMIS legacy Imprisonment Status, along with the inputs used, so that a
 * calculation that does not match NOMIS can be recorded and reproduced.
 *
 * @property nomisImprisonmentStatus The Imprisonment Status that NOMIS holds for the prisoner, from Prisoner Search.
 * @property receptionDate The prisoner's receptionDate from Prisoner Search, used as the booking start date.
 * @property rasData The Remand and Sentencing API data used in the calculation, as JSON: the raw charges response
 * (`personCharges`) and the details of each sentence type looked up (`sentenceTypeDetails`).
 */
data class LegacyImprisonmentStatusResult(
  val prisonNumber: String,
  val nomisImprisonmentStatus: String?,
  val receptionDate: LocalDate?,
  val sentencingData: LegacySentencingData,
  val calculation: ImprisonmentStatusCalculation,
  val rasData: String,
  val calculatedAt: Instant,
)
