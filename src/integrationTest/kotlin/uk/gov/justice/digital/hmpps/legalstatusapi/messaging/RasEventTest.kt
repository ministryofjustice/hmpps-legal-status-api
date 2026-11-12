package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.legalstatusapi.IntegrationTestBase
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.entity.ImprisonmentStatusCalculatedAtEntity
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.entity.assertThat
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.repository.ImprisonmentStatusCalculatedAtRepository
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.EventType.SENTENCE_UPDATED
import uk.gov.justice.digital.hmpps.legalstatusapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.manageoffences.resource.model.OffenceSeverityRating
import uk.gov.justice.digital.hmpps.prisonersearch.resource.model.aValidPrisoner
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonCharges
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.aValidPersonCharge
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.aValidSentenceTypeDetails
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Integration tests for the end-to-end message handling of RaS events.
 * The tests in this class only use one type of RaS event (SENTENCE_UPDATED), but the exact same behaviour
 * would exist for the other RaS events as they are all handled by the same HmppsDomainEventHandler class.
 */
@ActiveProfiles("integration-test", "ticking-clock")
class RasEventTest : IntegrationTestBase() {

  @Autowired
  private lateinit var imprisonmentStatusCalculatedAtRepository: ImprisonmentStatusCalculatedAtRepository

  @Autowired
  private lateinit var transactionTemplate: TransactionTemplate

  private lateinit var prisonNumber: String

  @BeforeEach
  fun beforeEach() {
    messagingHelper.purgeDomainEventsQueues()
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
  }

  @Test
  fun `should calculate Imprisonment Status and create new ImprisonmentStatusCalculatedAtEntity given no ImprisonmentStatusCalculatedAtEntity exists`() {
    // Given
    val now = Instant.now(clock)

    val sqsMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = prisonNumber,
      eventType = SENTENCE_UPDATED,
    )

    // When
    messagingHelper.sendDomainEvent(sqsMessage)

    // Then
    await untilCallTo { messagingHelper.messagesOnDomainEventsQueue() } matches { it == 0 }
    assertThat(messagingHelper.messagesOnDomainEventsDlq()).isZero()

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

    val sqsMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = prisonNumber,
      eventType = SENTENCE_UPDATED,
    )

    // When
    messagingHelper.sendDomainEvent(sqsMessage)

    // Then
    await untilCallTo { messagingHelper.messagesOnDomainEventsQueue() } matches { it == 0 }
    assertThat(messagingHelper.messagesOnDomainEventsDlq()).isZero()

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

    val sqsMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = prisonNumber,
      eventType = SENTENCE_UPDATED,
    )

    // When
    messagingHelper.sendDomainEvent(sqsMessage)

    // Then
    await untilCallTo { messagingHelper.messagesOnDomainEventsQueue() } matches { it == 0 }
    assertThat(messagingHelper.messagesOnDomainEventsDlq()).isZero()

    transactionTemplate.execute {
      assertThat(imprisonmentStatusCalculatedAtRepository.count()).isEqualTo(1)
      assertThat(imprisonmentStatusCalculatedAtRepository.getReferenceById(prisonNumber))
        .hasVersion(0)
        .wasCalculatedAt(originalCalculatedAt)
    }
  }
}
