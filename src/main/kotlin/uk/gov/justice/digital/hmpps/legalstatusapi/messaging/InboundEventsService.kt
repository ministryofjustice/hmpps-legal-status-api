package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.CHARGE_DELETED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.CHARGE_INSERTED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.CHARGE_UPDATED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.IMPRISONMENT_STATUS_CHANGED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.IMPRISONMENT_STATUS_COMPUTABLE
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.SENTENCE_DELETED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.SENTENCE_INSERTED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.SENTENCE_UPDATED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.eventhandling.HmppsDomainEventHandler
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.eventhandling.ImprisonmentStatusChangedEventHandler
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.eventhandling.ImprisonmentStatusComputableEventHandler
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.eventhandling.SentenceOrChargeEventHandler

private val log = KotlinLogging.logger {}

/**
 * Service class that processes SQS events. Messages are handled by specialised [HmppsDomainEventHandler] classes.
 */
@Service
class InboundEventsService(
  private val imprisonmentStatusChangedEventHandler: ImprisonmentStatusChangedEventHandler,
  private val sentenceOrChargeEventHandler: SentenceOrChargeEventHandler,
  private val imprisonmentStatusComputableEventHandler: ImprisonmentStatusComputableEventHandler,
) {

  fun process(domainEvent: HmppsDomainEvent) = with(domainEvent) {
    log.info { "Processing inbound event $eventType" }

    when (eventType) {
      IMPRISONMENT_STATUS_CHANGED -> {
        imprisonmentStatusChangedEventHandler.handle(domainEvent)
      }

      SENTENCE_INSERTED, SENTENCE_DELETED, SENTENCE_UPDATED, CHARGE_INSERTED, CHARGE_DELETED, CHARGE_UPDATED -> {
        sentenceOrChargeEventHandler.handle(domainEvent)
      }

      IMPRISONMENT_STATUS_COMPUTABLE -> {
        imprisonmentStatusComputableEventHandler.handle(domainEvent)
      }
    }
  }
}
