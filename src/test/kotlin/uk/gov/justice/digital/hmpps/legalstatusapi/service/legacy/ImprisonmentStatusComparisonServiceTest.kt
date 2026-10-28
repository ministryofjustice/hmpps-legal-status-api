package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.given
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.legalstatusapi.client.prisonersearch.PrisonerSearchApiException
import uk.gov.justice.digital.hmpps.legalstatusapi.client.slack.SlackApiClient
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.ImprisonmentStatusRepository
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatus
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatusEntity
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatusMismatchEntity
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.repository.ImprisonmentStatusMismatchRepository
import uk.gov.justice.digital.hmpps.legalstatusapi.randomValidPrisonNumber
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class ImprisonmentStatusComparisonServiceTest {

  private val legacyDataService: LegacyDataService = mock()

  private val offenceSeverityProvider: OffenceSeverityProvider = mock()

  private val imprisonmentStatusRepository: ImprisonmentStatusRepository = mock()

  private val imprisonmentStatusMismatchRepository: ImprisonmentStatusMismatchRepository = mock()

  private val slackApiClient: SlackApiClient = mock()

  private val fixedTimestamp = Instant.parse("2026-04-17T09:13:22.123Z")
  private val clock = Clock.fixed(fixedTimestamp, ZoneId.of("UTC"))

  private val savedMismatchId = UUID.randomUUID()

  private val imprisonmentStatusComparisonService = ImprisonmentStatusComparisonService(
    legacyDataService,
    offenceSeverityProvider,
    imprisonmentStatusRepository,
    imprisonmentStatusMismatchRepository,
    slackApiClient,
    clock,
  )

  @BeforeEach
  fun setUp() {
    given(imprisonmentStatusRepository.getReferenceById(any())).willAnswer { anImprisonmentStatusEntity(it.getArgument(0)) }
    // Hibernate would generate the ID on save
    given(imprisonmentStatusMismatchRepository.save(any<ImprisonmentStatusMismatchEntity>())).willAnswer { it.getArgument<ImprisonmentStatusMismatchEntity>(0).withId(savedMismatchId) }
  }

  @Test
  fun `should not record or notify a match given our Imprisonment Status is the same as NOMIS`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    given(legacyDataService.calculate(any())).willReturn(
      aLegacyImprisonmentStatusResult(prisonNumber = prisonNumber, nomisImprisonmentStatus = "SENT03", calculation = aCalculation(ImprisonmentStatus.SENT03)),
    )

    // When
    val actual = imprisonmentStatusComparisonService.compare(prisonNumber)

    // Then
    assertThat(actual).isEqualTo(ImprisonmentStatusComparisonResult.Match("SENT03"))
    verify(legacyDataService).calculate(prisonNumber)
    verifyNoInteractions(offenceSeverityProvider, imprisonmentStatusMismatchRepository, slackApiClient)
  }

  @Test
  fun `should compare using the NOMIS code rather than the enum name`() {
    // Given
    given(legacyDataService.calculate(any())).willReturn(
      aLegacyImprisonmentStatusResult(nomisImprisonmentStatus = "FTR/08", calculation = aCalculation(ImprisonmentStatus.FTR08)),
      aLegacyImprisonmentStatusResult(nomisImprisonmentStatus = "NON-CUST", calculation = aCalculation(ImprisonmentStatus.NON_CUST)),
    )

    // When
    val actual = listOf(
      imprisonmentStatusComparisonService.compare(randomValidPrisonNumber()),
      imprisonmentStatusComparisonService.compare(randomValidPrisonNumber()),
    )

    // Then
    assertThat(actual).containsExactly(
      ImprisonmentStatusComparisonResult.Match("FTR/08"),
      ImprisonmentStatusComparisonResult.Match("NON-CUST"),
    )
    verifyNoInteractions(imprisonmentStatusMismatchRepository, slackApiClient)
  }

  @Test
  fun `should record a mismatch with the calculation and its inputs given our Imprisonment Status is different to NOMIS`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    val receptionDate = LocalDate.parse("2026-01-12")
    val dpsCalculatedAt = Instant.parse("2026-04-17T09:13:20Z")
    val nomisCalculatedAt = Instant.parse("2026-04-17T09:13:01Z")
    val rasData = """{"personCharges": {"charges": [{"offenceCode": "CJ88116"}]}, "sentenceTypeDetails": {}}"""
    given(legacyDataService.calculate(any())).willReturn(
      aLegacyImprisonmentStatusResult(
        prisonNumber = prisonNumber,
        nomisImprisonmentStatus = "LR",
        receptionDate = receptionDate,
        sentencingData = LegacySentencingData(
          sentences = listOf(aSentence(sentenceCategory = "UNKNOWN", sentenceCalcType = "UNKNOWN", offenceCode = "TH68010")),
          offences = listOf(anOffence(offenceCode = "TH68010"), anOffence(offenceCode = "CJ88116", outcomeCode = "1056")),
        ),
        calculation = ImprisonmentStatusCalculation(ImprisonmentStatus.UNKNOWN, "CJ88116", CalculatedFrom.CHARGE_OUTCOME),
        rasData = rasData,
        calculatedAt = dpsCalculatedAt,
      ),
    )
    given(offenceSeverityProvider.getSeverities(any())).willReturn(mapOf("TH68010" to 46, "CJ88116" to 50))

    // When
    imprisonmentStatusComparisonService.compare(prisonNumber, bookingId = 1234567L, nomisCalculatedAt = nomisCalculatedAt)

    // Then
    verify(offenceSeverityProvider).getSeverities(listOf("TH68010", "TH68010", "CJ88116"))
    val mismatch = argumentCaptor<ImprisonmentStatusMismatchEntity>().apply { verify(imprisonmentStatusMismatchRepository).save(capture()) }.firstValue
    assertThat(mismatch.id).isNull()
    assertThat(mismatch.prisonNumber).isEqualTo(prisonNumber)
    assertThat(mismatch.bookingId).isEqualTo(1234567L)
    assertThat(mismatch.receptionDate).isEqualTo(receptionDate)
    assertThat(mismatch.nomisImprisonmentStatus).isEqualTo("LR")
    assertThat(mismatch.nomisCalculatedAt).isEqualTo(nomisCalculatedAt)
    assertThat(mismatch.dpsImprisonmentStatus.id).isEqualTo("UNKNOWN")
    assertThat(mismatch.dpsMainOffenceCode).isEqualTo("CJ88116")
    assertThat(mismatch.dpsCalculatedFrom).isEqualTo(CalculatedFrom.CHARGE_OUTCOME)
    assertThat(mismatch.dpsCalculatedAt).isEqualTo(dpsCalculatedAt)
    assertThat(mismatch.rasData).isEqualTo(rasData)
    assertThat(mismatch.offenceSeverities).isEqualTo(mapOf("TH68010" to 46, "CJ88116" to 50))
    assertThat(mismatch.nomisData).isEqualTo("{}")
    assertThat(mismatch.createdAt).isEqualTo(fixedTimestamp)
    verify(imprisonmentStatusRepository).getReferenceById("UNKNOWN")
  }

  @Test
  fun `should notify Slack with the ID of the recorded mismatch`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    given(legacyDataService.calculate(any())).willReturn(
      aLegacyImprisonmentStatusResult(prisonNumber = prisonNumber, nomisImprisonmentStatus = "LR", calculation = aCalculation(ImprisonmentStatus.UNKNOWN)),
    )

    // When
    val actual = imprisonmentStatusComparisonService.compare(prisonNumber)

    // Then
    assertThat(actual).isEqualTo(ImprisonmentStatusComparisonResult.Mismatch(savedMismatchId, nomisImprisonmentStatus = "LR", dpsImprisonmentStatus = "UNKNOWN"))
    inOrder(imprisonmentStatusMismatchRepository, slackApiClient) {
      verify(imprisonmentStatusMismatchRepository).save(any<ImprisonmentStatusMismatchEntity>())
      verify(slackApiClient).postToSlackApp(prisonNumber = prisonNumber, nomisStatus = "LR", dpsStatus = "UNKNOWN", dbReference = savedMismatchId)
    }
  }

  @Test
  fun `should record a mismatch without a booking ID or NOMIS calculation time given only the prison number`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    given(legacyDataService.calculate(any())).willReturn(
      aLegacyImprisonmentStatusResult(prisonNumber = prisonNumber, nomisImprisonmentStatus = "LR", calculation = aCalculation(ImprisonmentStatus.UNKNOWN)),
    )

    // When
    imprisonmentStatusComparisonService.compare(prisonNumber)

    // Then
    val mismatch = argumentCaptor<ImprisonmentStatusMismatchEntity>().apply { verify(imprisonmentStatusMismatchRepository).save(capture()) }.firstValue
    assertThat(mismatch.bookingId).isNull()
    assertThat(mismatch.nomisCalculatedAt).isNull()
  }

  @Test
  fun `should record a mismatch given NOMIS holds an Imprisonment Status that we do not have`() {
    // Given
    given(legacyDataService.calculate(any())).willReturn(
      aLegacyImprisonmentStatusResult(nomisImprisonmentStatus = "UNK_SENT", calculation = aCalculation(ImprisonmentStatus.SENT03)),
    )

    // When
    val actual = imprisonmentStatusComparisonService.compare(randomValidPrisonNumber())

    // Then
    assertThat(actual).isEqualTo(ImprisonmentStatusComparisonResult.Mismatch(savedMismatchId, nomisImprisonmentStatus = "UNK_SENT", dpsImprisonmentStatus = "SENT03"))
    val mismatch = argumentCaptor<ImprisonmentStatusMismatchEntity>().apply { verify(imprisonmentStatusMismatchRepository).save(capture()) }.firstValue
    assertThat(mismatch.nomisImprisonmentStatus).isEqualTo("UNK_SENT")
  }

  @Test
  fun `should still return the mismatch given Slack cannot be notified`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    given(legacyDataService.calculate(any())).willReturn(
      aLegacyImprisonmentStatusResult(prisonNumber = prisonNumber, nomisImprisonmentStatus = "LR", calculation = aCalculation(ImprisonmentStatus.UNKNOWN)),
    )
    given(slackApiClient.postToSlackApp(any(), any(), any(), any())).willThrow(RuntimeException("Slack is unavailable"))

    // When
    val actual = imprisonmentStatusComparisonService.compare(prisonNumber)

    // Then
    assertThat(actual).isEqualTo(ImprisonmentStatusComparisonResult.Mismatch(savedMismatchId, nomisImprisonmentStatus = "LR", dpsImprisonmentStatus = "UNKNOWN"))
    verify(imprisonmentStatusMismatchRepository).save(any<ImprisonmentStatusMismatchEntity>())
  }

  @Test
  fun `should not compare given Prisoner Search has no NOMIS Imprisonment Status`() {
    // Given
    given(legacyDataService.calculate(any())).willReturn(
      aLegacyImprisonmentStatusResult(nomisImprisonmentStatus = null, calculation = aCalculation(ImprisonmentStatus.SENT03)),
    )

    // When
    val actual = imprisonmentStatusComparisonService.compare(randomValidPrisonNumber())

    // Then
    assertThat(actual).isEqualTo(ImprisonmentStatusComparisonResult.NoNomisStatus)
    verifyNoInteractions(offenceSeverityProvider, imprisonmentStatusMismatchRepository, slackApiClient)
  }

  @Test
  fun `should throw the error given the Imprisonment Status cannot be calculated`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    val exception = PrisonerSearchApiException("Error retrieving prisoner by prisonNumber $prisonNumber", RuntimeException())
    given(legacyDataService.calculate(any())).willThrow(exception)

    // When
    val actual = catchThrowableOfType(PrisonerSearchApiException::class.java) {
      imprisonmentStatusComparisonService.compare(prisonNumber)
    }

    // Then
    assertThat(actual).isSameAs(exception)
    verify(imprisonmentStatusMismatchRepository, never()).save(any<ImprisonmentStatusMismatchEntity>())
    verifyNoInteractions(slackApiClient)
  }

  private fun aCalculation(imprisonmentStatus: ImprisonmentStatus) = ImprisonmentStatusCalculation(imprisonmentStatus, "TH68010", CalculatedFrom.SENTENCE)

  private fun anImprisonmentStatusEntity(nomisCode: String) = ImprisonmentStatusEntity(id = nomisCode, description = nomisCode, band = 1, rank = 1)

  private fun ImprisonmentStatusMismatchEntity.withId(id: UUID) = ImprisonmentStatusMismatchEntity(
    id = id,
    prisonNumber = prisonNumber,
    bookingId = bookingId,
    receptionDate = receptionDate,
    nomisImprisonmentStatus = nomisImprisonmentStatus,
    nomisCalculatedAt = nomisCalculatedAt,
    dpsImprisonmentStatus = dpsImprisonmentStatus,
    dpsMainOffenceCode = dpsMainOffenceCode,
    dpsCalculatedFrom = dpsCalculatedFrom,
    dpsCalculatedAt = dpsCalculatedAt,
    rasData = rasData,
    offenceSeverities = offenceSeverities,
    nomisData = nomisData,
    createdAt = createdAt,
  )
}
