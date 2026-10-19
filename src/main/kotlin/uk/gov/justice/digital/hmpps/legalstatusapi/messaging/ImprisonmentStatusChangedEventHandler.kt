package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import mu.KotlinLogging
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.AdditionalInformation.ImprisonmentStatusChangedAdditionalInformation
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.DomainEventType.IMPRISONMENT_STATUS_CHANGED

private val log = KotlinLogging.logger {}

/**
 * Handles [IMPRISONMENT_STATUS_CHANGED], raised when NOMIS recalculates a booking's imprisonment status.
 * Currently a no-op that logs the event, so that messages are consumed and removed from the queue.
 */
@Component
class ImprisonmentStatusChangedEventHandler {

  fun handle(domainEvent: DomainEvent, additionalInformation: ImprisonmentStatusChangedAdditionalInformation) {
    with(domainEvent) {
      log.info {
        "Processed $eventType event (no-op): prisonNumber=${prisonNumber()}, " +
          "bookingId=${additionalInformation.bookingId}, occurredAt=$occurredAt"
      }
    }
  }
}
