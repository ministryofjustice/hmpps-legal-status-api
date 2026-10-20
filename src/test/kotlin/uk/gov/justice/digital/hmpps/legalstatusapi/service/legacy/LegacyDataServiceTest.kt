package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.legalstatusapi.client.prisonersearch.PrisonerNotFoundException
import uk.gov.justice.digital.hmpps.legalstatusapi.client.prisonersearch.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.legalstatusapi.client.prisonersearch.PrisonerSearchApiException
import uk.gov.justice.digital.hmpps.legalstatusapi.client.prisonersearch.assertThat
import uk.gov.justice.digital.hmpps.legalstatusapi.client.remandandsentencing.RemandAndSentencingApiClient
import uk.gov.justice.digital.hmpps.legalstatusapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.legalstatusapi.uk.gov.justice.digital.hmpps.prisonersearch.resource.model.aValidPrisoner
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class LegacyDataServiceTest {

  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()

  private val remandAndSentencingApiClient: RemandAndSentencingApiClient = mock()

  private val fixedTimestamp = Instant.parse("2026-04-17T09:13:22.123Z")
  private val clock = Clock.fixed(fixedTimestamp, ZoneId.of("UTC"))

  private val legacyDataService = LegacyDataService(
    prisonerSearchApiClient,
    remandAndSentencingApiClient,
    clock,
  )

  @Test
  fun `should calculate Imprisonment Status`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    val receptionDate = LocalDate.now().minusMonths(3)
    val imprisonmentStatus = "14FTRHDC_ORA"

    val prisoner = aValidPrisoner(
      prisonNumber = prisonNumber,
      receptionDate = receptionDate,
      imprisonmentStatus = imprisonmentStatus,
    )
    given(prisonerSearchApiClient.getPrisoner(any())).willReturn(prisoner)

    // When
    val actual = legacyDataService.calculateImprisonmentStatus(prisonNumber)

    // Then
    verify(prisonerSearchApiClient).getPrisoner(prisonNumber)

    // Assert 'actual' here once the full implementation is complete
  }

  @Test
  fun `should not calculate Imprisonment Status given prisoner-search throws not found exception`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()

    given(prisonerSearchApiClient.getPrisoner(any())).willThrow(PrisonerNotFoundException(prisonNumber))

    // When
    val actual = catchThrowableOfType(PrisonerNotFoundException::class.java) {
      legacyDataService.calculateImprisonmentStatus(prisonNumber)
    }

    // Then
    assertThat(actual).hasPrisonNumber(prisonNumber)
    verify(prisonerSearchApiClient).getPrisoner(prisonNumber)
  }

  @Test
  fun `should not calculate Imprisonment Status given prisoner-search throws API exception`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()

    val cause = RuntimeException("Some exception thrown by the API")
    given(prisonerSearchApiClient.getPrisoner(any())).willThrow(PrisonerSearchApiException("Error retrieving prisoner by prisonNumber $prisonNumber", cause))

    // When
    val actual = catchThrowableOfType(PrisonerSearchApiException::class.java) {
      legacyDataService.calculateImprisonmentStatus(prisonNumber)
    }

    // Then
    assertThat(actual)
      .hasMessage("Error retrieving prisoner by prisonNumber $prisonNumber")
      .hasCause(cause)
    verify(prisonerSearchApiClient).getPrisoner(prisonNumber)
  }
}
