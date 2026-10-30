package uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.legalstatusapi.IntegrationTestBase
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatus
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.repository.ImprisonmentStatusMappingRepository

/**
 * Temporary test class for ImprisonmentStatusMappingRepository whilst we do not have any other tests that test the
 * repository and entity implicitly.
 *
 * TODO - remove this test class once we have other tests that cover the repository and entity implicitly.
 */
class ImprisonmentStatusMappingRepositoryTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: ImprisonmentStatusMappingRepository

  @Test
  fun `should load all NOMIS mappings to active Imprisonment Statuses`() {
    // Given

    // When
    val actual = repository.count()

    // Then
    Assertions.assertThat(actual).isEqualTo(340)
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "2020, ADIMP_ORA, ADIMP_ORA20",
      "2020, FTR_56ORA, FTR_56ORA",
      "2003, FTR_56, FTR_56ORA",
      "1991, A/FINE, A_FINE",
    ],
  )
  fun `should retrieve mapping by sentence category and calc type`(sentenceCategory: String, sentenceCalcType: String, expected: ImprisonmentStatus) {
    // Given

    // When
    val actual = repository.findBySentenceCategoryAndSentenceCalcType(sentenceCategory, sentenceCalcType)

    // Then
    assertThat(actual)
      .hasSentence(sentenceCategory, sentenceCalcType)
      .isMappedTo(expected)
  }

  @ParameterizedTest
  @CsvSource(
    value = [
      "4565, TRL",
      "1501, LR",
      "1003, HMPL",
    ],
  )
  fun `should retrieve mapping by offence result code`(offenceResultCode: String, expected: ImprisonmentStatus) {
    // Given

    // When
    val actual = repository.findByOffenceResultCode(offenceResultCode)

    // Then
    assertThat(actual)
      .hasOffenceResultCode(offenceResultCode)
      .isMappedTo(expected)
  }

  @Test
  fun `should not retrieve mapping given invalid codes`() {
    // Given
    val invalidCode = "INVALID_CODE"

    // When
    val byResultCode = repository.findByOffenceResultCode(invalidCode)
    val bySentence = repository.findBySentenceCategoryAndSentenceCalcType("2020", invalidCode)

    // Then
    assertThat(byResultCode).isNull()
    assertThat(bySentence).isNull()
  }
}
