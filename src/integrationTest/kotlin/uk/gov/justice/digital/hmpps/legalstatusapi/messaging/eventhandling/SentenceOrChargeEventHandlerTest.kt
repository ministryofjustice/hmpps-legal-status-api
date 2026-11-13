package uk.gov.justice.digital.hmpps.legalstatusapi.messaging.eventhandling

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataAccessException
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.legalstatusapi.IntegrationTestBase
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.entity.ImprisonmentStatusCalculatedAtEntity
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.entity.assertThat
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.repository.ImprisonmentStatusCalculatedAtRepository
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.SENTENCE_UPDATED
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.aHmppsDomainEvent
import uk.gov.justice.digital.hmpps.legalstatusapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.manageoffences.resource.model.OffenceSeverityRating
import uk.gov.justice.digital.hmpps.prisonersearch.resource.model.aValidPrisoner
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonCharges
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.aValidPersonCharge
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.aValidSentenceTypeDetails
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.CompletableFuture

@ActiveProfiles("integration-test", "ticking-clock")
class SentenceOrChargeEventHandlerTest : IntegrationTestBase() {

  @Autowired
  private lateinit var eventHandler: SentenceOrChargeEventHandler

  @Autowired
  private lateinit var imprisonmentStatusCalculatedAtRepository: ImprisonmentStatusCalculatedAtRepository

  @Autowired
  private lateinit var transactionTemplate: TransactionTemplate

  private lateinit var prisonNumber: String

  private lateinit var event: HmppsDomainEvent

  @BeforeEach
  fun beforeEach() {
    imprisonmentStatusCalculatedAtRepository.deleteAll()

    stubGetTokenFromHmppsAuth()

    prisonNumber = randomValidPrisonNumber()
    val prisoner = aValidPrisoner(prisonNumber)
    stubGetPrisonerFromPrisonerSearchApi(prisoner)

    val personCharges = PersonCharges(listOf(aValidPersonCharge()))
    stubGetChargesFromRemandAndSentencingApi(
      prisonNumber = prisonNumber,
      fromDate = prisoner.receptionDate,
      toDate = LocalDate.now(clock),
      response = personCharges,
    )
    personCharges.charges.forEach {
      it.sentence?.sentenceType?.sentenceTypeUuid?.run {
        stubGetSentenceTypeDetails(aValidSentenceTypeDetails(sentenceTypeUuid = this))
      }
    }
    personCharges.charges.map {
      OffenceSeverityRating(it.offenceCode, "10")
    }.also {
      stubGetOffenceSeverities(it.map { it.offenceCode }.toSet(), it)
    }

    event = aHmppsDomainEvent(
      prisonNumber = prisonNumber,
      eventType = SENTENCE_UPDATED,
    )
  }

  @Nested
  inner class SingleThreadTests {
    @Test
    fun `should calculate Imprisonment Status and create new ImprisonmentStatusCalculatedAtEntity given no ImprisonmentStatusCalculatedAtEntity exists`() {
      // Given
      val now = Instant.now(clock)

      // When
      eventHandler.handle(event)

      // Then
      transactionTemplate.execute {
        assertThat(imprisonmentStatusCalculatedAtRepository.count()).isEqualTo(1)
        assertThat(imprisonmentStatusCalculatedAtRepository.getReferenceById(prisonNumber))
          .hasVersion(0)
          .wasCalculatedAfter(now)
      }
    }

    @Test
    fun `should calculate Imprisonment Status and update ImprisonmentStatusCalculatedAtEntity given ImprisonmentStatusCalculatedAtEntity exists`() {
      // Given
      transactionTemplate.execute {
        imprisonmentStatusCalculatedAtRepository.save(
          ImprisonmentStatusCalculatedAtEntity(
            prisonNumber = prisonNumber,
            calculatedAt = Instant.now(clock).minus(1, ChronoUnit.HOURS),
          ),
        )
      }

      val now = Instant.now(clock)

      // When
      eventHandler.handle(event)

      // Then
      transactionTemplate.execute {
        assertThat(imprisonmentStatusCalculatedAtRepository.count()).isEqualTo(1)
        assertThat(imprisonmentStatusCalculatedAtRepository.getReferenceById(prisonNumber))
          .hasVersion(1)
          .wasCalculatedAfter(now)
      }
    }

    @Test
    fun `should not calculate Imprisonment Status given recent ImprisonmentStatusCalculatedAtEntity exists`() {
      // Given
      val originalCalculatedAt = Instant.now(clock).minus(1, ChronoUnit.SECONDS)
      transactionTemplate.execute {
        imprisonmentStatusCalculatedAtRepository.save(
          ImprisonmentStatusCalculatedAtEntity(
            prisonNumber = prisonNumber,
            calculatedAt = originalCalculatedAt,
          ),
        )
      }

      // When
      eventHandler.handle(event)

      // Then
      transactionTemplate.execute {
        assertThat(imprisonmentStatusCalculatedAtRepository.count()).isEqualTo(1)
        assertThat(imprisonmentStatusCalculatedAtRepository.getReferenceById(prisonNumber))
          .hasVersion(0)
          .wasCalculatedAt(originalCalculatedAt)
      }
    }
  }

  @Nested
  inner class ConcurrentThreadTests {
    @Test
    fun `only 1 thread should calculate Imprisonment Status and create new ImprisonmentStatusCalculatedAtEntity given no ImprisonmentStatusCalculatedAtEntity exists`() {
      // Given
      val now = Instant.now(clock)

      // When
      val completableFuture1 = CompletableFuture.supplyAsync { runCatching { eventHandler.handle(event) } }
      val completableFuture2 = CompletableFuture.supplyAsync { runCatching { eventHandler.handle(event) } }

      // Then
      await untilAsserted {
        assertThat(
          // Both CompletableFutures should be done, and one should be a success and one should be a failure (exception)
          (completableFuture1.isDone && completableFuture2.isDone) &&
            (completableFuture1.get().isSuccess && completableFuture2.get().isFailure) ||
            (completableFuture1.get().isFailure && completableFuture2.get().isSuccess),
        ).isTrue
      }

      val completableFuture1Result = completableFuture1.get()
      val completableFuture2Result = completableFuture2.get()
      // Either CompletableFuture 1 or 2 will be an exception - assert that it is the correct type. It will be one of the subclasses of DataAccessException depending on exactly which part failed
      if (completableFuture1Result.isFailure) {
        assertThat(completableFuture1Result.exceptionOrNull()).isInstanceOf(DataAccessException::class.java)
      } else {
        assertThat(completableFuture2Result.exceptionOrNull()).isInstanceOf(DataAccessException::class.java)
      }

      transactionTemplate.execute {
        assertThat(imprisonmentStatusCalculatedAtRepository.count()).isEqualTo(1)
        assertThat(imprisonmentStatusCalculatedAtRepository.getReferenceById(prisonNumber))
          .hasVersion(0)
          .wasCalculatedAfter(now)
      }
    }

    @Test
    fun `only 1 thread should calculate Imprisonment Status and update ImprisonmentStatusCalculatedAtEntity given ImprisonmentStatusCalculatedAtEntity exists`() {
      // Given
      transactionTemplate.execute {
        imprisonmentStatusCalculatedAtRepository.save(
          ImprisonmentStatusCalculatedAtEntity(
            prisonNumber = prisonNumber,
            calculatedAt = Instant.now(clock).minus(1, ChronoUnit.HOURS),
          ),
        )
      }

      val now = Instant.now(clock)

      // When
      val completableFuture1 = CompletableFuture.supplyAsync { runCatching { eventHandler.handle(event) } }
      val completableFuture2 = CompletableFuture.supplyAsync { runCatching { eventHandler.handle(event) } }

      // Then
      await untilAsserted {
        assertThat(
          // Both CompletableFutures should be done, and one should be a success and one should be a failure (exception)
          (completableFuture1.isDone && completableFuture2.isDone) &&
            (completableFuture1.get().isSuccess && completableFuture2.get().isFailure) ||
            (completableFuture1.get().isFailure && completableFuture2.get().isSuccess),
        ).isTrue
      }

      transactionTemplate.execute {
        assertThat(imprisonmentStatusCalculatedAtRepository.count()).isEqualTo(1)
        assertThat(imprisonmentStatusCalculatedAtRepository.getReferenceById(prisonNumber))
          .hasVersion(1)
          .wasCalculatedAfter(now)
      }
    }

    @Test
    fun `no thread should calculate Imprisonment Status given recent ImprisonmentStatusCalculatedAtEntity exists`() {
      // Given
      val originalCalculatedAt = Instant.now(clock).minus(1, ChronoUnit.SECONDS)
      transactionTemplate.execute {
        imprisonmentStatusCalculatedAtRepository.save(
          ImprisonmentStatusCalculatedAtEntity(
            prisonNumber = prisonNumber,
            calculatedAt = originalCalculatedAt,
          ),
        )
      }

      // When
      val completableFuture1 = CompletableFuture.supplyAsync { runCatching { eventHandler.handle(event) } }
      val completableFuture2 = CompletableFuture.supplyAsync { runCatching { eventHandler.handle(event) } }

      // Then
      await untilAsserted {
        assertThat(
          // Both CompletableFutures should be done, and both should be a success, even though neither calculated the Imprisonment Status
          (completableFuture1.isDone && completableFuture2.isDone) &&
            (completableFuture1.get().isSuccess && completableFuture2.get().isSuccess),
        ).isTrue
      }

      transactionTemplate.execute {
        assertThat(imprisonmentStatusCalculatedAtRepository.count()).isEqualTo(1)
        assertThat(imprisonmentStatusCalculatedAtRepository.getReferenceById(prisonNumber))
          .hasVersion(0)
          .wasCalculatedAt(originalCalculatedAt)
      }
    }
  }
}
