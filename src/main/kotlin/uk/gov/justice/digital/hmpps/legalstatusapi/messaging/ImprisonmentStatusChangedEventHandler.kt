package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

const val IMPRISONMENT_STATUS_CHANGED = "prison-offender-events.prisoner.imprisonment-status-changed"

/**
 * Handles [IMPRISONMENT_STATUS_CHANGED], raised when NOMIS recalculates a booking's imprisonment status.
 * Currently a no-op that logs the event, so that messages are consumed and removed from the queue.
 */
@Component
class ImprisonmentStatusChangedEventHandler {

  fun handle(messageId: String, event: HmppsDomainEvent) {
    log.info(
      "Processed {} event (no-op): messageId={}, prisonNumber={}, bookingId={}, occurredAt={}",
      event.eventType,
      messageId,
      event.prisonNumber(),
      event.additionalInformation?.get("bookingId"),
      event.occurredAt,
    )
  }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
