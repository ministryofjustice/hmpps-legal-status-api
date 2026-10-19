package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.willThrow
import org.mockito.kotlin.any
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.test.context.bean.override.mockito.MockReset
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import uk.gov.justice.digital.hmpps.legalstatusapi.IntegrationTestBase
import uk.gov.justice.hmpps.sqs.sendMessage

@ExtendWith(OutputCaptureExtension::class)
class ImprisonmentStatusChangedEventTest : IntegrationTestBase() {

  @MockitoSpyBean(reset = MockReset.BEFORE)
  private lateinit var imprisonmentStatusChangedEventHandler: ImprisonmentStatusChangedEventHandler

  @BeforeEach
  fun clearQueues() = purgeDomainEventsQueues()

  @Test
  fun `imprisonment-status-changed event published to the domain events topic is processed and removed from the queue`(output: CapturedOutput) {
    domainEventsTopic.publish(
      domainEvent = aValidImprisonmentStatusChangedDomainEvent(prisonNumber = "A7316ED", bookingId = "1240469"),
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
  fun `imprisonment-status-changed message sent to the domain events queue is processed and removed from the queue`(output: CapturedOutput) {
    domainEventsQueue.sendMessage(
      sqsMessage = aValidSqsMessage(message = aValidImprisonmentStatusChangedDomainEvent(prisonNumber = "A7316ED", bookingId = "1240469")),
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
  fun `imprisonment-status-changed message that fails to process is moved to the DLQ`(output: CapturedOutput) {
    willThrow(RuntimeException("Some exception processing the event"))
      .given(imprisonmentStatusChangedEventHandler).handle(any(), any())

    domainEventsQueue.sendMessage(
      sqsMessage = aValidSqsMessage(message = aValidImprisonmentStatusChangedDomainEvent(prisonNumber = "A7316ED", bookingId = "1240469")),
    )

    await untilCallTo { messagesOnDomainEventsDlq() } matches { it == 1 }
    assertThat(messagesOnDomainEventsQueue()).isZero()
  }

  @Test
  fun `message that cannot be read is moved to the DLQ`() {
    domainEventsQueue.sendMessage(eventType = "Notification", event = "this is not json")

    await untilCallTo { messagesOnDomainEventsDlq() } matches { it == 1 }
    assertThat(messagesOnDomainEventsQueue()).isZero()
  }
}
