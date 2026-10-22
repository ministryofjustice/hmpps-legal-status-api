package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import mu.KotlinLogging
import org.springframework.stereotype.Component

const val IMPRISONMENT_STATUS_CHANGED = "prison-offender-events.prisoner.imprisonment-status-changed"

private val log = KotlinLogging.logger {}

/**
 * Handles [IMPRISONMENT_STATUS_CHANGED], raised when NOMIS recalculates a booking's imprisonment status.
 * Currently a no-op that logs the event, so that messages are consumed and removed from the queue.
 */
@Component
class ImprisonmentStatusChangedEventHandler {

  fun handle(event: HmppsDomainEvent) {
    log.info {
      "Processed ${event.eventType} event (no-op): prisonNumber=${event.prisonNumber()}, " +
        "bookingId=${event.additionalInformation?.get("bookingId")}, occurredAt=${event.occurredAt}"
    }
  }
}
