package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

const val DOMAIN_EVENTS_QUEUE_ID = "domainevents"

private const val NOTIFICATION = "Notification"

/**
 * Listens to the Legal Status domain events queue, which is subscribed to the HMPPS domain events topic and filtered
 * to the event types we handle (see cloud-platform-environments `sqs-domain-events.tf`).
 *
 * Each event type is routed to its own handler. Unexpected event types and message types are logged and acknowledged
 * rather than failed, so they do not end up on the DLQ.
 */
@Component
@ConditionalOnProperty(name = ["hmpps.sqs.enabled"], havingValue = "true")
class DomainEventsListener(
  private val jsonMapper: JsonMapper,
  private val imprisonmentStatusChangedEventHandler: ImprisonmentStatusChangedEventHandler,
) {

  @SqsListener(DOMAIN_EVENTS_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onMessage(rawMessage: String) {
    val sqsMessage = jsonMapper.readValue(rawMessage, SqsMessage::class.java)
    if (sqsMessage.type != NOTIFICATION) {
      log.warn("Ignoring message {} with unexpected type {}", sqsMessage.messageId, sqsMessage.type)
      return
    }

    val event = jsonMapper.readValue(sqsMessage.message, HmppsDomainEvent::class.java)
    when (event.eventType) {
      IMPRISONMENT_STATUS_CHANGED -> imprisonmentStatusChangedEventHandler.handle(sqsMessage.messageId, event)
      else -> log.warn("Ignoring message {} with unexpected eventType {}", sqsMessage.messageId, event.eventType)
    }
  }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
