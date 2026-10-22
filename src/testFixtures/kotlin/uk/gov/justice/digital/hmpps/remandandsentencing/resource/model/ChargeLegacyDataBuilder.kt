package uk.gov.justice.digital.hmpps.remandandsentencing.resource.model

fun aValidChargeLegacyData(
  nomisOutcomeCode: String = "1000", // Used in calculation
): ChargeLegacyData = ChargeLegacyData(
  nomisOutcomeCode = nomisOutcomeCode,
)
