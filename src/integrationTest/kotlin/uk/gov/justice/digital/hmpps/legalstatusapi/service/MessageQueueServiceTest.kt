package uk.gov.justice.digital.hmpps.legalstatusapi.service

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.legalstatusapi.IntegrationTestBase
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.entity.assertThat
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.repository.MessageQueueItemRepository
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.Identifier
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.PersonReference
import uk.gov.justice.digital.hmpps.legalstatusapi.randomValidPrisonNumber
import java.time.Instant

class MessageQueueServiceTest : IntegrationTestBase() {
  @Autowired
  private lateinit var messageQueueService: MessageQueueService

  @Autowired
  private lateinit var messageQueueItemRepository: MessageQueueItemRepository

  @BeforeEach
  fun beforeEach() {
    messageQueueItemRepository.deleteAll()
  }

  @Test
  fun `should record new message queue item`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    val eventType = EventType.IMPRISONMENT_STATUS_CHANGED
    val occurredAt = Instant.now(clock).minusSeconds(10)

    val hmppsDomainEvent = HmppsDomainEvent(
      eventType = eventType,
      personReference = PersonReference(listOf(Identifier("NOMS", prisonNumber))),
      occurredAt = occurredAt,
      additionalInformation = "",
      description = "",
      version = "1.0",
    )

    // When
    messageQueueService.recordMessageQueueItem(hmppsDomainEvent)

    // Then
    await untilAsserted {
      assertThat(messageQueueItemRepository.count()).isEqualTo(1)
      val messageQueueItemEntity = messageQueueItemRepository.findAll().first()
      assertThat(messageQueueItemEntity)
        .hasAnId()
        .isForPrisonNumber(prisonNumber)
        .isEventType(eventType)
        .occurredAt(occurredAt)
    }
  }
}
