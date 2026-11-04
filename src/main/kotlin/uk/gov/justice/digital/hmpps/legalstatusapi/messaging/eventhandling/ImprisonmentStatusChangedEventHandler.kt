package uk.gov.justice.digital.hmpps.legalstatusapi.messaging.eventhandling

import mu.KotlinLogging
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.legalstatusapi.client.prisonersearch.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.IMPRISONMENT_STATUS_CHANGED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.HmppsDomainEvent

private val log = KotlinLogging.logger {}

/**
 * Handles [IMPRISONMENT_STATUS_CHANGED], raised when NOMIS recalculates a booking's imprisonment status.
 */
@Component
class ImprisonmentStatusChangedEventHandler(
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
) : HmppsDomainEventHandler {

  override fun handle(event: HmppsDomainEvent) {
    val prisoner = prisonerSearchApiClient.getPrisoner(event.prisonNumber()!!)

    log.info {
      "Processing ${event.eventType} event: prisonNumber=${event.prisonNumber()}, occurredAt=${event.occurredAt} " +
        "nomisImprisonmentStatus=${prisoner.imprisonmentStatus}"
    }
  }
}
