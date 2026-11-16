package uk.gov.justice.digital.hmpps.remandandsentencing.resource.model

import uk.gov.justice.digital.hmpps.legalstatusapi.aValidReference
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun aValidPersonCharge(
  chargeUuid: UUID = aValidReference(), // Used in calculation for logging
  offenceCode: String = "RF96119", // Used in calculation
  aggravatingFactors: List<AggravatingFactor> = emptyList(),
  outcome: ChargeOutcome? = aValidChargeOutcome(), // Used in calculation
  legacyData: ChargeLegacyData? = aValidChargeLegacyData(), // Used in calculation
  sentence: PersonSentence? = aValidPersonSentence(), // Used in calculation
  appearanceDate: LocalDate = LocalDate.now(),
  appearanceCreatedAt: LocalDateTime = LocalDateTime.now(),
): PersonCharge = PersonCharge(
  chargeUuid = chargeUuid,
  offenceCode = offenceCode,
  aggravatingFactors = aggravatingFactors,
  outcome = outcome,
  legacyData = legacyData,
  sentence = sentence,
  appearanceDate = appearanceDate,
  appearanceCreatedAt = appearanceCreatedAt,
)
