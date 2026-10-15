package uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.legalstatusapi.IntegrationTestBase
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatus
import kotlin.jvm.optionals.getOrNull

/**
 * Temporary test class for ImprisonmentStatusRepository whilst we do not have any other tests that test the repository
 * and entity implicitly.
 *
 * TODO - remove this test class once we have other tests that cover the repository and entity implicitly.
 */
class ImprisonmentStatusRepositoryTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: ImprisonmentStatusRepository

  @ParameterizedTest
  @CsvSource(
    value = [
      "AFIXED, AFIXED",
      "14FTRHDC_ORA, FTR_14_HDC_ORA",
      "FTR/08, FTR08",
      "SINE DIE, SINE_DIE",
      "NON-CUST, NON_CUST",
    ],
  )
  fun `should retrieve Imprisonment Status by NOMIS code`(nomisCode: String, expected: ImprisonmentStatus) {
    // Given

    // When
    val actual = repository.findById(nomisCode).get()

    // Then
    assertThat(actual)
      .isImprisonmentStatus(expected)
  }

  @Test
  fun `should not retrieve Imprisonment Status given invalid code`() {
    // Given
    val invalidNomisCode = "INVALID_CODE"

    // When
    val actual = repository.findById(invalidNomisCode).getOrNull()

    // Then
    assertThat(actual).isNull()
  }
}
