package uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.entity

import org.assertj.core.api.AbstractObjectAssert
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType
import java.time.Instant

fun assertThat(actual: MessageQueueItemEntity?) = MessageQueueItemEntityAssert(actual)

/**
 * AssertJ custom assertion for [MessageQueueItemEntity]
 */
class MessageQueueItemEntityAssert(actual: MessageQueueItemEntity?) : AbstractObjectAssert<MessageQueueItemEntityAssert, MessageQueueItemEntity?>(actual, MessageQueueItemEntityAssert::class.java) {

  fun hasAnId(): MessageQueueItemEntityAssert {
    isNotNull
    with(actual!!) {
      if (id == null) {
        failWithMessage("Expected MessageQueueItemEntity to have an ID, but it did not")
      }
    }
    return this
  }

  fun isForPrisonNumber(expected: String): MessageQueueItemEntityAssert {
    isNotNull
    with(actual!!) {
      if (prisonNumber != expected) {
        failWithMessage("Expected prisonNumber to be $expected, but it was $prisonNumber")
      }
    }
    return this
  }

  fun isEventType(expected: EventType): MessageQueueItemEntityAssert {
    isNotNull
    with(actual!!) {
      if (eventType != expected) {
        failWithMessage("Expected eventType to be $expected, but it was $eventType")
      }
    }
    return this
  }

  fun occurredAt(expected: Instant): MessageQueueItemEntityAssert {
    isNotNull
    with(actual!!) {
      if (occurredAt != expected) {
        failWithMessage("Expected occurredAt to be $expected, but it was $occurredAt")
      }
    }
    return this
  }
}
