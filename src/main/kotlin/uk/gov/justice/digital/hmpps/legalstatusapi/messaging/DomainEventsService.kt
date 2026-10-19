package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import mu.KotlinLogging
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.AdditionalInformation.ImprisonmentStatusChangedAdditionalInformation
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.DomainEventType.IMPRISONMENT_STATUS_CHANGED

private val log = KotlinLogging.logger {}

/**
 * Service class that processes SQS events. For each type the [DomainEvent]'s `additionalInformation` property is
 * deserialized into it's corresponding type, and then the [DomainEvent] and it's [AdditionalInformation] are sent
 * to the relevant service specific to that event type.
 */
@Service
class DomainEventsService(
  private val mapper: JsonMapper,
  private val imprisonmentStatusChangedEventHandler: ImprisonmentStatusChangedEventHandler,
) {

  fun process(domainEvent: DomainEvent) = with(domainEvent) {
    log.info { "Processing inbound event $eventType" }

    when (eventType) {
      IMPRISONMENT_STATUS_CHANGED -> {
        val additionalInformation =
          eventAdditionalInformation<ImprisonmentStatusChangedAdditionalInformation>(domainEvent)
        imprisonmentStatusChangedEventHandler.handle(domainEvent, additionalInformation)
      }
    }
  }

  private inline fun <reified T : AdditionalInformation> eventAdditionalInformation(domainEvent: DomainEvent): T = this.mapper.readValue(domainEvent.additionalInformation, T::class.java)
}
