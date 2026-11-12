package uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.entity

import uk.gov.justice.digital.hmpps.legalstatusapi.aValidReference
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.CHARGE_DELETED
import uk.gov.justice.digital.hmpps.legalstatusapi.randomValidPrisonNumber
import java.time.Instant
import java.util.UUID

fun aMessageQueueItemEntity(
  id: UUID? = aValidReference(),
  prisonNumber: String = randomValidPrisonNumber(),
  eventType: EventType = CHARGE_DELETED,
  occurredAt: Instant = Instant.now(),
): MessageQueueItemEntity = MessageQueueItemEntity(
  prisonNumber = prisonNumber,
  eventType = eventType,
  occurredAt = occurredAt,
).apply {
  this.id = id
}
