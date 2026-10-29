package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import mu.KotlinLogging
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.AdditionalInformation.ImprisonmentStatusChangedAdditionalInformation
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.IMPRISONMENT_STATUS_CHANGED
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.ImprisonmentStatusComparisonService

private val log = KotlinLogging.logger {}

/**
 * Handles [IMPRISONMENT_STATUS_CHANGED], raised when NOMIS recalculates a booking's imprisonment status.
 */
@Component
class ImprisonmentStatusChangedEventHandler(
  private val imprisonmentStatusComparisonService: ImprisonmentStatusComparisonService,
) {

  fun handle(event: HmppsDomainEvent, additionalInformation: ImprisonmentStatusChangedAdditionalInformation) {
    log.info {
      "Processing ${event.eventType} event: prisonNumber=${event.prisonNumber()}, " +
        "bookingId=${additionalInformation.bookingId}, occurredAt=${event.occurredAt}"
    }

    imprisonmentStatusComparisonService.compare(
      event.prisonNumber()!!,
      additionalInformation.bookingId,
      event.occurredAt,
    )
  }
}
