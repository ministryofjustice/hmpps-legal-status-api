package uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import org.hibernate.Hibernate
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType
import java.time.Instant
import java.util.UUID

@Entity(name = "MESSAGE_QUEUE_ITEM")
class MessageQueueItemEntity(
  @Column(name = "PRISON_NUMBER", nullable = false, updatable = false)
  val prisonNumber: String,

  @Enumerated(EnumType.STRING)
  @Column(name = "EVENT_TYPE", nullable = false, updatable = false)
  val eventType: EventType,

  @Column(name = "OCCURRED_AT", nullable = false, updatable = false)
  val occurredAt: Instant,
) {

  @Id
  @GeneratedValue
  var id: UUID? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as MessageQueueItemEntity

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String = this::class.simpleName + "(id = $id, prisonNumber = $prisonNumber, eventType = $eventType, occurredAt = $occurredAt)"
}
