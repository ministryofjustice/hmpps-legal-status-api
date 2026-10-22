package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import uk.gov.justice.digital.hmpps.legalstatusapi.IntegrationTestBase
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.IMPRISONMENT_STATUS_CHANGED
import uk.gov.justice.hmpps.sqs.publish
import uk.gov.justice.hmpps.sqs.sendMessage
import java.util.UUID

@ExtendWith(OutputCaptureExtension::class)
class ImprisonmentStatusChangedEventTest : IntegrationTestBase() {

  @BeforeEach
  fun clearQueues() = purgeDomainEventsQueues()

  @Test
  fun `imprisonment-status-changed event published to the domain events topic is processed and removed from the queue`(output: CapturedOutput) {
    domainEventsTopic.publish(
      eventType = IMPRISONMENT_STATUS_CHANGED.eventType,
      event = domainEvent(eventType = IMPRISONMENT_STATUS_CHANGED.eventType, prisonNumber = "A7316ED", bookingId = 1240469),
    )

    await untilAsserted {
      assertThat(output.out)
        .contains("Processed IMPRISONMENT_STATUS_CHANGED event (no-op)")
        .contains("prisonNumber=A7316ED, bookingId=1240469")
    }
    await untilCallTo { messagesOnDomainEventsQueue() } matches { it == 0 }
    assertThat(messagesOnDomainEventsDlq()).isZero()
  }

  @Test
  fun `message that cannot be read is moved to the DLQ`() {
    domainEventsQueue.sendMessage(eventType = IMPRISONMENT_STATUS_CHANGED.eventType, event = "this is not json")

    await untilCallTo { messagesOnDomainEventsDlq() } matches { it == 1 }
    assertThat(messagesOnDomainEventsQueue()).isZero()
  }

  private fun domainEvent(eventType: String, prisonNumber: String, bookingId: Long) =
    """
    {
      "eventType": "$eventType",
      "version": 1,
      "description": "A prisoner's imprisonment status has been changed",
      "occurredAt": "2026-09-28T12:37:46+01:00",
      "publishedAt": "2026-09-28T12:37:46.175375149+01:00",
      "personReference": { "identifiers": [ { "type": "NOMS", "value": "$prisonNumber" } ] },
      "additionalInformation": { "bookingId": $bookingId }
    }
    """.trimIndent()

  private fun snsEnvelope(message: String) =
    """
    {
      "Type": "Notification",
      "MessageId": "${UUID.randomUUID()}",
      "Message": ${jsonString(message)}
    }
    """.trimIndent()

  private fun jsonString(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
}
