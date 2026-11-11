package uk.gov.justice.digital.hmpps.legalstatusapi.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.entity.aMessageQueueItemEntity
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.repository.MessageQueueItemRepository
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.Identifier
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.PersonReference
import uk.gov.justice.digital.hmpps.legalstatusapi.randomValidPrisonNumber
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class MessageQueueServiceTest {

  @InjectMocks
  private lateinit var service: MessageQueueService

  @Mock
  private lateinit var messageQueueRepository: MessageQueueItemRepository

  @Test
  fun `should record new message queue item`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    val eventType = EventType.IMPRISONMENT_STATUS_CHANGED
    val occurredAt = Instant.now().minusSeconds(10)

    val hmppsDomainEvent = HmppsDomainEvent(
      eventType = eventType,
      personReference = PersonReference(listOf(Identifier("NOMS", prisonNumber))),
      occurredAt = occurredAt,
      additionalInformation = "",
      description = "",
      version = "1.0",
    )

    val expectedMessageQueueItemEntity = aMessageQueueItemEntity(
      id = null,
      prisonNumber = prisonNumber,
      eventType = eventType,
      occurredAt = occurredAt,
    )

    // When
    service.recordMessageQueueItem(hmppsDomainEvent)

    // Then
    verify(messageQueueRepository).save(expectedMessageQueueItemEntity)
  }
}
