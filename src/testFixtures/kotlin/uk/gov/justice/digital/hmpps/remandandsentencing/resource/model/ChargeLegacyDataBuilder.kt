package uk.gov.justice.digital.hmpps.remandandsentencing.resource.model

import uk.gov.justice.digital.hmpps.legalstatusapi.aValidReference
import java.util.UUID

fun aValidChargeLegacyData(
  nomisOutcomeCode: String = "1000",    // Used in calculation
): ChargeLegacyData = ChargeLegacyData(
  nomisOutcomeCode = nomisOutcomeCode,
)