package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import tools.jackson.module.kotlin.jacksonObjectMapper
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.CHARGE_DELETED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.CHARGE_INSERTED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.CHARGE_UPDATED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.IMPRISONMENT_STATUS_CHANGED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.IMPRISONMENT_STATUS_COMPUTABLE
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.SENTENCE_DELETED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.SENTENCE_INSERTED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.SENTENCE_UPDATED
import uk.gov.justice.digital.hmpps.legalstatusapi.randomValidPrisonNumber
import java.time.Instant
import java.util.UUID

fun aValidHmppsDomainEventsSqsMessage(
  prisonNumber: String = randomValidPrisonNumber(),
  eventType: EventType = IMPRISONMENT_STATUS_CHANGED,
  occurredAt: Instant = Instant.now().minusSeconds(10),
  description: String = "A prisoner's imprisonment status has been changed",
  version: String = "1.0",
  bookingId: Long? = null,
  additionalInformation: AdditionalInformation =
    when (eventType) {
      IMPRISONMENT_STATUS_CHANGED -> aValidImprisonmentStatusChangedAdditionalInformation(bookingId = bookingId ?: 1240469)
      SENTENCE_INSERTED, SENTENCE_DELETED, SENTENCE_UPDATED, CHARGE_INSERTED, CHARGE_DELETED, CHARGE_UPDATED -> aValidSentenceAdditionalInformation()
      IMPRISONMENT_STATUS_COMPUTABLE -> aValidImprisonmentStatusComputableAdditionalInformation()
    },
): SqsMessage = SqsMessage(
  type = "Notification",
  message = """
        {
          "eventType": "${eventType.eventType}",
          "personReference": { "identifiers": [ { "type": "NOMS", "value": "$prisonNumber" } ] },
          "occurredAt": "$occurredAt",
          "description": "$description",
          "version": "$version",
          "additionalInformation": ${jacksonObjectMapper().writeValueAsString(additionalInformation)}
        }        
  """.trimIndent(),
  messageId = UUID.randomUUID(),
)
