package uk.gov.justice.digital.hmpps.legalstatusapi.messaging.eventhandling

import mu.KotlinLogging
import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.CHARGE_DELETED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.CHARGE_INSERTED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.CHARGE_UPDATED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.SENTENCE_DELETED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.SENTENCE_INSERTED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.SENTENCE_UPDATED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.ImprisonmentStatusComparisonService
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.RecentImprisonmentStatusExistsException

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

    try {
      imprisonmentStatusComparisonService.compare(
        event.prisonNumber()!!,
        event.occurredAt,
      )
    } catch (e: RecentImprisonmentStatusExistsException) {
      // Handle RecentImprisonmentStatusExistsException by logging a warning and returning. SQS will see this as a successfully
      // processed message and will not retry or DLQ it.
      log.warn { "Imprisonment Status has been recently calculated for [${event.prisonNumber()}]. No need to recalculate." }
      return
    } catch (e: DataAccessException) {
      // Handle DataAccessException by logging and rethrowing so that SQS retries or DLQ's the message.
      // DataAccessException subclasses are thrown when multiple messages/threads try to calculate an Imprisonment Status
      // for the same prisoner at the same time, triggering an optimistic lock error when both threads try to create or
      // update the same record together.
      // Rethrowing the exception allows SQS to retry or DLQ the message. If it is retried it will throw RecentImprisonmentStatusExistsException
      // because the "winning" thread of the original issue will have created/updated the Imprisonment Status recently
      // and there is therefore no need to recalculate it.
      log.error { "Error with multiple messages calculating Imprisonment Status for [${event.prisonNumber()}] at the same time. Exception: [${e::class.simpleName}]" }
      throw e
    } catch (e: Exception) {
      // Catch all exception handler. Rethrow the exception so that SQS retries or DLQ's the message.
      log.error { "Error calculating Imprisonment Status for [${event.prisonNumber()}]. Exception: [${e::class.simpleName}]" }
      throw e
    }
  }
}
