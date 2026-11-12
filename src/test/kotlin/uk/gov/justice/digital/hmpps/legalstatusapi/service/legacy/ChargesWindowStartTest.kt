package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.personlocation.resource.model.aValidPeriodOfCustody
import uk.gov.justice.digital.hmpps.personlocation.resource.model.aValidPreviousPeriod
import java.time.LocalDate
import java.time.OffsetDateTime

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
    "2026-05-10T14:00:00Z, 2026-05-11",
    // BST: 23:30 UTC is 00:30 on the next day in the UK
    "2026-06-01T23:30:00Z, 2026-06-03",
    // GMT: UTC and UK time are the same
    "2026-01-10T23:30:00Z, 2026-01-11",
    // An offset other than UTC: 00:30 on 2 June in the UK
    "2026-06-02T00:30:00+01:00, 2026-06-03",
  )
  fun `should return the day after the previous period concluded, as a UK date`(concludedAt: String, expectedFromDate: String) {
    // Given
    val periodOfCustody = aValidPeriodOfCustody(previousPeriod = aValidPreviousPeriod(concludedAt = OffsetDateTime.parse(concludedAt)))

    // When
    val fromDate = chargesWindowStart(periodOfCustody)

    // Then
    assertThat(fromDate).isEqualTo(LocalDate.parse(expectedFromDate))
  }

  @Test
  fun `should use the previous period when the latest period of custody has concluded`() {
    // Given
    val periodOfCustody = aValidPeriodOfCustody(
      commencedAt = OffsetDateTime.parse("2026-03-01T10:15:00Z"),
      concludedAt = OffsetDateTime.parse("2026-09-01T10:00:00Z"),
      previousPeriod = aValidPreviousPeriod(concludedAt = OffsetDateTime.parse("2025-01-20T09:00:00Z")),
    )

    // When
    val fromDate = chargesWindowStart(periodOfCustody)

    // Then
    assertThat(fromDate).isEqualTo(LocalDate.parse("2025-01-21"))
  }
}
