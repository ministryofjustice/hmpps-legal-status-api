package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.CHARGE_DELETED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.CHARGE_INSERTED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.CHARGE_UPDATED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.IMPRISONMENT_STATUS_CHANGED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.SENTENCE_DELETED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.SENTENCE_INSERTED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.SENTENCE_UPDATED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.eventhandling.ImprisonmentStatusChangedEventHandler
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.eventhandling.SentenceOrChargeEventHandler

private val log = KotlinLogging.logger {}

/**
 * Service class that processes SQS events. For each type the [HmppsDomainEvent]'s `additionalInformation` property is
 * deserialized into it's corresponding type, and then the [HmppsDomainEvent] and it's [AdditionalInformation] are sent
 * to the relevant service specific to that event type.
 */
@Service
class InboundEventsService(
  private val imprisonmentStatusChangedEventHandler: ImprisonmentStatusChangedEventHandler,
  private val sentenceOrChargeEventHandler: SentenceOrChargeEventHandler,
) {

  fun process(domainEvent: HmppsDomainEvent) = with(domainEvent) {
    log.info { "Processing inbound event $eventType" }

    when (eventType) {
      IMPRISONMENT_STATUS_CHANGED -> {
        imprisonmentStatusChangedEventHandler.handle(domainEvent)
      }
      SENTENCE_INSERTED -> {
        sentenceOrChargeEventHandler.handle(domainEvent)
      }
      SENTENCE_DELETED -> {
        sentenceOrChargeEventHandler.handle(domainEvent)
      }
      SENTENCE_UPDATED -> {
        sentenceOrChargeEventHandler.handle(domainEvent)
      }
      CHARGE_INSERTED -> {
        sentenceOrChargeEventHandler.handle(domainEvent)
      }
      CHARGE_DELETED -> {
        sentenceOrChargeEventHandler.handle(domainEvent)
      }
      CHARGE_UPDATED -> {
        sentenceOrChargeEventHandler.handle(domainEvent)
      }
    }
  }
}
