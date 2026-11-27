package uk.gov.justice.digital.hmpps.legalstatusapi.resource.model

import uk.gov.justice.digital.hmpps.legalstatusapi.randomValidPrisonNumber
import java.time.LocalDateTime

fun aSyncCreateImprisonmentStatusRequest(
  prisonNumber: String = randomValidPrisonNumber(),
  prisonId: String = "BXI",
  status: String = "SENT03",
  comment: String? = null,
  createdBy: String = "AUSER",
  createdAt: LocalDateTime = LocalDateTime.now(),
  updatedBy: String = "AUSER",
  updatedAt: LocalDateTime = LocalDateTime.now(),
): SyncCreateImprisonmentStatusRequest = SyncCreateImprisonmentStatusRequest(
  prisonNumber = prisonNumber,
  prisonId = prisonId,
  status = status,
  createdBy = createdBy,
  createdAt = createdAt,
  updatedBy = updatedBy,
  updatedAt = updatedAt,
  comment = comment,
)
