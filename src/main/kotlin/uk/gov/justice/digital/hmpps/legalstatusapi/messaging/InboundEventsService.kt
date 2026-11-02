package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import mu.KotlinLogging
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.AdditionalInformation.ChargeAdditionalInformation
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.AdditionalInformation.ImprisonmentStatusChangedAdditionalInformation
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.AdditionalInformation.SentenceAdditionalInformation
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.CHARGE_DELETED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.CHARGE_INSERTED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.CHARGE_UPDATED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.IMPRISONMENT_STATUS_CHANGED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.SENTENCE_DELETED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.SENTENCE_INSERTED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.SENTENCE_UPDATED

private val log = KotlinLogging.logger {}

/**
 * Service class that processes SQS events. For each type the [HmppsDomainEvent]'s `additionalInformation` property is
 * deserialized into it's corresponding type, and then the [HmppsDomainEvent] and it's [AdditionalInformation] are sent
 * to the relevant service specific to that event type.
 */
@Service
class InboundEventsService(
  private val jsonMapper: JsonMapper,
  private val imprisonmentStatusChangedEventHandler: ImprisonmentStatusChangedEventHandler,
) {

  fun process(domainEvent: HmppsDomainEvent) = with(domainEvent) {
    log.info { "Processing inbound event $eventType" }

    when (eventType) {
      IMPRISONMENT_STATUS_CHANGED -> {
        val additionalInformation = eventAdditionalInformation<ImprisonmentStatusChangedAdditionalInformation>(domainEvent)
        imprisonmentStatusChangedEventHandler.handle(domainEvent, additionalInformation)
      }
      SENTENCE_INSERTED -> {
        val additionalInformation = eventAdditionalInformation<SentenceAdditionalInformation>(domainEvent)
        log.info { "Received sentence.inserted event with additionalInformation $additionalInformation" }
      }
      SENTENCE_DELETED -> {
        val additionalInformation = eventAdditionalInformation<SentenceAdditionalInformation>(domainEvent)
        log.info { "Received sentence.deleted event with additionalInformation $additionalInformation" }
      }
      SENTENCE_UPDATED -> {
        val additionalInformation = eventAdditionalInformation<SentenceAdditionalInformation>(domainEvent)
        log.info { "Received sentence.updated event with additionalInformation $additionalInformation" }
      }
      CHARGE_INSERTED -> {
        val additionalInformation = eventAdditionalInformation<ChargeAdditionalInformation>(domainEvent)
        log.info { "Received charge.inserted event with additionalInformation $additionalInformation" }
      }
      CHARGE_DELETED -> {
        val additionalInformation = eventAdditionalInformation<ChargeAdditionalInformation>(domainEvent)
        log.info { "Received charge.deleted event with additionalInformation $additionalInformation" }
      }
      CHARGE_UPDATED -> {
        val additionalInformation = eventAdditionalInformation<ChargeAdditionalInformation>(domainEvent)
        log.info { "Received charge.updated event with additionalInformation $additionalInformation" }
      }
    }
  }

  private inline fun <reified T : AdditionalInformation> eventAdditionalInformation(domainEvent: HmppsDomainEvent): T = jsonMapper.readValue(domainEvent.additionalInformation, T::class.java)
}
