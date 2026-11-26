package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.secondValue
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.legalstatusapi.client.personlocation.PeriodOfCustodyNotFoundException
import uk.gov.justice.digital.hmpps.legalstatusapi.client.personlocation.PersonLocationApiClient
import uk.gov.justice.digital.hmpps.legalstatusapi.client.prisonersearch.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.legalstatusapi.client.prisonersearch.PrisonerSearchApiException
import uk.gov.justice.digital.hmpps.legalstatusapi.client.remandandsentencing.PrisonerCharges
import uk.gov.justice.digital.hmpps.legalstatusapi.client.remandandsentencing.RemandAndSentencingApiClient
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.entity.ImprisonmentStatusCalculatedAtEntity
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.entity.anImprisonmentStatusCalculatedAtEntity
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatus
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.repository.ImprisonmentStatusCalculatedAtRepository
import uk.gov.justice.digital.hmpps.legalstatusapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.NotCalculatedReason.CURRENT_PERIOD_OF_CUSTODY_CONCLUDED
import uk.gov.justice.digital.hmpps.personlocation.resource.model.aValidPeriodOfCustody
import uk.gov.justice.digital.hmpps.personlocation.resource.model.aValidPreviousPeriod
import uk.gov.justice.digital.hmpps.prisonersearch.resource.model.aValidPrisoner
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.ChargeLegacyData
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.ChargeOutcome
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonCharge
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonPeriodLength
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonSentence
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.SentenceLegacyData
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.SentenceType
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.SentenceTypeDetails
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.SentenceTypeDetails.Classification.STANDARD
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.aValidPersonCharge
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.aValidPersonPeriodLength
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Optional
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LegacyDataServiceTest {

  private val durationBeforeRecalculationRequired = Duration.ofSeconds(10)

  private val imprisonmentStatusCalculatedAtRepository: ImprisonmentStatusCalculatedAtRepository = mock()

  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()

  private val personLocationApiClient: PersonLocationApiClient = mock()

  private val remandAndSentencingApiClient: RemandAndSentencingApiClient = mock()

  private val sentenceTypeDetailsProvider: SentenceTypeDetailsProvider = mock()

  private val imprisonmentStatusCalculator: ImprisonmentStatusCalculator = mock()

  private val fixedTimestamp = Instant.parse("2026-04-17T09:13:22.123Z")
  private val clock = Clock.fixed(fixedTimestamp, ZoneId.of("UTC"))

  private val jsonMapper = JsonMapper.builder().build()

  private val transactionManager: PlatformTransactionManager = mock()

  private val legacyDataService = LegacyDataService(
    durationBeforeRecalculationRequired,
    imprisonmentStatusCalculatedAtRepository,
    prisonerSearchApiClient,
    personLocationApiClient,
    remandAndSentencingApiClient,
    sentenceTypeDetailsProvider,
    clock,
    imprisonmentStatusCalculator,
    jsonMapper,
    TransactionTemplate(transactionManager),
  )

  @BeforeAll
  fun beforeAll() {
    given(transactionManager.getTransaction(anyOrNull())).willReturn(SimpleTransactionStatus())
    given(imprisonmentStatusCalculatedAtRepository.save(any<ImprisonmentStatusCalculatedAtEntity>())).willAnswer {
      it.arguments[0]
    }
  }

  // By default, the prisoner has a period of custody with no previous period, so RaS is called with no fromDate
  @BeforeEach
  fun setUp() {
    Mockito.reset(
      remandAndSentencingApiClient,
      prisonerSearchApiClient,
      personLocationApiClient,
      imprisonmentStatusCalculatedAtRepository,
    )
    given(personLocationApiClient.getLatestPeriodOfCustody(any())).willReturn(aValidPeriodOfCustody(previousPeriod = null))
  }

  @Test
  fun `should calculate Imprisonment Status from all of the prisoner's RaS charges`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    given(prisonerSearchApiClient.getPrisoner(any())).willReturn(aValidPrisoner(prisonNumber = prisonNumber))
    given(personLocationApiClient.getLatestPeriodOfCustody(any())).willReturn(
      aValidPeriodOfCustody(previousPeriod = aValidPreviousPeriod(concludedAt = LocalDateTime.parse("2025-01-20T09:00:00"))),
    )

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
    verify(personLocationApiClient).getLatestPeriodOfCustody(prisonNumber)
    verify(remandAndSentencingApiClient).getPrisonerCharges(prisonNumber, LocalDate.parse("2025-01-21"), LocalDate.now(clock))
    verify(sentenceTypeDetailsProvider).getSentenceTypeDetails(sentenceTypeUuid)
    verify(imprisonmentStatusCalculator).calculate(
      LegacySentencingData(
        sentences = listOf(
          aRasSentence(classification = STANDARD, legacySentenceCalcType = "ADIMP", legacySentenceCategory = "2003", offence = Offence(offenceCode = "TH68010", outcomeCode = "1002")),
          aLegacySentence(sentenceCalcType = "ADIMP_ORA", sentenceCategory = "2020", offence = Offence(offenceCode = "CJ88116", outcomeCode = "4565")),
        ),
        offences = listOf(
          Offence(offenceCode = "TH68010", outcomeCode = "1002"),
          Offence(offenceCode = "CJ88116", outcomeCode = "4565"),
          Offence(offenceCode = "SX56025-029N", outcomeCode = null),
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
    given(personLocationApiClient.getLatestPeriodOfCustody(any())).willReturn(
      aValidPeriodOfCustody(previousPeriod = aValidPreviousPeriod(concludedAt = LocalDateTime.parse("2025-01-20T09:00:00"))),
    )
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
    assertThat(actual.previousPeriodConcludedAt).isEqualTo(LocalDateTime.parse("2025-01-20T09:00:00"))
    assertThat(actual.chargesWindowStart).isEqualTo(LocalDate.parse("2025-01-21"))
    assertThat(actual.calculation).isEqualTo(calculation)
    assertThat(actual.sentencingData).isEqualTo(LegacySentencingData(offences = listOf(Offence(offenceCode = "TH68010", outcomeCode = "1002"))))
    assertThat(actual.calculatedAt).isEqualTo(fixedTimestamp)
  }

  @Test
  fun `should derive each sentence's length from its period lengths`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    given(prisonerSearchApiClient.getPrisoner(any())).willReturn(aValidPrisoner(prisonNumber = prisonNumber))

    val sentenceTypeUuid = UUID.randomUUID()
    given(remandAndSentencingApiClient.getPrisonerCharges(any(), anyOrNull(), anyOrNull())).willReturn(
      aRasPrisonerCharges(
        aRasCharge(offenceCode = "TH68010", sentence = aRasSentence(sentenceType = aRasSentenceType(sentenceTypeUuid), periodLengths = listOf(aValidPersonPeriodLength(years = 2)))),
        aValidPersonCharge(
          offenceCode = "CJ88116",
          sentence = aRasSentence(
            legacyData = SentenceLegacyData(postedDate = "2026-01-01", sentenceCalcType = "ADIMP_ORA", sentenceCategory = "2020"),
            periodLengths = listOf(aValidPersonPeriodLength(weeks = 3), aValidPersonPeriodLength(days = 4)),
          ),
        ),
        aRasCharge(offenceCode = "SX56025-029N", sentence = aRasSentence(legacyData = SentenceLegacyData(postedDate = "2026-01-01", sentenceCalcType = "ADIMP", sentenceCategory = "2003"))),
      ),
    )
    given(sentenceTypeDetailsProvider.getSentenceTypeDetails(any())).willReturn(aRasSentenceTypeDetails(sentenceTypeUuid, nomisCjaCode = "2003", nomisSentenceCalcType = "ADIMP"))
    given(imprisonmentStatusCalculator.calculate(any())).willReturn(ImprisonmentStatusCalculation(ImprisonmentStatus.SENT03, "TH68010", CalculatedFrom.SENTENCE))

    // When
    val actual = legacyDataService.calculate(prisonNumber)

    // Then
    assertThat(actual.sentencingData.sentences.map { it.sentenceLengthDays }).containsExactly(
      731, // 2 years from 2000-01-01, which includes 29 Feb 2000
      25, // 3 weeks + 4 days, summed across both period lengths
      null, // no period lengths
    )
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
  fun `should call RaS with no fromDate given the prisoner has no previous period of custody`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    given(prisonerSearchApiClient.getPrisoner(any())).willReturn(aValidPrisoner(prisonNumber = prisonNumber, receptionDate = LocalDate.now().minusMonths(3)))
    given(personLocationApiClient.getLatestPeriodOfCustody(any())).willReturn(aValidPeriodOfCustody(previousPeriod = null))
    given(remandAndSentencingApiClient.getPrisonerCharges(any(), anyOrNull(), anyOrNull())).willReturn(aRasPrisonerCharges())
    given(imprisonmentStatusCalculator.calculate(any())).willReturn(ImprisonmentStatusCalculation(ImprisonmentStatus.UNKNOWN, null, CalculatedFrom.CHARGE_OUTCOME))

    // When
    val actual = legacyDataService.calculate(prisonNumber)

    // Then
    verify(remandAndSentencingApiClient).getPrisonerCharges(prisonNumber, null, LocalDate.now(clock))
    assertThat(actual.previousPeriodConcludedAt).isNull()
    assertThat(actual.chargesWindowStart).isNull()
  }

  @Test
  fun `should calculate Imprisonment Status given the prisoner has no period of custody at all`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    given(prisonerSearchApiClient.getPrisoner(any())).willReturn(aValidPrisoner(prisonNumber = prisonNumber))
    given(personLocationApiClient.getLatestPeriodOfCustody(any())).willThrow(
      PeriodOfCustodyNotFoundException(prisonNumber),
    )
    given(remandAndSentencingApiClient.getPrisonerCharges(any(), anyOrNull(), anyOrNull())).willReturn(aRasPrisonerCharges())
    given(imprisonmentStatusCalculator.calculate(any())).willReturn(ImprisonmentStatusCalculation(ImprisonmentStatus.UNKNOWN, null, CalculatedFrom.CHARGE_OUTCOME))

    // When
    val actual = legacyDataService.calculate(prisonNumber)

    // Then
    verify(remandAndSentencingApiClient).getPrisonerCharges(prisonNumber, null, LocalDate.now(clock))
    assertThat(actual.previousPeriodConcludedAt).isNull()
    assertThat(actual.chargesWindowStart).isNull()
  }

  // IS-181: we assume that an empty list of charges from RaS is never valid, so the status is not calculated
  @Nested
  inner class EmptyRasChargesTests {
    @BeforeEach
    fun setUp() {
      Mockito.clearInvocations(imprisonmentStatusCalculator, sentenceTypeDetailsProvider)
    }

    @Test
    fun `should return no calculation with its inputs and the NOMIS Imprisonment Status given RaS returns no charges`() {
      // Given
      val prisonNumber = randomValidPrisonNumber()
      val receptionDate = LocalDate.now().minusMonths(3)
      given(prisonerSearchApiClient.getPrisoner(any())).willReturn(aValidPrisoner(prisonNumber = prisonNumber, receptionDate = receptionDate, imprisonmentStatus = "SENT03"))
      given(personLocationApiClient.getLatestPeriodOfCustody(any())).willReturn(
        aValidPeriodOfCustody(previousPeriod = aValidPreviousPeriod(concludedAt = LocalDateTime.parse("2025-01-20T09:00:00"))),
      )
      given(remandAndSentencingApiClient.getPrisonerCharges(any(), anyOrNull(), anyOrNull())).willReturn(aRasPrisonerCharges())
      given(imprisonmentStatusCalculatedAtRepository.findById(prisonNumber)).willReturn(Optional.empty())

      // When
      val actual = legacyDataService.calculate(prisonNumber)

      // Then
      assertThat(actual.calculation).isNull()
      assertThat(actual.prisonNumber).isEqualTo(prisonNumber)
      assertThat(actual.notCalculatedReason).isEqualTo(NotCalculatedReason.NO_RAS_CHARGES)
      assertThat(actual.nomisImprisonmentStatus).isEqualTo("SENT03")
      assertThat(actual.receptionDate).isEqualTo(receptionDate)
      assertThat(actual.previousPeriodConcludedAt).isEqualTo(LocalDateTime.parse("2025-01-20T09:00:00"))
      assertThat(actual.chargesWindowStart).isEqualTo(LocalDate.parse("2025-01-21"))
      assertThat(actual.sentencingData).isEqualTo(LegacySentencingData(sentences = emptyList(), offences = emptyList()))
      assertThat(jsonMapper.readTree(actual.rasData)).isEqualTo(jsonMapper.readTree("""{"personCharges": {"charges": []}, "sentenceTypeDetails": {}}"""))
      assertThat(actual.calculatedAt).isEqualTo(fixedTimestamp)
      verifyNoInteractions(imprisonmentStatusCalculator, sentenceTypeDetailsProvider)
    }

    @Test
    fun `should return NOT_CALCULATED from calculateImprisonmentStatus given RaS returns no charges`() {
      // Given
      val prisonNumber = randomValidPrisonNumber()
      given(prisonerSearchApiClient.getPrisoner(any())).willReturn(aValidPrisoner(prisonNumber = prisonNumber))
      given(remandAndSentencingApiClient.getPrisonerCharges(any(), anyOrNull(), anyOrNull())).willReturn(aRasPrisonerCharges())
      given(imprisonmentStatusCalculatedAtRepository.findById(prisonNumber)).willReturn(Optional.empty())

      // When
      val actual = legacyDataService.calculateImprisonmentStatus(prisonNumber)

      // Then
      assertThat(actual).isEqualTo(NOT_CALCULATED)
    }

    @Test
    fun `should release the lock record given the prisoner has not had an imprisonment status calculated before and RaS returns no charges`() {
      // Given
      val prisonNumber = randomValidPrisonNumber()
      given(prisonerSearchApiClient.getPrisoner(any())).willReturn(aValidPrisoner(prisonNumber = prisonNumber))
      given(remandAndSentencingApiClient.getPrisonerCharges(any(), anyOrNull(), anyOrNull())).willReturn(aRasPrisonerCharges())
      given(imprisonmentStatusCalculatedAtRepository.findById(prisonNumber)).willReturn(Optional.empty())

      // When
      legacyDataService.calculate(prisonNumber)

      // Then
      val saveArgumentCaptor = ArgumentCaptor.forClass(ImprisonmentStatusCalculatedAtEntity::class.java)
      verify(imprisonmentStatusCalculatedAtRepository).save(saveArgumentCaptor.capture())
      assertThat(saveArgumentCaptor.value.prisonNumber).isEqualTo(prisonNumber)
      assertThat(saveArgumentCaptor.value.calculatedAt).isEqualTo(Instant.now(clock))
      verify(imprisonmentStatusCalculatedAtRepository).deleteById(prisonNumber)
    }

    @Test
    fun `should release the lock record given the prisoner has had an imprisonment status calculated before and RaS returns no charges`() {
      // Given
      val prisonNumber = randomValidPrisonNumber()
      val now = Instant.now(clock)
      given(prisonerSearchApiClient.getPrisoner(any())).willReturn(aValidPrisoner(prisonNumber = prisonNumber))
      given(remandAndSentencingApiClient.getPrisonerCharges(any(), anyOrNull(), anyOrNull())).willReturn(aRasPrisonerCharges())

      val previousCalculatedAt = now.minus(1, ChronoUnit.DAYS)
      given(imprisonmentStatusCalculatedAtRepository.findById(prisonNumber))
        .willReturn(Optional.of(anImprisonmentStatusCalculatedAtEntity(prisonNumber = prisonNumber, calculatedAt = previousCalculatedAt)))
      given(imprisonmentStatusCalculatedAtRepository.getReferenceById(prisonNumber))
        .willReturn(anImprisonmentStatusCalculatedAtEntity(prisonNumber = prisonNumber, calculatedAt = now))

      // When
      legacyDataService.calculate(prisonNumber)

      // Then
      val saveArgumentCaptor = ArgumentCaptor.forClass(ImprisonmentStatusCalculatedAtEntity::class.java)
      verify(imprisonmentStatusCalculatedAtRepository, times(2)).save(saveArgumentCaptor.capture())
      // The first save takes the lock, the second save sets calculatedAt back to the previous value
      assertThat(saveArgumentCaptor.firstValue.calculatedAt).isEqualTo(now)
      assertThat(saveArgumentCaptor.secondValue.calculatedAt).isEqualTo(previousCalculatedAt)
      verify(imprisonmentStatusCalculatedAtRepository, never()).deleteById(prisonNumber)
    }
  }

  // IS-210: A prisoner whose current period of custody is concluded is released and we should not recalculate the status in this case
  @Nested
  inner class CurrentPeriodOfCustodyConcludedTests {
    @BeforeEach
    fun setUp() {
      Mockito.clearInvocations(imprisonmentStatusCalculator, sentenceTypeDetailsProvider)
    }

    @Test
    fun `should return no calculation with its inputs and the NOMIS Imprisonment Status given the prisoners current period of custody is concluded`() {
      // Given
      val prisonNumber = randomValidPrisonNumber()
      val receptionDate = LocalDate.now().minusMonths(3)
      given(prisonerSearchApiClient.getPrisoner(any())).willReturn(aValidPrisoner(prisonNumber = prisonNumber, receptionDate = receptionDate, imprisonmentStatus = "SENT03"))
      given(personLocationApiClient.getLatestPeriodOfCustody(any())).willReturn(
        aValidPeriodOfCustody(concludedAt = LocalDateTime.parse("2025-01-20T09:00:00")),
      )

      // When
      val actual = legacyDataService.calculate(prisonNumber)

      // Then
      assertThat(actual.calculation).isNull()
      assertThat(actual.prisonNumber).isEqualTo(prisonNumber)
      assertThat(actual.notCalculatedReason).isEqualTo(CURRENT_PERIOD_OF_CUSTODY_CONCLUDED)
      assertThat(actual.nomisImprisonmentStatus).isEqualTo("SENT03")
      assertThat(actual.receptionDate).isEqualTo(receptionDate)
      assertThat(actual.chargesWindowStart).isNull()
      assertThat(actual.sentencingData).isEqualTo(LegacySentencingData(sentences = emptyList(), offences = emptyList()))
      assertThat(jsonMapper.readTree(actual.rasData)).isEqualTo(jsonMapper.readTree("""{"personCharges": {"charges": []}, "sentenceTypeDetails": {}}"""))
      assertThat(actual.calculatedAt).isEqualTo(fixedTimestamp)
      verifyNoInteractions(imprisonmentStatusCalculator, sentenceTypeDetailsProvider)
    }

    @Test
    fun `should return NOT_CALCULATED from calculateImprisonmentStatus given given the prisoners current period of custody is concluded`() {
      // Given
      val prisonNumber = randomValidPrisonNumber()
      given(prisonerSearchApiClient.getPrisoner(any())).willReturn(aValidPrisoner(prisonNumber = prisonNumber))
      given(personLocationApiClient.getLatestPeriodOfCustody(any())).willReturn(
        aValidPeriodOfCustody(concludedAt = LocalDateTime.parse("2025-01-20T09:00:00")),
      )

      // When
      val actual = legacyDataService.calculateImprisonmentStatus(prisonNumber)

      // Then
      assertThat(actual).isEqualTo(NOT_CALCULATED)
    }

    @Test
    fun `should release the lock record given the prisoner has not had an imprisonment status calculated before and the prisoners current period of custody is concluded`() {
      // Given
      val prisonNumber = randomValidPrisonNumber()
      given(prisonerSearchApiClient.getPrisoner(any())).willReturn(aValidPrisoner(prisonNumber = prisonNumber))
      given(personLocationApiClient.getLatestPeriodOfCustody(any())).willReturn(
        aValidPeriodOfCustody(concludedAt = LocalDateTime.parse("2025-01-20T09:00:00")),
      )
      given(imprisonmentStatusCalculatedAtRepository.findById(prisonNumber)).willReturn(Optional.empty())

      // When
      legacyDataService.calculate(prisonNumber)

      // Then
      val saveArgumentCaptor = ArgumentCaptor.forClass(ImprisonmentStatusCalculatedAtEntity::class.java)
      verify(imprisonmentStatusCalculatedAtRepository).save(saveArgumentCaptor.capture())
      assertThat(saveArgumentCaptor.value.prisonNumber).isEqualTo(prisonNumber)
      assertThat(saveArgumentCaptor.value.calculatedAt).isEqualTo(Instant.now(clock))
      verify(imprisonmentStatusCalculatedAtRepository).deleteById(prisonNumber)
    }

    @Test
    fun `should release the lock record given the prisoner has had an imprisonment status calculated before and the prisoners current period of custody is concluded`() {
      // Given
      val prisonNumber = randomValidPrisonNumber()
      val now = Instant.now(clock)
      given(prisonerSearchApiClient.getPrisoner(any())).willReturn(aValidPrisoner(prisonNumber = prisonNumber))
      given(personLocationApiClient.getLatestPeriodOfCustody(any())).willReturn(
        aValidPeriodOfCustody(concludedAt = LocalDateTime.parse("2025-01-20T09:00:00")),
      )

      val previousCalculatedAt = now.minus(1, ChronoUnit.DAYS)
      given(imprisonmentStatusCalculatedAtRepository.findById(prisonNumber))
        .willReturn(Optional.of(anImprisonmentStatusCalculatedAtEntity(prisonNumber = prisonNumber, calculatedAt = previousCalculatedAt)))
      given(imprisonmentStatusCalculatedAtRepository.getReferenceById(prisonNumber))
        .willReturn(anImprisonmentStatusCalculatedAtEntity(prisonNumber = prisonNumber, calculatedAt = now))

      // When
      legacyDataService.calculate(prisonNumber)

      // Then
      val saveArgumentCaptor = ArgumentCaptor.forClass(ImprisonmentStatusCalculatedAtEntity::class.java)
      verify(imprisonmentStatusCalculatedAtRepository, times(2)).save(saveArgumentCaptor.capture())
      // The first save takes the lock, the second save sets calculatedAt back to the previous value
      assertThat(saveArgumentCaptor.firstValue.calculatedAt).isEqualTo(now)
      assertThat(saveArgumentCaptor.secondValue.calculatedAt).isEqualTo(previousCalculatedAt)
      verify(imprisonmentStatusCalculatedAtRepository, never()).deleteById(prisonNumber)
    }
  }

  @Nested
  inner class SadPathTests {
    @Test
    fun `should not calculate Imprisonment Status given the prisoner has not had an imprisonment status calculated before and an API throws an exception`() {
      // Given
      val prisonNumber = randomValidPrisonNumber()

      // It doesn't matter which API throws. The behaviour that we are asserting will be the same
      given(prisonerSearchApiClient.getPrisoner(any())).willThrow(
        PrisonerSearchApiException("Error retrieving prisoner by prisonNumber $prisonNumber", RuntimeException("Some nested error")),
      )

      // Prisoner has never had an Imprisonment Status calculated before
      given(imprisonmentStatusCalculatedAtRepository.findById(prisonNumber)).willReturn(Optional.empty())

      // When
      runCatching {
        legacyDataService.calculateImprisonmentStatus(prisonNumber)
      }

      // Then
      verify(imprisonmentStatusCalculatedAtRepository).findById(prisonNumber)

      val saveArgumentCaptor = ArgumentCaptor.forClass(ImprisonmentStatusCalculatedAtEntity::class.java)
      verify(imprisonmentStatusCalculatedAtRepository).save(saveArgumentCaptor.capture())
      val saveArgument = saveArgumentCaptor.value
      assertThat(saveArgument.prisonNumber).isEqualTo(prisonNumber)
      assertThat(saveArgument.calculatedAt).isEqualTo(Instant.now(clock))

      verify(imprisonmentStatusCalculatedAtRepository).deleteById(prisonNumber)
    }

    @Test
    fun `should not calculate Imprisonment Status given the prisoner has had an imprisonment status calculated before and an API throws an exception`() {
      // Given
      val prisonNumber = randomValidPrisonNumber()
      val now = Instant.now(clock)

      // It doesn't matter which API throws. The behaviour that we are asserting will be the same
      given(prisonerSearchApiClient.getPrisoner(any())).willThrow(
        PrisonerSearchApiException("Error retrieving prisoner by prisonNumber $prisonNumber", RuntimeException("Some nested error")),
      )

      // Prisoner has had an Imprisonment Status calculated before. It was calculated yesterday
      val previousCalculatedAt = now.minus(1, ChronoUnit.DAYS)
      val previousImprisonmentStatusCalculatedAtEntity = anImprisonmentStatusCalculatedAtEntity(
        prisonNumber = prisonNumber,
        calculatedAt = previousCalculatedAt,
      )
      given(imprisonmentStatusCalculatedAtRepository.findById(prisonNumber))
        .willReturn(Optional.of(previousImprisonmentStatusCalculatedAtEntity))

      val updatedImprisonmentStatusCalculatedAtEntity = anImprisonmentStatusCalculatedAtEntity(
        prisonNumber = prisonNumber,
        calculatedAt = now,
      )
      given(imprisonmentStatusCalculatedAtRepository.getReferenceById(prisonNumber))
        .willReturn(updatedImprisonmentStatusCalculatedAtEntity)

      // When
      runCatching {
        legacyDataService.calculateImprisonmentStatus(prisonNumber)
      }

      // Then
      verify(imprisonmentStatusCalculatedAtRepository).findById(prisonNumber)
      verify(imprisonmentStatusCalculatedAtRepository).getReferenceById(prisonNumber)

      val saveArgumentCaptor = ArgumentCaptor.forClass(ImprisonmentStatusCalculatedAtEntity::class.java)
      verify(imprisonmentStatusCalculatedAtRepository, times(2)).save(saveArgumentCaptor.capture())
      // assert that the first save was to set the calculatedAt time to now
      val firstSaveArgument = saveArgumentCaptor.firstValue
      assertThat(firstSaveArgument.prisonNumber).isEqualTo(prisonNumber)
      assertThat(firstSaveArgument.calculatedAt).isEqualTo(now)
      // assert that the second save was to set the calculatedAt time back to the previous value
      val secondSaveArgument = saveArgumentCaptor.secondValue
      assertThat(secondSaveArgument.prisonNumber).isEqualTo(prisonNumber)
      assertThat(secondSaveArgument.calculatedAt).isEqualTo(previousCalculatedAt)

      verify(imprisonmentStatusCalculatedAtRepository, never()).deleteById(prisonNumber)
    }

    @Test
    fun `should not calculate Imprisonment Status given the prisoner has had an imprisonment status calculated within the threshold`() {
      // Given
      val prisonNumber = randomValidPrisonNumber()
      val now = Instant.now(clock)

      // Prisoner has had an Imprisonment Status calculated before. It was calculated recently within the threshold
      val previousCalculatedAt = now.minus(1, ChronoUnit.SECONDS)
      val previousImprisonmentStatusCalculatedAtEntity = anImprisonmentStatusCalculatedAtEntity(
        prisonNumber = prisonNumber,
        calculatedAt = previousCalculatedAt,
      )
      given(imprisonmentStatusCalculatedAtRepository.findById(prisonNumber))
        .willReturn(Optional.of(previousImprisonmentStatusCalculatedAtEntity))

      // When
      val actual = catchThrowableOfType(RecentImprisonmentStatusExistsException::class.java, {
        legacyDataService.calculateImprisonmentStatus(prisonNumber)
      })

      // Then
      assertThat(actual.prisonNumber).isEqualTo(prisonNumber)
      assertThat(actual.lastCalculatedAt).isEqualTo(previousCalculatedAt)

      verify(imprisonmentStatusCalculatedAtRepository).findById(prisonNumber)
      verifyNoMoreInteractions(imprisonmentStatusCalculatedAtRepository)
      verifyNoInteractions(prisonerSearchApiClient, personLocationApiClient, remandAndSentencingApiClient)
    }
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
    appearanceDate = LocalDate.now(),
    appearanceCreatedAt = LocalDateTime.now(),
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

  private fun aRasSentence(
    sentenceType: SentenceType? = null,
    legacyData: SentenceLegacyData? = null,
    periodLengths: List<PersonPeriodLength> = emptyList(),
  ) = PersonSentence(
    sentenceUuid = UUID.randomUUID(),
    periodLengths = periodLengths,
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
    classification = STANDARD,
    displayOrder = 1,
    isRecallable = true,
    nomisCjaCode = nomisCjaCode,
    nomisSentenceCalcType = nomisSentenceCalcType,
    status = SentenceTypeDetails.Status.ACTIVE,
  )
}
