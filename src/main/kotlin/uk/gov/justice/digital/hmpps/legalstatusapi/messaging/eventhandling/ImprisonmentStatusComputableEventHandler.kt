package uk.gov.justice.digital.hmpps.legalstatusapi.messaging.eventhandling

import mu.KotlinLogging
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.IMPRISONMENT_STATUS_COMPUTABLE
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.ImprisonmentStatusComparisonService

private val log = KotlinLogging.logger {}

/**
 * Handles [IMPRISONMENT_STATUS_COMPUTABLE], raised as an internal event when a prisoner's imprisonment status is ready for calculation.
 */
@Component
class ImprisonmentStatusComputableEventHandler(
  private val imprisonmentStatusComparisonService: ImprisonmentStatusComparisonService,
) : HmppsDomainEventHandler {

  override fun handle(event: HmppsDomainEvent) {
    log.info {
      "Processing ${event.eventType} event: prisonNumber=${event.prisonNumber()}, occurredAt=${event.occurredAt}"
    }

    imprisonmentStatusComparisonService.compare(
      event.prisonNumber()!!,
      event.occurredAt,
    )
  }
}
