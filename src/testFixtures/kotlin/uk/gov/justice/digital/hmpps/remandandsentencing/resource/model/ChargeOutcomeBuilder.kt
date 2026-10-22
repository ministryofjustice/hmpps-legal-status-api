package uk.gov.justice.digital.hmpps.remandandsentencing.resource.model

import uk.gov.justice.digital.hmpps.legalstatusapi.aValidReference
import java.util.UUID

fun aValidChargeOutcome(
  outcomeUuid: UUID = aValidReference(),
  outcomeName: String = "Imprisonment",
  nomisCode: String = "1002", // Used in calculation
  outcomeType: String = "SENTENCING",
  displayOrder: Int = 10,
  dispositionCode: String = "FINAL",
  status: ChargeOutcome.Status = ChargeOutcome.Status.ACTIVE,
): ChargeOutcome = ChargeOutcome(
  outcomeUuid = outcomeUuid,
  outcomeName = outcomeName,
  nomisCode = nomisCode,
  outcomeType = outcomeType,
  displayOrder = displayOrder,
  dispositionCode = dispositionCode,
  status = status,
)
