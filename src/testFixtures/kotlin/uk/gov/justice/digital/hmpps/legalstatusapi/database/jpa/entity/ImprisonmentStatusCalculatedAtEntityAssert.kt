package uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.entity

import org.assertj.core.api.AbstractObjectAssert
import java.time.Instant

fun assertThat(actual: ImprisonmentStatusCalculatedAtEntity?) = ImprisonmentStatusCalculatedAtEntityAssert(actual)

/**
 * AssertJ custom assertion for [ImprisonmentStatusCalculatedAtEntity]
 */
class ImprisonmentStatusCalculatedAtEntityAssert(actual: ImprisonmentStatusCalculatedAtEntity?) : AbstractObjectAssert<ImprisonmentStatusCalculatedAtEntityAssert, ImprisonmentStatusCalculatedAtEntity?>(actual, ImprisonmentStatusCalculatedAtEntityAssert::class.java) {

  fun isForPrisonNumber(expected: String): ImprisonmentStatusCalculatedAtEntityAssert {
    isNotNull
    with(actual!!) {
      if (prisonNumber != expected) {
        failWithMessage("Expected prisonNumber to be $expected, but it was $prisonNumber")
      }
    }
    return this
  }

  fun wasCalculatedAt(expected: Instant): ImprisonmentStatusCalculatedAtEntityAssert {
    isNotNull
    with(actual!!) {
      if (calculatedAt != expected) {
        failWithMessage("Expected calculatedAt to be $expected, but it was $calculatedAt")
      }
    }
    return this
  }

  fun wasCalculatedAfter(instant: Instant): ImprisonmentStatusCalculatedAtEntityAssert {
    isNotNull
    with(actual!!) {
      if (!calculatedAt.isAfter(instant)) {
        failWithMessage("Expected calculatedAt to be after $instant, but it was $calculatedAt")
      }
    }
    return this
  }

  fun hasVersion(expected: Int): ImprisonmentStatusCalculatedAtEntityAssert {
    isNotNull
    with(actual!!) {
      if (version != expected) {
        failWithMessage("Expected version to be $expected, but it was $version")
      }
    }
    return this
  }
}
