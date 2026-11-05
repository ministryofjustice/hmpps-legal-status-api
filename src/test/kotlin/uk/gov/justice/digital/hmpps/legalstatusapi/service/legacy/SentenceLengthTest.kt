package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonPeriodLength.PeriodLengthType.CUSTODIAL_TERM
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonPeriodLength.PeriodLengthType.LICENCE_PERIOD
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonPeriodLength.PeriodLengthType.TERM_LENGTH
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.aValidPersonPeriodLength

class SentenceLengthTest {

  @Test
  fun `should return null given no period lengths`() {
    assertThat(SentenceLength.inDays(emptyList())).isNull()
  }

  @Test
  fun `should return 0 given a period length with no values`() {
    assertThat(SentenceLength.inDays(listOf(aValidPersonPeriodLength()))).isEqualTo(0)
  }

  @Test
  fun `should treat weeks as 7 days`() {
    assertThat(SentenceLength.inDays(listOf(aValidPersonPeriodLength(weeks = 2, days = 3)))).isEqualTo(17)
  }

  @Test
  fun `should count years and months as calendar months from the anchor date`() {
    // From 2000-01-01: 2000 is a leap year, so 1 year is 366 days; 1 month is January, 31 days
    assertThat(SentenceLength.inDays(listOf(aValidPersonPeriodLength(years = 1)))).isEqualTo(366)
    assertThat(SentenceLength.inDays(listOf(aValidPersonPeriodLength(months = 1)))).isEqualTo(31)
    assertThat(SentenceLength.inDays(listOf(aValidPersonPeriodLength(months = 12)))).isEqualTo(366)
  }

  @Test
  fun `should add the months before the days`() {
    // 2000-01-01 + 1 month = 2000-02-01, + 30 days = 2000-03-02 (61 days). Days first would give 2000-02-29 (59 days)
    assertThat(SentenceLength.inDays(listOf(aValidPersonPeriodLength(months = 1, days = 30)))).isEqualTo(61)
  }

  @Test
  fun `should sum all period lengths whatever their type`() {
    // e.g. an extended sentence: NOMIS's END_DATE includes the licence period
    val periodLengths = listOf(
      aValidPersonPeriodLength(years = 2, periodLengthType = CUSTODIAL_TERM),
      aValidPersonPeriodLength(years = 1, periodLengthType = LICENCE_PERIOD),
    )

    assertThat(SentenceLength.inDays(periodLengths)).isEqualTo(SentenceLength.inDays(listOf(aValidPersonPeriodLength(years = 3))))
  }

  @Test
  fun `should ignore the period order`() {
    val actual = SentenceLength.inDays(listOf(aValidPersonPeriodLength(years = 1, days = 10, periodOrder = "days,years", periodLengthType = TERM_LENGTH)))

    assertThat(actual).isEqualTo(376)
  }

  @Test
  fun `should order sentences by length`() {
    val sixMonths = SentenceLength.inDays(listOf(aValidPersonPeriodLength(months = 6)))!!
    val twentySixWeeks = SentenceLength.inDays(listOf(aValidPersonPeriodLength(weeks = 26)))!!

    // 6 months from 2000-01-01 is 182 days, 26 weeks is 182 days
    assertThat(sixMonths).isEqualTo(182)
    assertThat(twentySixWeeks).isEqualTo(182)
    assertThat(SentenceLength.inDays(listOf(aValidPersonPeriodLength(months = 7))))
      .isGreaterThan(sixMonths)
  }
}
