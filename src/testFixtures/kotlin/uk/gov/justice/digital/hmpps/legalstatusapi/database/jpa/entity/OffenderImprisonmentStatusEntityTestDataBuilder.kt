package uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.entity

import uk.gov.justice.digital.hmpps.legalstatusapi.aValidReference
import uk.gov.justice.digital.hmpps.legalstatusapi.randomValidPrisonNumber
import java.time.Instant
import java.util.UUID

fun anOffenderImprisonmentStatusEntity(
  id: UUID = aValidReference(),
  prisonNumber: String = randomValidPrisonNumber(),
  prisonId: String = "BXI",
  imprisonmentStatus: String = "SENT03",
  source: SourceSystem = SourceSystem.DPS,
  comment: String? = null,
  createdAt: Instant = Instant.now(),
  createdBy: String = "AUSER",
  updatedAt: Instant = Instant.now(),
  updatedBy: String = "AUSER",
): OffenderImprisonmentStatusEntity = OffenderImprisonmentStatusEntity(
  prisonNumber = prisonNumber,
  prisonId = prisonId,
  imprisonmentStatus = imprisonmentStatus,
  source = source,
  comment = comment,
).apply {
  this.id = id
  this.createdAt = createdAt
  this.createdBy = createdBy
  this.updatedAt = updatedAt
  this.updatedBy = updatedBy
}
