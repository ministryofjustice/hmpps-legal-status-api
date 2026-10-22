package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.IMPRISONMENT_STATUS_CHANGED

private val log = KotlinLogging.logger {}

/**
 * Service class that processes SQS events. For each type the [HmppsDomainEvent]'s `additionalInformation` property is
 * deserialized into it's corresponding type, and then the [HmppsDomainEvent] and it's [AdditionalInformation] are sent
 * to the relevant service specific to that event type.
 */
@Service
class InboundEventsService(
  private val imprisonmentStatusChangedEventHandler: ImprisonmentStatusChangedEventHandler,
) {

  fun process(domainEvent: HmppsDomainEvent) = with(domainEvent) {
    log.info { "Processing inbound event $eventType" }

    when (eventType) {
      IMPRISONMENT_STATUS_CHANGED -> imprisonmentStatusChangedEventHandler.handle(domainEvent)
    }
  }
}
