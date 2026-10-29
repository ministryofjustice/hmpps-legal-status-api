package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.given
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.legalstatusapi.client.prisonersearch.PrisonerNotFoundException
import uk.gov.justice.digital.hmpps.legalstatusapi.client.prisonersearch.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.legalstatusapi.client.prisonersearch.PrisonerSearchApiException
import uk.gov.justice.digital.hmpps.legalstatusapi.client.prisonersearch.assertThat
import uk.gov.justice.digital.hmpps.legalstatusapi.client.remandandsentencing.PrisonerCharges
import uk.gov.justice.digital.hmpps.legalstatusapi.client.remandandsentencing.RemandAndSentencingApiClient
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatus
import uk.gov.justice.digital.hmpps.legalstatusapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.prisonersearch.resource.model.aValidPrisoner
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.ChargeLegacyData
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.ChargeOutcome
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonCharge
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonSentence
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.SentenceLegacyData
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.SentenceType
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.SentenceTypeDetails
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class LegacyDataServiceTest {

  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()

  private val remandAndSentencingApiClient: RemandAndSentencingApiClient = mock()

  private val sentenceTypeDetailsProvider: SentenceTypeDetailsProvider = mock()

  private val imprisonmentStatusCalculator: ImprisonmentStatusCalculator = mock()

  private val fixedTimestamp = Instant.parse("2026-04-17T09:13:22.123Z")
  private val clock = Clock.fixed(fixedTimestamp, ZoneId.of("UTC"))

  private val jsonMapper = JsonMapper.builder().build()

  private val legacyDataService = LegacyDataService(
    prisonerSearchApiClient,
    remandAndSentencingApiClient,
    sentenceTypeDetailsProvider,
    clock,
    imprisonmentStatusCalculator,
    jsonMapper,
  )

  @Test
  fun `should calculate Imprisonment Status from all of the prisoner's RaS charges`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    val receptionDate = LocalDate.now().minusMonths(3)
    given(prisonerSearchApiClient.getPrisoner(any())).willReturn(aValidPrisoner(prisonNumber = prisonNumber, receptionDate = receptionDate))

    val sentenceTypeUuid = UUID.randomUUID()
    given(remandAndSentencingApiClient.getPrisonerCharges(any(), anyOrNull(), anyOrNull())).willReturn(
      aRasPrisonerCharges(
        aRasCharge(offenceCode = "TH68010", outcome = aRasChargeOutcome(nomisCode = "1002"), sentence = aRasSentence(sentenceType = aRasSentenceType(sentenceTypeUuid))),
        aRasCharge(offenceCode = "CJ88116", legacyData = ChargeLegacyData(nomisOutcomeCode = "4565"), sentence = aRasSentence(legacyData = SentenceLegacyData(postedDate = "2026-01-01", sentenceCalcType = "ADIMP_ORA", sentenceCategory = "2020"))),
        aRasCharge(offenceCode = "SX56025-029N"),
      ),
    )
    given(sentenceTypeDetailsProvider.getSentenceTypeDetails(any())).willReturn(aRasSentenceTypeDetails(sentenceTypeUuid, nomisCjaCode = "2003", nomisSentenceCalcType = "ADIMP"))
    given(imprisonmentStatusCalculator.calculate(any())).willReturn(ImprisonmentStatusCalculation(ImprisonmentStatus.SENT03, "TH68010", CalculatedFrom.SENTENCE))

    // When
    val actual = legacyDataService.calculateImprisonmentStatus(prisonNumber)

    // Then
    assertThat(actual).isEqualTo("SENT03")
    verify(prisonerSearchApiClient).getPrisoner(prisonNumber)
    verify(remandAndSentencingApiClient).getPrisonerCharges(prisonNumber, receptionDate, LocalDate.now(clock))
    verify(sentenceTypeDetailsProvider).getSentenceTypeDetails(sentenceTypeUuid)
    verify(imprisonmentStatusCalculator).calculate(
      LegacySentencingData(
        sentences = listOf(
          LegacySentence(sentenceCalcType = "ADIMP", sentenceCategory = "2003", offence = LegacyOffence(offenceCode = "TH68010", outcomeCode = "1002")),
          LegacySentence(sentenceCalcType = "ADIMP_ORA", sentenceCategory = "2020", offence = LegacyOffence(offenceCode = "CJ88116", outcomeCode = "4565")),
        ),
        offences = listOf(
          LegacyOffence(offenceCode = "TH68010", outcomeCode = "1002"),
          LegacyOffence(offenceCode = "CJ88116", outcomeCode = "4565"),
          LegacyOffence(offenceCode = "SX56025-029N", outcomeCode = null),
        ),
      ),
    )
  }

  @Test
  fun `should return the calculation with its inputs and the NOMIS Imprisonment Status`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    val receptionDate = LocalDate.now().minusMonths(3)
    given(prisonerSearchApiClient.getPrisoner(any())).willReturn(aValidPrisoner(prisonNumber = prisonNumber, receptionDate = receptionDate, imprisonmentStatus = "LR"))
    given(remandAndSentencingApiClient.getPrisonerCharges(any(), anyOrNull(), anyOrNull())).willReturn(
      aRasPrisonerCharges(aRasCharge(offenceCode = "TH68010", outcome = aRasChargeOutcome(nomisCode = "1002"))),
    )
    val calculation = ImprisonmentStatusCalculation(ImprisonmentStatus.UNKNOWN, "TH68010", CalculatedFrom.CHARGE_OUTCOME)
    given(imprisonmentStatusCalculator.calculate(any())).willReturn(calculation)

    // When
    val actual = legacyDataService.calculate(prisonNumber)

    // Then
    assertThat(actual.prisonNumber).isEqualTo(prisonNumber)
    assertThat(actual.nomisImprisonmentStatus).isEqualTo("LR")
    assertThat(actual.receptionDate).isEqualTo(receptionDate)
    assertThat(actual.calculation).isEqualTo(calculation)
    assertThat(actual.sentencingData).isEqualTo(LegacySentencingData(offences = listOf(LegacyOffence(offenceCode = "TH68010", outcomeCode = "1002"))))
    assertThat(actual.calculatedAt).isEqualTo(fixedTimestamp)
  }

  @Test
  fun `should return the raw RaS charges response and each sentence type looked up as the RaS data`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    given(prisonerSearchApiClient.getPrisoner(any())).willReturn(aValidPrisoner(prisonNumber = prisonNumber))

    val sentenceTypeUuid = UUID.randomUUID()
    val sentenceType = aRasSentenceType(sentenceTypeUuid)
    // Includes a field that our RaS models do not have, which must not be lost
    val rawJson = """{"charges": [], "someNewRasField": 1}"""
    given(remandAndSentencingApiClient.getPrisonerCharges(any(), anyOrNull(), anyOrNull())).willReturn(
      PrisonerCharges(
        charges = listOf(
          aRasCharge(offenceCode = "TH68010", sentence = aRasSentence(sentenceType = sentenceType)),
          aRasCharge(offenceCode = "CJ88116", sentence = aRasSentence(sentenceType = sentenceType)),
        ),
        rawJson = rawJson,
      ),
    )
    given(sentenceTypeDetailsProvider.getSentenceTypeDetails(any())).willReturn(aRasSentenceTypeDetails(sentenceTypeUuid, nomisCjaCode = "2003", nomisSentenceCalcType = "ADIMP"))
    given(imprisonmentStatusCalculator.calculate(any())).willReturn(ImprisonmentStatusCalculation(ImprisonmentStatus.SENT03, "TH68010", CalculatedFrom.SENTENCE))

    // When
    val actual = legacyDataService.calculate(prisonNumber)

    // Then
    val rasData = jsonMapper.readTree(actual.rasData)
    assertThat(rasData["personCharges"]).isEqualTo(jsonMapper.readTree(rawJson))
    assertThat(rasData["sentenceTypeDetails"].propertyNames()).containsExactly(sentenceTypeUuid.toString())
    assertThat(rasData["sentenceTypeDetails"][sentenceTypeUuid.toString()]["nomisSentenceCalcType"].asString()).isEqualTo("ADIMP")
    assertThat(rasData["sentenceTypeDetails"][sentenceTypeUuid.toString()]["nomisCjaCode"].asString()).isEqualTo("2003")
    // Both sentences have the same sentence type, so it is only looked up once
    verify(sentenceTypeDetailsProvider, times(1)).getSentenceTypeDetails(sentenceTypeUuid)
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

  private fun aRasPrisonerCharges(vararg charges: PersonCharge) = PrisonerCharges(
    charges = charges.toList(),
    rawJson = """{"charges": []}""",
  )

  private fun aRasCharge(
    offenceCode: String,
    outcome: ChargeOutcome? = null,
    legacyData: ChargeLegacyData? = null,
    sentence: PersonSentence? = null,
  ) = PersonCharge(
    chargeUuid = UUID.randomUUID(),
    offenceCode = offenceCode,
    aggravatingFactors = emptyList(),
    outcome = outcome,
    legacyData = legacyData,
    sentence = sentence,
  )

  private fun aRasChargeOutcome(nomisCode: String) = ChargeOutcome(
    outcomeUuid = UUID.randomUUID(),
    outcomeName = "An outcome",
    nomisCode = nomisCode,
    outcomeType = "SENTENCING",
    displayOrder = 1,
    dispositionCode = "FINAL",
    status = ChargeOutcome.Status.ACTIVE,
  )

  private fun aRasSentence(sentenceType: SentenceType? = null, legacyData: SentenceLegacyData? = null) = PersonSentence(
    sentenceUuid = UUID.randomUUID(),
    periodLengths = emptyList(),
    sentenceServeType = "FORTHWITH",
    hasRecall = false,
    status = PersonSentence.Status.ACTIVE,
    sentenceType = sentenceType,
    legacyData = legacyData,
  )

  private fun aRasSentenceType(sentenceTypeUuid: UUID) = SentenceType(
    sentenceTypeUuid = sentenceTypeUuid,
    description = "A sentence type",
    classification = SentenceType.Classification.STANDARD,
    displayOrder = 1,
  )

  private fun aRasSentenceTypeDetails(sentenceTypeUuid: UUID, nomisCjaCode: String, nomisSentenceCalcType: String) = SentenceTypeDetails(
    sentenceTypeUuid = sentenceTypeUuid,
    description = "A sentence type",
    classification = SentenceTypeDetails.Classification.STANDARD,
    displayOrder = 1,
    isRecallable = true,
    nomisCjaCode = nomisCjaCode,
    nomisSentenceCalcType = nomisSentenceCalcType,
    status = SentenceTypeDetails.Status.ACTIVE,
  )
}
