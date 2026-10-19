package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import io.awspring.cloud.sqs.annotation.SqsListener
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

const val DOMAIN_EVENTS_QUEUE_ID = "domainevents"

private const val NOTIFICATION = "Notification"

private val log = KotlinLogging.logger {}

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
  private val domainEventsService: DomainEventsService,
) {

  @SqsListener(DOMAIN_EVENTS_QUEUE_ID, factory = "hmppsQueueContainerFactoryProxy")
  fun onMessage(sqsMessage: SqsMessage) {
    log.debug { "Inbound event message: ${sqsMessage.type}" }

    when (sqsMessage.type) {
      NOTIFICATION -> domainEventsService.process(jsonMapper.readValue(sqsMessage.message, DomainEvent::class.java))
      else -> log.info { "Unrecognised message type: ${sqsMessage.type}" }
    }
  }
}
