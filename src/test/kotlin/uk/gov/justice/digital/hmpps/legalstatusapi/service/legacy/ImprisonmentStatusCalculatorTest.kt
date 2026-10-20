package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.quality.Strictness
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatus
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatus.ADIMP_ORA20
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatus.ALP
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatus.LR
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatus.SENT03
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatus.TRL
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatus.UNKNOWN
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatusEntity
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatusMappingEntity
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.repository.ImprisonmentStatusMappingRepository
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.CalculatedFrom.CHARGE_OUTCOME
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.CalculatedFrom.SENTENCE
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImprisonmentStatusCalculatorTest {

  @InjectMocks
  private lateinit var calculator: ImprisonmentStatusCalculator

  @Mock
  private lateinit var imprisonmentStatusMappingRepository: ImprisonmentStatusMappingRepository

  @Mock
  private lateinit var offenceSeverityProvider: OffenceSeverityProvider

  @Nested
  inner class FromSentences {

    @Test
    fun `should calculate status from a single mapped sentence`() {
      // Given
      givenSentenceMapping("2003", "ADIMP", SENT03)
      givenSeverities("TH68010" to 50)
      val sentencingData = LegacySentencingData(
        sentences = listOf(aSentence(sentenceCategory = "2003", sentenceCalcType = "ADIMP", offenceCode = "TH68010")),
        offences = listOf(anOffence(offenceCode = "TH68010")),
      )

      // When
      val actual = calculator.calculate(sentencingData)

      // Then
      assertThat(actual).isEqualTo(ImprisonmentStatusCalculation(SENT03, "TH68010", SENTENCE))
    }

    @Test
    fun `should choose the sentence whose status has the lowest rank`() {
      // Given
      givenSentenceMapping("2003", "ADIMP", SENT03) // rank 110
      givenSentenceMapping("1991", "ALP", ALP) // rank 10
      givenSeverities("OFF001" to 1, "OFF002" to 99)
      val start = LocalDate.of(2026, 1, 1)
      val sentencingData = LegacySentencingData(
        sentences = listOf(
          aSentence(sentenceCategory = "2003", sentenceCalcType = "ADIMP", offenceCode = "OFF001", startDate = start, endDate = start.plusYears(5)),
          aSentence(sentenceCategory = "1991", sentenceCalcType = "ALP", offenceCode = "OFF002"),
        ),
      )

      // When
      val actual = calculator.calculate(sentencingData)

      // Then
      assertThat(actual).isEqualTo(ImprisonmentStatusCalculation(ALP, "OFF002", SENTENCE))
    }

    @Test
    fun `should choose the longest sentence given statuses with the same rank`() {
      // Given
      givenSentenceMapping("2003", "ADIMP", SENT03) // rank 110
      givenSentenceMapping("2020", "ADIMP_ORA", ADIMP_ORA20) // rank 110
      givenSeverities("OFF001" to 1, "OFF002" to 99)
      val start = LocalDate.of(2026, 1, 1)
      val sentencingData = LegacySentencingData(
        sentences = listOf(
          aSentence(sentenceCategory = "2003", sentenceCalcType = "ADIMP", offenceCode = "OFF001", startDate = start, endDate = start.plusYears(1)),
          aSentence(sentenceCategory = "2020", sentenceCalcType = "ADIMP_ORA", offenceCode = "OFF002", startDate = start, endDate = start.plusYears(2)),
        ),
      )

      // When
      val actual = calculator.calculate(sentencingData)

      // Then
      assertThat(actual).isEqualTo(ImprisonmentStatusCalculation(ADIMP_ORA20, "OFF002", SENTENCE))
    }

    @Test
    fun `should choose a sentence with a length over one without, given statuses with the same rank`() {
      // Given
      givenSentenceMapping("2003", "ADIMP", SENT03)
      givenSentenceMapping("2020", "ADIMP_ORA", ADIMP_ORA20)
      givenSeverities("OFF001" to 1, "OFF002" to 99)
      val start = LocalDate.of(2026, 1, 1)
      val sentencingData = LegacySentencingData(
        sentences = listOf(
          aSentence(sentenceCategory = "2003", sentenceCalcType = "ADIMP", offenceCode = "OFF001", startDate = start, endDate = null),
          aSentence(sentenceCategory = "2020", sentenceCalcType = "ADIMP_ORA", offenceCode = "OFF002", startDate = start, endDate = start.plusDays(1)),
        ),
      )

      // When
      val actual = calculator.calculate(sentencingData)

      // Then
      assertThat(actual).isEqualTo(ImprisonmentStatusCalculation(ADIMP_ORA20, "OFF002", SENTENCE))
    }

    @Test
    fun `should choose the sentence with the most severe offence given statuses with the same rank and length`() {
      // Given
      givenSentenceMapping("2003", "ADIMP", SENT03)
      givenSentenceMapping("2020", "ADIMP_ORA", ADIMP_ORA20)
      givenSeverities("LESS01" to 80, "MORE01" to 20)
      val sentencingData = LegacySentencingData(
        sentences = listOf(
          aSentence(sentenceCategory = "2003", sentenceCalcType = "ADIMP", offenceCode = "LESS01"),
          aSentence(sentenceCategory = "2020", sentenceCalcType = "ADIMP_ORA", offenceCode = "MORE01"),
        ),
      )

      // When
      val actual = calculator.calculate(sentencingData)

      // Then
      assertThat(actual).isEqualTo(ImprisonmentStatusCalculation(ADIMP_ORA20, "MORE01", SENTENCE))
    }

    @Test
    fun `should choose the first sentence given statuses with the same rank, length and offence severity`() {
      // Given
      givenSentenceMapping("2003", "ADIMP", SENT03)
      givenSentenceMapping("2020", "ADIMP_ORA", ADIMP_ORA20)
      givenSeverities("OFF001" to 50, "OFF002" to 50)
      val sentencingData = LegacySentencingData(
        sentences = listOf(
          aSentence(sentenceCategory = "2020", sentenceCalcType = "ADIMP_ORA", offenceCode = "OFF002"),
          aSentence(sentenceCategory = "2003", sentenceCalcType = "ADIMP", offenceCode = "OFF001"),
        ),
      )

      // When
      val actual = calculator.calculate(sentencingData)

      // Then
      assertThat(actual).isEqualTo(ImprisonmentStatusCalculation(ADIMP_ORA20, "OFF002", SENTENCE))
    }

    @Test
    fun `should choose a sentence for an offence with a severity over one without`() {
      // Given
      givenSentenceMapping("2003", "ADIMP", SENT03)
      givenSeverities("TH68010" to 90)
      val sentencingData = LegacySentencingData(
        sentences = listOf(aSentence(offenceCode = "SX56025-029N"), aSentence(offenceCode = "TH68010")),
      )

      // When
      val actual = calculator.calculate(sentencingData)

      // Then
      assertThat(actual).isEqualTo(ImprisonmentStatusCalculation(SENT03, "TH68010", SENTENCE))
    }

    @Test
    fun `should still calculate status from sentences given none of their offences have a severity`() {
      // Deliberate difference from NOMIS, which would fall back to the charge outcome calculation
      // Given
      givenSentenceMapping("2003", "ADIMP", SENT03)
      givenResultCodeMapping("4565", TRL)
      givenSeverities()
      val sentencingData = LegacySentencingData(
        sentences = listOf(aSentence(offenceCode = "SX56087-088N"), aSentence(offenceCode = "SX56025-029N")),
        offences = listOf(anOffence(offenceCode = "SX56025-029N", outcomeCode = "4565")),
      )

      // When
      val actual = calculator.calculate(sentencingData)

      // Then
      assertThat(actual).isEqualTo(ImprisonmentStatusCalculation(SENT03, "SX56087-088N", SENTENCE))
    }

    @Test
    fun `should ignore sentences with no mapping`() {
      // Given
      givenSentenceMapping("2003", "ADIMP", SENT03)
      givenSeverities("OFF001" to 1, "OFF002" to 99)
      val sentencingData = LegacySentencingData(
        sentences = listOf(
          aSentence(sentenceCategory = "2003", sentenceCalcType = "UNMAPPED", offenceCode = "OFF001"),
          aSentence(sentenceCategory = "2003", sentenceCalcType = "ADIMP", offenceCode = "OFF002"),
        ),
      )

      // When
      val actual = calculator.calculate(sentencingData)

      // Then
      assertThat(actual).isEqualTo(ImprisonmentStatusCalculation(SENT03, "OFF002", SENTENCE))
    }

    @Test
    fun `should calculate from charge outcomes given no sentence has a mapping`() {
      // Given
      givenResultCodeMapping("4565", TRL)
      val sentencingData = LegacySentencingData(
        sentences = listOf(aSentence(sentenceCalcType = "UNMAPPED", offenceCode = "TH68010")),
        offences = listOf(anOffence(offenceCode = "TH68010", outcomeCode = "4565")),
      )

      // When
      val actual = calculator.calculate(sentencingData)

      // Then
      assertThat(actual).isEqualTo(ImprisonmentStatusCalculation(TRL, "TH68010", CHARGE_OUTCOME))
    }
  }

  @Nested
  inner class FromChargeOutcomes {

    @Test
    fun `should return UNKNOWN given no charges or sentences`() {
      // Given
      givenSeverities()

      // When
      val actual = calculator.calculate(LegacySentencingData())

      // Then
      assertThat(actual).isEqualTo(ImprisonmentStatusCalculation(UNKNOWN, null, CHARGE_OUTCOME))
    }

    @Test
    fun `should choose the charge whose outcome code maps to the status with the lowest rank`() {
      // Given
      givenResultCodeMapping("4565", TRL) // rank 560
      givenResultCodeMapping("1501", LR) // rank 200
      givenSeverities("MORE01" to 1, "LESS01" to 99)
      val sentencingData = LegacySentencingData(
        offences = listOf(
          anOffence(offenceCode = "MORE01", outcomeCode = "4565"),
          anOffence(offenceCode = "LESS01", outcomeCode = "1501"),
        ),
      )

      // When
      val actual = calculator.calculate(sentencingData)

      // Then
      assertThat(actual).isEqualTo(ImprisonmentStatusCalculation(LR, "LESS01", CHARGE_OUTCOME))
    }

    @Test
    fun `should choose an offence with a mapped outcome code over ones without`() {
      // Given
      givenResultCodeMapping("4565", TRL)
      givenSeverities("MORE01" to 1, "MORE02" to 1, "LESS01" to 99)
      val sentencingData = LegacySentencingData(
        offences = listOf(
          anOffence(offenceCode = "MORE01", outcomeCode = "UNMAPPED"),
          anOffence(offenceCode = "MORE02", outcomeCode = "UNMAPPED2"),
          anOffence(offenceCode = "LESS01", outcomeCode = "4565"),
        ),
      )

      // When
      val actual = calculator.calculate(sentencingData)

      // Then
      assertThat(actual).isEqualTo(ImprisonmentStatusCalculation(TRL, "LESS01", CHARGE_OUTCOME))
    }

    @Test
    fun `should return UNKNOWN with the most severe charge as main offence given no outcome codes are mapped`() {
      // Given
      givenSeverities("MORE01" to 1, "LESS01" to 99)
      val sentencingData = LegacySentencingData(
        offences = listOf(
          anOffence(offenceCode = "LESS01", outcomeCode = "UNMAPPED"),
          anOffence(offenceCode = "MORE01", outcomeCode = "UNMAPPED2"),
        ),
      )

      // When
      val actual = calculator.calculate(sentencingData)

      // Then
      assertThat(actual).isEqualTo(ImprisonmentStatusCalculation(UNKNOWN, "MORE01", CHARGE_OUTCOME))
    }

    @Test
    fun `should choose the most severe charge, then the first, given statuses with the same rank`() {
      // Given
      givenResultCodeMapping("4565", TRL)
      givenSeverities("LESS01" to 99, "MORE01" to 1, "MORE02" to 1)
      val sentencingData = LegacySentencingData(
        offences = listOf(
          anOffence(offenceCode = "LESS01", outcomeCode = "4565"),
          anOffence(offenceCode = "MORE02", outcomeCode = "4565"),
          anOffence(offenceCode = "MORE01", outcomeCode = "4565"),
        ),
      )

      // When
      val actual = calculator.calculate(sentencingData)

      // Then
      assertThat(actual).isEqualTo(ImprisonmentStatusCalculation(TRL, "MORE02", CHARGE_OUTCOME))
    }

    @Test
    fun `should choose a charge with a severity over one without, given statuses with the same rank`() {
      // Deliberate difference from NOMIS, which would return both charges
      // Given
      givenResultCodeMapping("4565", TRL)
      givenSeverities("LESS01" to 99)
      val sentencingData = LegacySentencingData(
        offences = listOf(
          anOffence(offenceCode = "SX56025-029N", outcomeCode = "4565"),
          anOffence(offenceCode = "LESS01", outcomeCode = "4565"),
        ),
      )

      // When
      val actual = calculator.calculate(sentencingData)

      // Then
      assertThat(actual).isEqualTo(ImprisonmentStatusCalculation(TRL, "LESS01", CHARGE_OUTCOME))
    }
  }

  @Test
  fun `should look up severities for the offences of all sentences and charges`() {
    // Given
    givenSeverities()
    val sentencingData = LegacySentencingData(
      sentences = listOf(aSentence(offenceCode = "TH68010")),
      offences = listOf(anOffence(offenceCode = "TH68010"), anOffence(offenceCode = "SX56025-029N")),
    )

    // When
    calculator.calculate(sentencingData)

    // Then
    verify(offenceSeverityProvider).getSeverities(listOf("TH68010", "TH68010", "SX56025-029N"))
  }

  private fun givenSentenceMapping(sentenceCategory: String, sentenceCalcType: String, status: ImprisonmentStatus) {
    given(imprisonmentStatusMappingRepository.findBySentenceCategoryAndSentenceCalcType(sentenceCategory, sentenceCalcType))
      .willReturn(aMapping(status, sentenceCategory = sentenceCategory, sentenceCalcType = sentenceCalcType))
  }

  private fun givenResultCodeMapping(resultCode: String, status: ImprisonmentStatus) {
    given(imprisonmentStatusMappingRepository.findByOffenceResultCode(resultCode))
      .willReturn(aMapping(status, offenceResultCode = resultCode))
  }

  private fun givenSeverities(vararg severities: Pair<String, Int>) {
    given(offenceSeverityProvider.getSeverities(any())).willReturn(severities.toMap())
  }

  private fun aMapping(
    status: ImprisonmentStatus,
    sentenceCategory: String? = null,
    sentenceCalcType: String? = null,
    offenceResultCode: String? = null,
  ) = ImprisonmentStatusMappingEntity(
    id = UUID.randomUUID(),
    imprisonmentStatus = ImprisonmentStatusEntity(id = status.nomisCode, description = status.name, band = 1, rank = RANKS.getValue(status)),
    sentenceCategory = sentenceCategory,
    sentenceCalcType = sentenceCalcType,
    offenceResultCode = offenceResultCode,
  )

  private companion object {
    // The real RANK_VALUEs from the IMPRISONMENT_STATUS reference data
    val RANKS = mapOf(ALP to 10, SENT03 to 110, ADIMP_ORA20 to 110, LR to 200, TRL to 560)
  }
}
