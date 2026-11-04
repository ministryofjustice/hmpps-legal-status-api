package uk.gov.justice.digital.hmpps.legalstatusapi.messaging.eventhandling

import mu.KotlinLogging
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.CHARGE_DELETED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.CHARGE_INSERTED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.CHARGE_UPDATED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.SENTENCE_DELETED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.SENTENCE_INSERTED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.SENTENCE_UPDATED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.ImprisonmentStatusComparisonService

private val log = KotlinLogging.logger {}

/**
 * Handles [SENTENCE_INSERTED], [SENTENCE_DELETED], [SENTENCE_UPDATED], [CHARGE_INSERTED], [CHARGE_DELETED],
 * [CHARGE_UPDATED], raised when RaS marks any change in Sentences or Charges.
 */
@Component
class SentenceOrChargeEventHandler(
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
