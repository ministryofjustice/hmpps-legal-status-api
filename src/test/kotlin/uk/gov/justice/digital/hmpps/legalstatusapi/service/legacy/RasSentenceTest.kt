package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonSentence
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.SentenceTypeDetails

class RasSentenceTest {
  @Nested
  inner class LegacySentenceCalcTypeForImprisonmentStatusCalculation {

    @Test
    fun `should return legacySentenceCalcType given RasSentence with no recall`() {
      // Given
      val legacySentenceCalcType = "ADIMP"

      val sentence = aRasSentence(
        legacySentenceCalcType = legacySentenceCalcType,
        latestRecallType = null,
      )

      // When
      val actual = sentence.legacySentenceCalcTypeForImprisonmentStatusCalculation

      // Then
      assertThat(actual).isEqualTo(legacySentenceCalcType)
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "FTR_14, 14FTR_ORA",
        "FTR_56, FTR_56ORA",
        "FTR_HDC_14, 14FTRHDC_ORA",
        "FTR_HDC_28, FTR_HDC",
      ],
    )
    fun `should return legacySentenceCalcType given a recallType where the original sentence calc type is not relevant`(recallType: PersonSentence.LatestRecallType, expected: String) {
      // Given
      val sentence = aRasSentence(
        latestRecallType = recallType,
      )

      // When
      val actual = sentence.legacySentenceCalcTypeForImprisonmentStatusCalculation

      // Then
      assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "YOI_ORA, CUR_ORA",
        "YOI, CUR",
        "TISCS, CUR",
        "STS21, CUR",
        "STS18, CUR",
        "SOPC21, CUR",
        "SOPC18, CUR",
        "SEC94, CUR",
        "SEC93_03, CUR",
        "SEC93, CUR",
        "SEC91_03_ORA, CUR_ORA",
        "SEC91_03, CUR",
        "SEC91, CUR",
        "SEC275, CUR",
        "SEC272, CUR",
        "SEC250_ORA, CUR_ORA",
        "SEC250, CUR",
        "SEC236A, CUR",
        "SDOPCU18, CUR",
        "PPEXT_SENT, CUR",
        "NP, CUR",
        "MLP, CUR",
        "LR_YOI_ORA, CUR_ORA",
        "LR_SOPC21, CUR",
        "LR_SOPC18, CUR",
        "LR_SEC91_ORA, CUR_ORA",
        "LRSEC250_ORA, CUR_ORA",
        "LR_SEC236A, CUR",
        "LR_ORA, CUR_ORA",
        "LR_MLP, CUR",
        "LR_LIFE, CUR",
        "LR_LASPO_DR, CUR",
        "LR_LASPO_AR, CUR",
        "LR_IPP, CUR",
        "LR_ES, CUR",
        "LR_EPP, CUR",
        "LR_EDSU18, CUR",
        "LR_EDS21, CUR",
        "LR_EDS18, CUR",
        "LR_DPP, CUR",
        "LR_DLP, CUR",
        "LR_ALP_LASPO, CUR",
        "LR_ALP_CDE21, CUR",
        "LR_ALP_CDE18, CUR",
        "LR_ALP, CUR",
        "LR, CUR",
        "LASPO_DR, CUR",
        "LASPO_AR, CUR",
        "IPP, CUR",
        "HMPL, CUR",
        "HDR_ORA, CUR_ORA",
        "HDR, CUR",
        "FTRSCH18_ORA, CUR_ORA",
        "FTRSCH18, CUR",
        "FTRSCH15_ORA, CUR_ORA",
        "FTR_SCH15, CUR",
        "FTR_ORA, CUR_ORA",
        "FTR_HDC_ORA, CUR_ORA",
        "FTR_HDC, CUR",
        "FTR_56ORA, CUR_ORA",
        "FTR_56, CUR",
        "FTR, CUR",
        "EXT, CUR",
        "EPP, CUR",
        "EDSU18, CUR",
        "EDS21, CUR",
        "EDS18, CUR",
        "DTO_ORA, CUR_ORA",
        "DTO, CUR",
        "DPP, CUR",
        "DLP, CUR",
        "DFL, CUR",
        "HDR_ORA, CUR_ORA",
        "CUR, CUR",
        "CR, CUR",
        "CIVILLT, CUR",
        "CIVIL, CUR",
        "BOTUS, CUR",
        "AR, CUR",
        "ALP_LASPO, CUR",
        "ALP_CODE21, CUR",
        "ALP_CODE18, CUR",
        "ALP, CUR",
        "A/FINE, CUR",
        "ADIMP_ORA, CUR_ORA",
        "ADIMP, CUR",
        "79, CUR",
        "78, CUR",
        "37, CUR",
        "33, CUR",
        "30, CUR",
        "14FTR_ORA, CUR_ORA",
        "14FTRHDC_ORA, CUR_ORA",
      ],
    )
    fun `should return legacySentenceCalcType CUR for non-ORA sentence types given a recallType CUR_HDC`(sentenceCalcType: String, expected: String) {
      // Given
      val sentence = aRasSentence(
        legacySentenceCalcType = sentenceCalcType,
        latestRecallType = PersonSentence.LatestRecallType.CUR_HDC,
      )

      // When
      val actual = sentence.legacySentenceCalcTypeForImprisonmentStatusCalculation

      // Then
      assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "YOI_ORA, HDR_ORA",
        "YOI, HDR",
        "TISCS, HDR",
        "STS21, HDR",
        "STS18, HDR",
        "SOPC21, HDR",
        "SOPC18, HDR",
        "SEC94, HDR",
        "SEC93_03, HDR",
        "SEC93, HDR",
        "SEC91_03_ORA, HDR_ORA",
        "SEC91_03, HDR",
        "SEC91, HDR",
        "SEC275, HDR",
        "SEC272, HDR",
        "SEC250_ORA, HDR_ORA",
        "SEC250, HDR",
        "SEC236A, HDR",
        "SDOPCU18, HDR",
        "PPEXT_SENT, HDR",
        "NP, HDR",
        "MLP, HDR",
        "LR_YOI_ORA, HDR_ORA",
        "LR_SOPC21, HDR",
        "LR_SOPC18, HDR",
        "LR_SEC91_ORA, HDR_ORA",
        "LRSEC250_ORA, HDR_ORA",
        "LR_SEC236A, HDR",
        "LR_ORA, HDR_ORA",
        "LR_MLP, HDR",
        "LR_LIFE, HDR",
        "LR_LASPO_DR, HDR",
        "LR_LASPO_AR, HDR",
        "LR_IPP, HDR",
        "LR_ES, HDR",
        "LR_EPP, HDR",
        "LR_EDSU18, HDR",
        "LR_EDS21, HDR",
        "LR_EDS18, HDR",
        "LR_DPP, HDR",
        "LR_DLP, HDR",
        "LR_ALP_LASPO, HDR",
        "LR_ALP_CDE21, HDR",
        "LR_ALP_CDE18, HDR",
        "LR_ALP, HDR",
        "LR, HDR",
        "LASPO_DR, HDR",
        "LASPO_AR, HDR",
        "IPP, HDR",
        "HMPL, HDR",
        "HDR_ORA, HDR_ORA",
        "HDR, HDR",
        "FTRSCH18_ORA, HDR_ORA",
        "FTRSCH18, HDR",
        "FTRSCH15_ORA, HDR_ORA",
        "FTR_SCH15, HDR",
        "FTR_ORA, HDR_ORA",
        "FTR_HDC_ORA, HDR_ORA",
        "FTR_HDC, HDR",
        "FTR_56ORA, HDR_ORA",
        "FTR_56, HDR",
        "FTR, HDR",
        "EXT, HDR",
        "EPP, HDR",
        "EDSU18, HDR",
        "EDS21, HDR",
        "EDS18, HDR",
        "DTO_ORA, HDR_ORA",
        "DTO, HDR",
        "DPP, HDR",
        "DLP, HDR",
        "DFL, HDR",
        "HDR_ORA, HDR_ORA",
        "CUR, HDR",
        "CR, HDR",
        "CIVILLT, HDR",
        "CIVIL, HDR",
        "BOTUS, HDR",
        "AR, HDR",
        "ALP_LASPO, HDR",
        "ALP_CODE21, HDR",
        "ALP_CODE18, HDR",
        "ALP, HDR",
        "A/FINE, HDR",
        "ADIMP_ORA, HDR_ORA",
        "ADIMP, HDR",
        "79, HDR",
        "78, HDR",
        "37, HDR",
        "33, HDR",
        "30, HDR",
        "14FTR_ORA, HDR_ORA",
        "14FTRHDC_ORA, HDR_ORA",
      ],
    )
    fun `should return legacySentenceCalcType given a recallType IN_HDC`(sentenceCalcType: String, expected: String) {
      // Given
      val sentence = aRasSentence(
        legacySentenceCalcType = sentenceCalcType,
        latestRecallType = PersonSentence.LatestRecallType.IN_HDC,
      )

      // When
      val actual = sentence.legacySentenceCalcTypeForImprisonmentStatusCalculation

      // Then
      assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "YOI_ORA, FTR_ORA",
        "YOI, FTR",
        "TISCS, FTR",
        "STS21, FTR",
        "STS18, FTR",
        "SOPC21, FTR",
        "SOPC18, FTR",
        "SEC94, FTR",
        "SEC93_03, FTR",
        "SEC93, FTR",
        "SEC91_03_ORA, FTR_ORA",
        "SEC91_03, FTR",
        "SEC91, FTR",
        "SEC275, FTR",
        "SEC272, FTR",
        "SEC250_ORA, FTR_ORA",
        "SEC250, FTR",
        "SEC236A, FTR",
        "SDOPCU18, FTR",
        "PPEXT_SENT, FTR",
        "NP, FTR",
        "MLP, FTR",
        "LR_YOI_ORA, FTR_ORA",
        "LR_SOPC21, FTR",
        "LR_SOPC18, FTR",
        "LR_SEC91_ORA, FTR_ORA",
        "LRSEC250_ORA, FTR_ORA",
        "LR_SEC236A, FTR",
        "LR_ORA, FTR_ORA",
        "LR_MLP, FTR",
        "LR_LIFE, FTR",
        "LR_LASPO_DR, FTR",
        "LR_LASPO_AR, FTR",
        "LR_IPP, FTR",
        "LR_ES, FTR",
        "LR_EPP, FTR",
        "LR_EDSU18, FTR",
        "LR_EDS21, FTR",
        "LR_EDS18, FTR",
        "LR_DPP, FTR",
        "LR_DLP, FTR",
        "LR_ALP_LASPO, FTR",
        "LR_ALP_CDE21, FTR",
        "LR_ALP_CDE18, FTR",
        "LR_ALP, FTR",
        "LR, FTR",
        "LASPO_DR, FTR",
        "LASPO_AR, FTR",
        "IPP, FTR",
        "HMPL, FTR",
        "HDR_ORA, FTR_ORA",
        "HDR, FTR",
        "FTRSCH18_ORA, FTR_ORA",
        "FTRSCH18, FTR",
        "FTRSCH15_ORA, FTR_ORA",
        "FTR_SCH15, FTR",
        "FTR_ORA, FTR_ORA",
        "FTR_HDC_ORA, FTR_ORA",
        "FTR_HDC, FTR",
        "FTR_56ORA, FTR_ORA",
        "FTR_56, FTR",
        "FTR, FTR",
        "EXT, FTR",
        "EPP, FTR",
        "EDSU18, FTR",
        "EDS21, FTR",
        "EDS18, FTR",
        "DTO_ORA, FTR_ORA",
        "DTO, FTR",
        "DPP, FTR",
        "DLP, FTR",
        "DFL, FTR",
        "HDR_ORA, FTR_ORA",
        "CUR, FTR",
        "CR, FTR",
        "CIVILLT, FTR",
        "CIVIL, FTR",
        "BOTUS, FTR",
        "AR, FTR",
        "ALP_LASPO, FTR",
        "ALP_CODE21, FTR",
        "ALP_CODE18, FTR",
        "ALP, FTR",
        "A/FINE, FTR",
        "ADIMP_ORA, FTR_ORA",
        "ADIMP, FTR",
        "79, FTR",
        "78, FTR",
        "37, FTR",
        "33, FTR",
        "30, FTR",
        "14FTR_ORA, FTR_ORA",
        "14FTRHDC_ORA, FTR_ORA",
      ],
    )
    fun `should return legacySentenceCalcType given a recallType FTR_28`(sentenceCalcType: String, expected: String) {
      // Given
      val sentence = aRasSentence(
        legacySentenceCalcType = sentenceCalcType,
        latestRecallType = PersonSentence.LatestRecallType.FTR_28,
      )

      // When
      val actual = sentence.legacySentenceCalcTypeForImprisonmentStatusCalculation

      // Then
      assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "BOTUS",
        "CIVIL",
        "DTO",
        "FINE",
        "LEGACY",
        "NON_CUSTODIAL",
        "LEGACY_RECALL",
        "UNKNOWN",
      ],
    )
    fun `should return legacySentenceCalcType LR given a sentence with recallType LR and classification`(classification: SentenceTypeDetails.Classification) {
      // Given
      val sentence = aRasSentence(
        classification = classification,
        latestRecallType = PersonSentence.LatestRecallType.LR,
      )

      val expected = "LR"

      // When
      val actual = sentence.legacySentenceCalcTypeForImprisonmentStatusCalculation

      // Then
      assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "YOI_ORA, LR_YOI_ORA",
        "YOI, LR",
        "TISCS, LR",
        "STS21, LR",
        "STS18, LR",
        "SOPC21, LR",
        "SOPC18, LR",
        "SEC94, LR",
        "SEC93_03, LR",
        "SEC93, LR",
        "SEC91_03_ORA, LR",
        "SEC91_03, LR",
        "SEC91, LR",
        "SEC275, LR",
        "SEC272, LR",
        "SEC250_ORA, LRSEC250_ORA",
        "SEC250, LRSEC250_ORA",
        "SEC236A, LR",
        "SDOPCU18, LR",
        "PPEXT_SENT, LR",
        "NP, LR",
        "MLP, LR",
        "LR_YOI_ORA, LR_YOI_ORA",
        "LR_SOPC21, LR",
        "LR_SOPC18, LR",
        "LR_SEC91_ORA, LR",
        "LRSEC250_ORA, LRSEC250_ORA",
        "LR_SEC236A, LR",
        "LR_ORA, LR",
        "LR_MLP, LR",
        "LR_LIFE, LR",
        "LR_LASPO_DR, LR",
        "LR_LASPO_AR, LR",
        "LR_IPP, LR",
        "LR_ES, LR",
        "LR_EPP, LR",
        "LR_EDSU18, LR",
        "LR_EDS21, LR",
        "LR_EDS18, LR",
        "LR_DPP, LR",
        "LR_DLP, LR",
        "LR_ALP_LASPO, LR",
        "LR_ALP_CDE21, LR",
        "LR_ALP_CDE18, LR",
        "LR_ALP, LR",
        "LR, LR",
        "LASPO_DR, LR",
        "LASPO_AR, LR",
        "IPP, LR",
        "HMPL, LR",
        "HDR_ORA, LR",
        "HDR, LR",
        "FTRSCH18_ORA, LR",
        "FTRSCH18, LR",
        "FTRSCH15_ORA, LR",
        "FTR_SCH15, LR",
        "FTR_ORA, LR",
        "FTR_HDC_ORA, LR",
        "FTR_HDC, LR",
        "FTR_56ORA, LR",
        "FTR_56, LR",
        "FTR, LR",
        "EXT, LR",
        "EPP, LR",
        "EDSU18, LR",
        "EDS21, LR",
        "EDS18, LR",
        "DTO_ORA, LR",
        "DTO, LR",
        "DPP, LR",
        "DLP, LR",
        "DFL, LR",
        "HDR_ORA, LR",
        "CUR, LR",
        "CR, LR",
        "CIVILLT, LR",
        "CIVIL, LR",
        "BOTUS, LR",
        "AR, LR",
        "ALP_LASPO, LR",
        "ALP_CODE21, LR",
        "ALP_CODE18, LR",
        "ALP, LR",
        "A/FINE, LR",
        "ADIMP_ORA, LR_ORA",
        "ADIMP, LR",
        "79, LR",
        "78, LR",
        "37, LR",
        "33, LR",
        "30, LR",
        "14FTR_ORA, LR",
        "14FTRHDC_ORA, LR",
      ],
    )
    fun `should return legacySentenceCalcType given a sentence with recallType LR and Standard classification`(sentenceCalcType: String, expected: String) {
      // Given
      val sentence = aRasSentence(
        classification = SentenceTypeDetails.Classification.STANDARD,
        latestRecallType = PersonSentence.LatestRecallType.LR,
        legacySentenceCalcType = sentenceCalcType,
      )

      // When
      val actual = sentence.legacySentenceCalcTypeForImprisonmentStatusCalculation

      // Then
      assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "YOI_ORA, LR_EDS21",
        "YOI, LR_EDS21",
        "TISCS, LR_EDS21",
        "STS21, LR",
        "STS18, LR",
        "SOPC21, LR_EDS21",
        "SOPC18, LR_EDS21",
        "SEC94, LR_EDS21",
        "SEC93_03, LR_EDS21",
        "SEC93, LR_EDS21",
        "SEC91_03_ORA, LR_EDS21",
        "SEC91_03, LR_EDS21",
        "SEC91, LR_EDS21",
        "SEC275, LR_EDS21",
        "SEC272, LR_EDS21",
        "SEC250_ORA, LR_EDS21",
        "SEC250, LR_EDS21",
        "SEC236A, LR_EDS21",
        "SDOPCU18, LR_EDS21",
        "PPEXT_SENT, LR_EDS21",
        "NP, LR_EDS21",
        "MLP, LR_EDS21",
        "LR_YOI_ORA, LR_EDS21",
        "LR_SOPC21, LR_EDS21",
        "LR_SOPC18, LR_EDS21",
        "LR_SEC91_ORA, LR_EDS21",
        "LRSEC250_ORA, LR_EDS21",
        "LR_SEC236A, LR_EDS21",
        "LR_ORA, LR_EDS21",
        "LR_MLP, LR_EDS21",
        "LR_LIFE, LR_EDS21",
        "LR_LASPO_DR, LR_LASPO_DR",
        "LR_LASPO_AR, LR_LASPO_AR",
        "LR_IPP, LR_EDS21",
        "LR_ES, LR_EDS21",
        "LR_EPP, LR_EPP",
        "LR_EDSU18, LR_EDSU18",
        "LR_EDS21, LR_EDS21",
        "LR_EDS18, LR_EDS18",
        "LR_DPP, LR_EDS21",
        "LR_DLP, LR_EDS21",
        "LR_ALP_LASPO, LR_EDS21",
        "LR_ALP_CDE21, LR_EDS21",
        "LR_ALP_CDE18, LR_EDS21",
        "LR_ALP, LR_EDS21",
        "LR, LR_EDS21",
        "LASPO_DR, LR_LASPO_DR",
        "LASPO_AR, LR_LASPO_AR",
        "IPP, LR_EDS21",
        "HMPL, LR_EDS21",
        "HDR_ORA, LR_EDS21",
        "HDR, LR_EDS21",
        "FTRSCH18_ORA, LR_EDS21",
        "FTRSCH18, LR_EDS21",
        "FTRSCH15_ORA, LR_EDS21",
        "FTR_SCH15, LR_EDS21",
        "FTR_ORA, LR_EDS21",
        "FTR_HDC_ORA, LR_EDS21",
        "FTR_HDC, LR_EDS21",
        "FTR_56ORA, LR_EDS21",
        "FTR_56, LR_EDS21",
        "FTR, LR_EDS21",
        "EXT, LR",
        "EPP, LR_EPP",
        "EDSU18, LR_EDSU18",
        "EDS21, LR_EDS21",
        "EDS18, LR_EDS18",
        "DTO_ORA, LR_EDS21",
        "DTO, LR_EDS21",
        "DPP, LR_EDS21",
        "DLP, LR_EDS21",
        "DFL, LR_EDS21",
        "HDR_ORA, LR_EDS21",
        "CUR, LR_EDS21",
        "CR, LR_EDS21",
        "CIVILLT, LR_EDS21",
        "CIVIL, LR_EDS21",
        "BOTUS, LR_EDS21",
        "AR, LR_EDS21",
        "ALP_LASPO, LR_EDS21",
        "ALP_CODE21, LR_EDS21",
        "ALP_CODE18, LR_EDS21",
        "ALP, LR_EDS21",
        "A/FINE, LR_EDS21",
        "ADIMP_ORA, LR_EDS21",
        "ADIMP, LR_EDS21",
        "79, LR_EDS21",
        "78, LR_EDS21",
        "37, LR_EDS21",
        "33, LR_EDS21",
        "30, LR_EDS21",
        "14FTR_ORA, LR_EDS21",
        "14FTRHDC_ORA, LR_EDS21",
      ],
    )
    fun `should return legacySentenceCalcType given a sentence with recallType LR and Extended classification`(sentenceCalcType: String, expected: String) {
      // Given
      val sentence = aRasSentence(
        classification = SentenceTypeDetails.Classification.EXTENDED,
        latestRecallType = PersonSentence.LatestRecallType.LR,
        legacySentenceCalcType = sentenceCalcType,
      )

      // When
      val actual = sentence.legacySentenceCalcTypeForImprisonmentStatusCalculation

      // Then
      assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "YOI_ORA, LR_SOPC21",
        "YOI, LR_SOPC21",
        "TISCS, LR_SOPC21",
        "STS21, LR_SOPC21",
        "STS18, LR_SOPC21",
        "SOPC21, LR_SOPC21",
        "SOPC18, LR_SOPC18",
        "SEC94, LR_SOPC21",
        "SEC93_03, LR_SOPC21",
        "SEC93, LR_SOPC21",
        "SEC91_03_ORA, LR_SOPC21",
        "SEC91_03, LR_SOPC21",
        "SEC91, LR_SOPC21",
        "SEC275, LR_SOPC21",
        "SEC272, LR_SOPC21",
        "SEC250_ORA, LR_SOPC21",
        "SEC250, LR_SOPC21",
        "SEC236A, LR_SEC236A",
        "SDOPCU18, LR_SOPC18",
        "PPEXT_SENT, LR_SOPC21",
        "NP, LR_SOPC21",
        "MLP, LR_SOPC21",
        "LR_YOI_ORA, LR_SOPC21",
        "LR_SOPC21, LR_SOPC21",
        "LR_SOPC18, LR_SOPC18",
        "LR_SEC91_ORA, LR_SOPC21",
        "LRSEC250_ORA, LR_SOPC21",
        "LR_SEC236A, LR_SEC236A",
        "LR_ORA, LR_SOPC21",
        "LR_MLP, LR_SOPC21",
        "LR_LIFE, LR_SOPC21",
        "LR_LASPO_DR, LR_SOPC21",
        "LR_LASPO_AR, LR_SOPC21",
        "LR_IPP, LR_SOPC21",
        "LR_ES, LR_SOPC21",
        "LR_EPP, LR_SOPC21",
        "LR_EDSU18, LR_SOPC21",
        "LR_EDS21, LR_SOPC21",
        "LR_EDS18, LR_SOPC21",
        "LR_DPP, LR_SOPC21",
        "LR_DLP, LR_SOPC21",
        "LR_ALP_LASPO, LR_SOPC21",
        "LR_ALP_CDE21, LR_SOPC21",
        "LR_ALP_CDE18, LR_SOPC21",
        "LR_ALP, LR_SOPC21",
        "LR, LR_SOPC21",
        "LASPO_DR, LR_SOPC21",
        "LASPO_AR, LR_SOPC21",
        "IPP, LR_SOPC21",
        "HMPL, LR_SOPC21",
        "HDR_ORA, LR_SOPC21",
        "HDR, LR_SOPC21",
        "FTRSCH18_ORA, LR_SOPC21",
        "FTRSCH18, LR_SOPC21",
        "FTRSCH15_ORA, LR_SOPC21",
        "FTR_SCH15, LR_SOPC21",
        "FTR_ORA, LR_SOPC21",
        "FTR_HDC_ORA, LR_SOPC21",
        "FTR_HDC, LR_SOPC21",
        "FTR_56ORA, LR_SOPC21",
        "FTR_56, LR_SOPC21",
        "FTR, LR_SOPC21",
        "EXT, LR_SOPC21",
        "EPP, LR_SOPC21",
        "EDSU18, LR_SOPC21",
        "EDS21, LR_SOPC21",
        "EDS18, LR_SOPC21",
        "DTO_ORA, LR_SOPC21",
        "DTO, LR_SOPC21",
        "DPP, LR_SOPC21",
        "DLP, LR_SOPC21",
        "DFL, LR_SOPC21",
        "HDR_ORA, LR_SOPC21",
        "CUR, LR_SOPC21",
        "CR, LR_SOPC21",
        "CIVILLT, LR_SOPC21",
        "CIVIL, LR_SOPC21",
        "BOTUS, LR_SOPC21",
        "AR, LR_SOPC21",
        "ALP_LASPO, LR_SOPC21",
        "ALP_CODE21, LR_SOPC21",
        "ALP_CODE18, LR_SOPC21",
        "ALP, LR_SOPC21",
        "A/FINE, LR_SOPC21",
        "ADIMP_ORA, LR_SOPC21",
        "ADIMP, LR_SOPC21",
        "79, LR_SOPC21",
        "78, LR_SOPC21",
        "37, LR_SOPC21",
        "33, LR_SOPC21",
        "30, LR_SOPC21",
        "14FTR_ORA, LR_SOPC21",
        "14FTRHDC_ORA, LR_SOPC21",
      ],
    )
    fun `should return legacySentenceCalcType given a sentence with recallType LR and SOPC classification`(sentenceCalcType: String, expected: String) {
      // Given
      val sentence = aRasSentence(
        classification = SentenceTypeDetails.Classification.SOPC,
        latestRecallType = PersonSentence.LatestRecallType.LR,
        legacySentenceCalcType = sentenceCalcType,
      )

      // When
      val actual = sentence.legacySentenceCalcTypeForImprisonmentStatusCalculation

      // Then
      assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
      value = [
        "YOI_ORA, LR_LIFE",
        "YOI, LR_LIFE",
        "TISCS, LR_LIFE",
        "STS21, LR_LIFE",
        "STS18, LR_LIFE",
        "SOPC21, LR_LIFE",
        "SOPC18, LR_LIFE",
        "SEC94, LR_LIFE",
        "SEC93_03, LR_LIFE",
        "SEC93, LR_LIFE",
        "SEC91_03_ORA, LR_LIFE",
        "SEC91_03, LR_LIFE",
        "SEC91, LR_LIFE",
        "SEC275, LR_LIFE",
        "SEC272, LR_LIFE",
        "SEC250_ORA, LR_LIFE",
        "SEC250, LR_LIFE",
        "SEC236A, LR_LIFE",
        "SDOPCU18, LR_LIFE",
        "PPEXT_SENT, LR_LIFE",
        "NP, LR_LIFE",
        "MLP, LR_MLP",
        "LR_YOI_ORA, LR_LIFE",
        "LR_SOPC21, LR_LIFE",
        "LR_SOPC18, LR_LIFE",
        "LR_SEC91_ORA, LR_LIFE",
        "LRSEC250_ORA, LR_LIFE",
        "LR_SEC236A, LR_LIFE",
        "LR_ORA, LR_LIFE",
        "LR_MLP, LR_MLP",
        "LR_LIFE, LR_LIFE",
        "LR_LASPO_DR, LR_LIFE",
        "LR_LASPO_AR, LR_LIFE",
        "LR_IPP, LR_IPP",
        "LR_ES, LR_LIFE",
        "LR_EPP, LR_LIFE",
        "LR_EDSU18, LR_LIFE",
        "LR_EDS21, LR_LIFE",
        "LR_EDS18, LR_LIFE",
        "LR_DPP, LR_LIFE",
        "LR_DLP, LR_DLP",
        "LR_ALP_LASPO, LR_ALP_LASPO",
        "LR_ALP_CDE21, LR_ALP_CDE21",
        "LR_ALP_CDE18, LR_ALP_CDE18",
        "LR_ALP, LR_ALP",
        "LR, LR_LIFE",
        "LASPO_DR, LR_LIFE",
        "LASPO_AR, LR_LIFE",
        "IPP, LR_IPP",
        "HMPL, LR_LIFE",
        "HDR_ORA, LR_LIFE",
        "HDR, LR_LIFE",
        "FTRSCH18_ORA, LR_LIFE",
        "FTRSCH18, LR_LIFE",
        "FTRSCH15_ORA, LR_LIFE",
        "FTR_SCH15, LR_LIFE",
        "FTR_ORA, LR_LIFE",
        "FTR_HDC_ORA, LR_LIFE",
        "FTR_HDC, LR_LIFE",
        "FTR_56ORA, LR_LIFE",
        "FTR_56, LR_LIFE",
        "FTR, LR_LIFE",
        "EXT, LR_LIFE",
        "EPP, LR_LIFE",
        "EDSU18, LR_LIFE",
        "EDS21, LR_LIFE",
        "EDS18, LR_LIFE",
        "DTO_ORA, LR_LIFE",
        "DTO, LR_LIFE",
        "DPP, LR_LIFE",
        "DLP, LR_DLP",
        "DFL, LR_LIFE",
        "HDR_ORA, LR_LIFE",
        "CUR, LR_LIFE",
        "CR, LR_LIFE",
        "CIVILLT, LR_LIFE",
        "CIVIL, LR_LIFE",
        "BOTUS, LR_LIFE",
        "AR, LR_LIFE",
        "ALP_LASPO, LR_ALP_LASPO",
        "ALP_CODE21, LR_ALP_CDE21",
        "ALP_CODE18, LR_ALP_CDE18",
        "ALP, LR_ALP",
        "A/FINE, LR_LIFE",
        "ADIMP_ORA, LR_LIFE",
        "ADIMP, LR_LIFE",
        "79, LR_LIFE",
        "78, LR_LIFE",
        "37, LR_LIFE",
        "33, LR_LIFE",
        "30, LR_LIFE",
        "14FTR_ORA, LR_LIFE",
        "14FTRHDC_ORA, LR_LIFE",
      ],
    )
    fun `should return legacySentenceCalcType given a sentence with recallType LR and Intermediate classification`(sentenceCalcType: String, expected: String) {
      // Given
      val sentence = aRasSentence(
        classification = SentenceTypeDetails.Classification.INDETERMINATE,
        latestRecallType = PersonSentence.LatestRecallType.LR,
        legacySentenceCalcType = sentenceCalcType,
      )

      // When
      val actual = sentence.legacySentenceCalcTypeForImprisonmentStatusCalculation

      // Then
      assertThat(actual).isEqualTo(expected)
    }
  }
}
