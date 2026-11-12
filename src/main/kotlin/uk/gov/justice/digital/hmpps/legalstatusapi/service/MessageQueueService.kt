package uk.gov.justice.digital.hmpps.legalstatusapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.entity.MessageQueueItemEntity
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.repository.MessageQueueItemRepository
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.HmppsDomainEvent

/**
 * Service class responsible for actions related to managing [MessageQueueItemEntity]s via the [MessageQueueItemRepository]
 */
@Service
class MessageQueueService(
  private val messageQueueItemRepository: MessageQueueItemRepository,
) {

  /**
   * Persists a new [MessageQueueItemEntity] created from the specified [HmppsDomainEvent]
   */
  @Transactional
  fun recordMessageQueueItem(hmppsDomainEvent: HmppsDomainEvent) {
    val messageQueueItem = with(hmppsDomainEvent) {
      MessageQueueItemEntity(
        prisonNumber = prisonNumber()!!,
        eventType = eventType,
        occurredAt = occurredAt,
      )
    }
    messageQueueItemRepository.save(messageQueueItem)
  }
}
