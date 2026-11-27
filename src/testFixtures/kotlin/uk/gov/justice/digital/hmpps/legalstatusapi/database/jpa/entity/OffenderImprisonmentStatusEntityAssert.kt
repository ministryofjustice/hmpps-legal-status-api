package uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.entity

import org.assertj.core.api.AbstractObjectAssert
import java.time.Instant

fun assertThat(actual: OffenderImprisonmentStatusEntity?) = OffenderImprisonmentStatusEntityAssert(actual)

/**
 * AssertJ custom assertion for [OffenderImprisonmentStatusEntity]
 */
class OffenderImprisonmentStatusEntityAssert(actual: OffenderImprisonmentStatusEntity?) : AbstractObjectAssert<OffenderImprisonmentStatusEntityAssert, OffenderImprisonmentStatusEntity?>(actual, OffenderImprisonmentStatusEntityAssert::class.java) {

  fun isForPrisonNumber(expected: String): OffenderImprisonmentStatusEntityAssert {
    isNotNull
    with(actual!!) {
      if (prisonNumber != expected) {
        failWithMessage("Expected prisonNumber to be $expected, but it was $prisonNumber")
      }
    }
    return this
  }

  fun hasPrisonId(expected: String): OffenderImprisonmentStatusEntityAssert {
    isNotNull
    with(actual!!) {
      if (prisonId != expected) {
        failWithMessage("Expected prisonId to be $expected, but was $prisonId")
      }
    }
    return this
  }

  fun hasImprisonmentStatus(expected: String): OffenderImprisonmentStatusEntityAssert {
    isNotNull
    with(actual!!) {
      if (imprisonmentStatus != expected) {
        failWithMessage("Expected imprisonmentStatus to be $expected, but was $imprisonmentStatus")
      }
    }
    return this
  }

  fun hasSource(expected: SourceSystem): OffenderImprisonmentStatusEntityAssert {
    isNotNull
    with(actual!!) {
      if (source != expected) {
        failWithMessage("Expected source to be $expected, but was $source")
      }
    }
    return this
  }

  fun hasComment(expected: String): OffenderImprisonmentStatusEntityAssert {
    isNotNull
    with(actual!!) {
      if (comment != expected) {
        failWithMessage("Expected comment to be $expected, but was $comment")
      }
    }
    return this
  }

  fun hasNoComment(): OffenderImprisonmentStatusEntityAssert {
    isNotNull
    with(actual!!) {
      if (comment != null) {
        failWithMessage("Expected no comment, but was $comment")
      }
    }
    return this
  }

  fun wasCreatedBy(expected: String): OffenderImprisonmentStatusEntityAssert {
    isNotNull
    with(actual!!) {
      if (createdBy != expected) {
        failWithMessage("Expected createdBy to be $expected, but was $createdBy")
      }
    }
    return this
  }

  fun wasCreatedAt(expected: Instant): OffenderImprisonmentStatusEntityAssert {
    isNotNull
    with(actual!!) {
      if (createdAt != expected) {
        failWithMessage("Expected createdAt to be $expected, but was $createdAt")
      }
    }
    return this
  }

  fun wasUpdatedBy(expected: String): OffenderImprisonmentStatusEntityAssert {
    isNotNull
    with(actual!!) {
      if (createdBy != expected) {
        failWithMessage("Expected createdBy to be $expected, but was $createdBy")
      }
    }
    return this
  }

  fun wasUpdatedAt(expected: Instant): OffenderImprisonmentStatusEntityAssert {
    isNotNull
    with(actual!!) {
      if (updatedAt != expected) {
        failWithMessage("Expected updatedAt to be $expected, but was $updatedAt")
      }
    }
    return this
  }
}
