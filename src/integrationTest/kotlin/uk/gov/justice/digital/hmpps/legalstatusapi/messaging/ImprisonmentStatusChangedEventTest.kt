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
import java.util.UUID

@ExtendWith(OutputCaptureExtension::class)
class ImprisonmentStatusChangedEventTest : IntegrationTestBase() {

  @BeforeEach
  fun clearQueues() = messagingHelper.purgeDomainEventsQueues()

  @Test
  fun `imprisonment-status-changed event published to the domain events topic is processed and removed from the queue`(output: CapturedOutput) {
    // Given
    val sqsMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = "A7316ED",
      eventType = IMPRISONMENT_STATUS_CHANGED,
      additionalInformation = aValidImprisonmentStatusChangedAdditionalInformation(bookingId = "1240469"),
    )

    // When
    messagingHelper.sendDomainEvent(sqsMessage)

    // Then
    await untilAsserted {
      assertThat(output.out)
        .contains("Processed IMPRISONMENT_STATUS_CHANGED event (no-op)")
        .contains("prisonNumber=A7316ED, bookingId=1240469")
    }
    await untilCallTo { messagingHelper.messagesOnDomainEventsQueue() } matches { it == 0 }
    assertThat(messagingHelper.messagesOnDomainEventsDlq()).isZero()
  }

  @Test
  fun `message that cannot be read is moved to the DLQ`() {
    // Given
    val sqsMessage = SqsMessage(
      type = "Notification",
      message = "this is not json",
      messageId = UUID.randomUUID(),
    )
    // When
    messagingHelper.sendDomainEvent(sqsMessage)

    // Then
    await untilCallTo { messagingHelper.messagesOnDomainEventsDlq() } matches { it == 1 }
    assertThat(messagingHelper.messagesOnDomainEventsQueue()).isZero()
  }
}
