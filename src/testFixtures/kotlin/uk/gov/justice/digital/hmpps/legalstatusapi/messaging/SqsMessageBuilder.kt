package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import tools.jackson.module.kotlin.jacksonObjectMapper
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.IMPRISONMENT_STATUS_CHANGED
import uk.gov.justice.digital.hmpps.legalstatusapi.randomValidPrisonNumber
import java.time.Instant
import java.util.UUID

fun aValidHmppsDomainEventsSqsMessage(
  prisonNumber: String = randomValidPrisonNumber(),
  eventType: EventType = IMPRISONMENT_STATUS_CHANGED,
  occurredAt: Instant = Instant.now().minusSeconds(10),
  publishedAt: Instant = Instant.now(),
  description: String = "A prisoner's imprisonment status has been changed",
  version: String = "1.0",
  bookingId: Long? = null,
  additionalInformation: AdditionalInformation =
    when (eventType) {
      IMPRISONMENT_STATUS_CHANGED -> aValidImprisonmentStatusChangedAdditionalInformation(bookingId = bookingId ?: 1240469)
      EventType.SENTENCE_INSERTED -> aValidSentenceAdditionalInformation()
      EventType.SENTENCE_DELETED -> aValidSentenceAdditionalInformation()
      EventType.SENTENCE_UPDATED -> aValidSentenceAdditionalInformation()
      EventType.CHARGE_INSERTED -> aValidChargeAdditionalInformation()
      EventType.CHARGE_DELETED -> aValidChargeAdditionalInformation()
      EventType.CHARGE_UPDATED -> aValidChargeAdditionalInformation()
    },
): SqsMessage = SqsMessage(
  type = "Notification",
  message = """
        {
          "eventType": "${eventType.eventType}",
          "personReference": { "identifiers": [ { "type": "NOMS", "value": "$prisonNumber" } ] },
          "occurredAt": "$occurredAt",
          "publishedAt": "$publishedAt",
          "description": "$description",
          "version": "$version",
          "additionalInformation": ${jacksonObjectMapper().writeValueAsString(additionalInformation)}
        }        
  """.trimIndent(),
  messageId = UUID.randomUUID(),
)
