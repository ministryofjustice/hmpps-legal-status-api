package uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy

import org.assertj.core.api.AbstractObjectAssert
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatus
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatusEntity

fun assertThat(actual: ImprisonmentStatusEntity?) = ImprisonmentStatusEntityAssert(actual)

/**
 * AssertJ custom assertion for [ImprisonmentStatusEntity]
 */
class ImprisonmentStatusEntityAssert(actual: ImprisonmentStatusEntity?) : AbstractObjectAssert<ImprisonmentStatusEntityAssert, ImprisonmentStatusEntity?>(actual, ImprisonmentStatusEntityAssert::class.java) {

  fun hasId(expected: String): ImprisonmentStatusEntityAssert {
    isNotNull
    with(actual!!) {
      if (id != expected) {
        failWithMessage("Expected id to be $expected, but was $id")
      }
    }
    return this
  }

  fun hasDescription(expected: String): ImprisonmentStatusEntityAssert {
    isNotNull
    with(actual!!) {
      if (description != expected) {
        failWithMessage("Expected description to be $expected, but was $description")
      }
    }
    return this
  }

  fun hasBand(expected: Int): ImprisonmentStatusEntityAssert {
    isNotNull
    with(actual!!) {
      if (band != expected) {
        failWithMessage("Expected band to be $expected, but was $band")
      }
    }
    return this
  }

  fun hasRank(expected: Int): ImprisonmentStatusEntityAssert {
    isNotNull
    with(actual!!) {
      if (rank != expected) {
        failWithMessage("Expected rank to be $expected, but was $rank")
      }
    }
    return this
  }

  fun hasSequence(expected: Int): ImprisonmentStatusEntityAssert {
    isNotNull
    with(actual!!) {
      if (sequence != expected) {
        failWithMessage("Expected sequence to be $expected, but was $sequence")
      }
    }
    return this
  }

  fun hasNoSequence(): ImprisonmentStatusEntityAssert {
    isNotNull
    with(actual!!) {
      if (sequence != null) {
        failWithMessage("Expected sequence to be null, but was $sequence")
      }
    }
    return this
  }

  fun isImprisonmentStatus(expected: ImprisonmentStatus): ImprisonmentStatusEntityAssert {
    isNotNull
    with(actual!!) {
      if (imprisonmentStatus != expected) {
        failWithMessage("Expected imprisonmentStatus to be $expected, but was $imprisonmentStatus")
      }
    }
    return this
  }
}
