package uk.gov.justice.digital.hmpps.legalstatusapi.messaging.eventhandling

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.aHmppsDomainEvent
import uk.gov.justice.digital.hmpps.legalstatusapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.ImprisonmentStatusComparisonService
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class SentenceOrChargeEventHandlerTest {

  @InjectMocks
  private lateinit var handler: SentenceOrChargeEventHandler

  @Mock
  private lateinit var imprisonmentStatusComparisonService: ImprisonmentStatusComparisonService

  @Test
  fun `should handle event`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    val occurredAt = Instant.now()

    val domainEvent = aHmppsDomainEvent(
      eventType = EventType.CHARGE_DELETED,
      prisonNumber = prisonNumber,
      occurredAt = occurredAt,
    )

    // When
    handler.handle(domainEvent)

    // Then
    verify(imprisonmentStatusComparisonService).compare(
      prisonNumber = prisonNumber,
      nomisCalculatedAt = occurredAt,
    )
  }
}
