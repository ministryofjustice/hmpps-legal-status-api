package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.personlocation.resource.model.aValidPeriodOfCustody
import uk.gov.justice.digital.hmpps.personlocation.resource.model.aValidPreviousPeriod
import java.time.LocalDate
import java.time.LocalDateTime

class ChargesWindowStartTest {

  @Test
  fun `should return null when there is no previous period of custody`() {
    // Given
    val periodOfCustody = aValidPeriodOfCustody(previousPeriod = null)

    // When
    val fromDate = chargesWindowStart(periodOfCustody)

    // Then
    assertThat(fromDate).isNull()
  }

  @ParameterizedTest(name = "previous period concluded at {0} gives fromDate {1}")
  @CsvSource(
    // A release in the middle of the day
    "2026-05-10T14:00:00, 2026-05-11",
    // The format that the Person Location API returns
    "2026-05-30T18:00:29, 2026-05-31",
    // Late in the day: the date is used as it is, with no time zone conversion
    "2026-06-01T23:30:00, 2026-06-02",
    // Just after midnight
    "2026-06-02T00:30:00, 2026-06-03",
  )
  fun `should return the day after the date on which the previous period concluded`(concludedAt: String, expectedFromDate: String) {
    // Given
    val periodOfCustody = aValidPeriodOfCustody(previousPeriod = aValidPreviousPeriod(concludedAt = LocalDateTime.parse(concludedAt)))

    // When
    val fromDate = chargesWindowStart(periodOfCustody)

    // Then
    assertThat(fromDate).isEqualTo(LocalDate.parse(expectedFromDate))
  }

  @Test
  fun `should use the previous period when the latest period of custody has concluded`() {
    // Given
    val periodOfCustody = aValidPeriodOfCustody(
      commencedAt = LocalDateTime.parse("2026-03-01T10:15:00"),
      concludedAt = LocalDateTime.parse("2026-09-01T10:00:00"),
      previousPeriod = aValidPreviousPeriod(concludedAt = LocalDateTime.parse("2025-01-20T09:00:00")),
    )

    // When
    val fromDate = chargesWindowStart(periodOfCustody)

    // Then
    assertThat(fromDate).isEqualTo(LocalDate.parse("2025-01-21"))
  }
}
