package uk.gov.justice.digital.hmpps.legalstatusapi.resource.model

import uk.gov.justice.digital.hmpps.legalstatusapi.randomValidPrisonNumber

fun aCreateImprisonmentStatusRequest(
  prisonNumber: String = randomValidPrisonNumber(),
  prisonId: String = "BXI",
  status: String = "SENT03",
  comment: String? = null,
): CreateImprisonmentStatusRequest = CreateImprisonmentStatusRequest(
  prisonNumber = prisonNumber,
  prisonId = prisonId,
  status = status,
  comment = comment,
)
