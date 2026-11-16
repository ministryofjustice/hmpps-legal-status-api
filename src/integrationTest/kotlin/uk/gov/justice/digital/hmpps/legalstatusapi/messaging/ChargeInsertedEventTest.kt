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
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.CHARGE_INSERTED
import uk.gov.justice.digital.hmpps.legalstatusapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.legalstatusapi.wiremock.HmppsPersonLocationApiExtension.Companion.hmppsPersonLocationApi
import uk.gov.justice.digital.hmpps.personlocation.resource.model.aValidPeriodOfCustody
import uk.gov.justice.digital.hmpps.personlocation.resource.model.aValidPreviousPeriod
import uk.gov.justice.digital.hmpps.prisonersearch.resource.model.aValidPrisoner
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonCharges
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(OutputCaptureExtension::class)
class ChargeInsertedEventTest : IntegrationTestBase() {

  @BeforeEach
  fun clearQueues() = messagingHelper.purgeDomainEventsQueues()

  @Test
  fun `charge inserted event published to the domain events topic is processed and removed from the queue`(output: CapturedOutput) {
    // Given
    stubGetTokenFromHmppsAuth()

    val prisonNumber = randomValidPrisonNumber()
    val prisoner = aValidPrisoner(prisonNumber)
    stubGetPrisonerFromPrisonerSearchApi(prisoner)
    stubGetLatestPeriodOfCustodyFromPersonLocationApi(
      prisonNumber,
      aValidPeriodOfCustody(previousPeriod = aValidPreviousPeriod(concludedAt = OffsetDateTime.parse("2025-01-20T09:00:00Z"))),
    )
    // The day after the previous period of custody concluded
    stubGetChargesFromRemandAndSentencingApi(
      prisonNumber = prisonNumber,
      fromDate = LocalDate.parse("2025-01-21"),
      toDate = LocalDate.now(),
      response = PersonCharges(charges = emptyList()),
    )

    val sqsMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = prisonNumber,
      eventType = CHARGE_INSERTED,
      additionalInformation = aValidChargeAdditionalInformation(),
    )

    // When
    messagingHelper.sendDomainEvent(sqsMessage)

    // Then
    await untilAsserted {
      assertThat(output.out)
        .contains("Processing CHARGE_INSERTED event")
        .contains("prisonNumber=$prisonNumber")
    }
    await untilCallTo { messagingHelper.messagesOnDomainEventsQueue() } matches { it == 0 }
    assertThat(messagingHelper.messagesOnDomainEventsDlq()).isZero()
  }

  @Test
  fun `charge inserted event published to the domain events topic fails processing and is moved to the DLQ`(output: CapturedOutput) {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    // no stubs are setup, so retrieving the prisoner from prisoner-search-api will fail, which will result in the message ending up on the DLQ

    val sqsMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = prisonNumber,
      eventType = CHARGE_INSERTED,
      additionalInformation = aValidChargeAdditionalInformation(),
    )

    // When
    messagingHelper.sendDomainEvent(sqsMessage)

    // Then
    await untilAsserted {
      assertThat(output.out)
        .contains("Processing CHARGE_INSERTED event")
        .contains("prisonNumber=$prisonNumber")
    }
    await untilCallTo { messagingHelper.messagesOnDomainEventsQueue() } matches { it == 0 }
    await untilCallTo { messagingHelper.messagesOnDomainEventsDlq() } matches { it == 1 }
  }

  @Test
  fun `charge inserted event for a prisoner with no period of custody is moved to the DLQ`(output: CapturedOutput) {
    // Given
    stubGetTokenFromHmppsAuth()

    val prisonNumber = randomValidPrisonNumber()
    stubGetPrisonerFromPrisonerSearchApi(aValidPrisoner(prisonNumber))
    hmppsPersonLocationApi.stubGetLatestPeriodOfCustodyNotFound(prisonNumber)

    val sqsMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = prisonNumber,
      eventType = CHARGE_INSERTED,
      additionalInformation = aValidChargeAdditionalInformation(),
    )

    // When
    messagingHelper.sendDomainEvent(sqsMessage)

    // Then
    await untilAsserted {
      assertThat(output.out).contains("Period of custody for prisoner [$prisonNumber] not returned by Person Location API")
    }
    await untilCallTo { messagingHelper.messagesOnDomainEventsQueue() } matches { it == 0 }
    await untilCallTo { messagingHelper.messagesOnDomainEventsDlq() } matches { it == 1 }
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
