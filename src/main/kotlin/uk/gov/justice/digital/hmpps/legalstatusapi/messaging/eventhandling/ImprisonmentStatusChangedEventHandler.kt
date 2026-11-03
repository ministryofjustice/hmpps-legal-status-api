package uk.gov.justice.digital.hmpps.legalstatusapi.messaging.eventhandling

import mu.KotlinLogging
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.AdditionalInformation
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.AdditionalInformation.ImprisonmentStatusChangedAdditionalInformation
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.IMPRISONMENT_STATUS_CHANGED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.ImprisonmentStatusComparisonService

private val log = KotlinLogging.logger {}

/**
 * Handles [IMPRISONMENT_STATUS_CHANGED], raised when NOMIS recalculates a booking's imprisonment status.
 */
@Component
class ImprisonmentStatusChangedEventHandler(
  private val jsonMapper: JsonMapper,
  private val imprisonmentStatusComparisonService: ImprisonmentStatusComparisonService,
) : HmppsDomainEventHandler {

  override fun handle(event: HmppsDomainEvent) {
    val additionalInformation = eventAdditionalInformation<ImprisonmentStatusChangedAdditionalInformation>(event)
    log.info {
      "Processing ${event.eventType} event: prisonNumber=${event.prisonNumber()}, " +
        "bookingId=${additionalInformation.bookingId}, occurredAt=${event.occurredAt}"
    }

    imprisonmentStatusComparisonService.compare(
      event.prisonNumber()!!,
      event.occurredAt,
    )
  }

  private inline fun <reified T : AdditionalInformation> eventAdditionalInformation(event: HmppsDomainEvent): T = jsonMapper.readValue(event.additionalInformation, T::class.java)
}
