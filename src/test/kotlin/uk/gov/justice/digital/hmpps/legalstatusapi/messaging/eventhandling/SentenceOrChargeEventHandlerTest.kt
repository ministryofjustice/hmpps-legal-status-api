package uk.gov.justice.digital.hmpps.legalstatusapi.messaging.eventhandling

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.springframework.dao.DataAccessException
import org.springframework.dao.DuplicateKeyException
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.aHmppsDomainEvent
import uk.gov.justice.digital.hmpps.legalstatusapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.ImprisonmentStatusComparisonService
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.RecentImprisonmentStatusExistsException
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

  @Test
  fun `should handle event and not rethrow given comparison service throws RecentImprisonmentStatusExistsException`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    val occurredAt = Instant.now()

    val domainEvent = aHmppsDomainEvent(
      eventType = EventType.CHARGE_DELETED,
      prisonNumber = prisonNumber,
      occurredAt = occurredAt,
    )

    given(imprisonmentStatusComparisonService.compare(any(), any()))
      .willThrow(RecentImprisonmentStatusExistsException(prisonNumber, occurredAt))

    // When
    handler.handle(domainEvent)

    // Then
    verify(imprisonmentStatusComparisonService).compare(
      prisonNumber = prisonNumber,
      nomisCalculatedAt = occurredAt,
    )
  }

  @Test
  fun `should handle event and rethrow given comparison service throws DataAccessException`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    val occurredAt = Instant.now()

    val domainEvent = aHmppsDomainEvent(
      eventType = EventType.CHARGE_DELETED,
      prisonNumber = prisonNumber,
      occurredAt = occurredAt,
    )

    val exception = DuplicateKeyException("Primary key violation")
    given(imprisonmentStatusComparisonService.compare(any(), any())).willThrow(exception)

    // When
    val actual = catchThrowableOfType(DataAccessException::class.java, { handler.handle(domainEvent) })

    // Then
    assertThat(actual).isEqualTo(exception)
    verify(imprisonmentStatusComparisonService).compare(
      prisonNumber = prisonNumber,
      nomisCalculatedAt = occurredAt,
    )
  }

  @Test
  fun `should handle event and rethrow given comparison service throws other exception`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    val occurredAt = Instant.now()

    val domainEvent = aHmppsDomainEvent(
      eventType = EventType.CHARGE_DELETED,
      prisonNumber = prisonNumber,
      occurredAt = occurredAt,
    )

    val exception = RuntimeException("Some other error not specifically handled")
    given(imprisonmentStatusComparisonService.compare(any(), any())).willThrow(exception)

    // When
    val actual = catchThrowableOfType(RuntimeException::class.java, { handler.handle(domainEvent) })

    // Then
    assertThat(actual).isEqualTo(exception)
    verify(imprisonmentStatusComparisonService).compare(
      prisonNumber = prisonNumber,
      nomisCalculatedAt = occurredAt,
    )
  }
}
