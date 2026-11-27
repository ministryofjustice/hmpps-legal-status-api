package uk.gov.justice.digital.hmpps.legalstatusapi.resource.model

import org.assertj.core.api.AbstractAssert
import java.time.LocalDateTime

fun assertThat(actual: ImprisonmentStatusResponse?) = ImprisonmentStatusResponseAssert(actual)

class ImprisonmentStatusResponseAssert(actual: ImprisonmentStatusResponse?) : AbstractAssert<ImprisonmentStatusResponseAssert, ImprisonmentStatusResponse?>(actual, ImprisonmentStatusResponseAssert::class.java) {

  fun isForPrisonNumber(expected: String): ImprisonmentStatusResponseAssert {
    isNotNull
    with(actual!!) {
      if (prisonNumber != expected) {
        failWithMessage("Expected prisonNumber to be $expected, but was $prisonNumber")
      }
    }
    return this
  }

  fun hasPrisonId(expected: String): ImprisonmentStatusResponseAssert {
    isNotNull
    with(actual!!) {
      if (prisonId != expected) {
        failWithMessage("Expected prisonId to be $expected, but was $prisonId")
      }
    }
    return this
  }

  fun hasStatus(expected: String): ImprisonmentStatusResponseAssert {
    isNotNull
    with(actual!!) {
      if (status != expected) {
        failWithMessage("Expected status to be $expected, but was $status")
      }
    }
    return this
  }

  fun hasSource(expected: SourceSystem): ImprisonmentStatusResponseAssert {
    isNotNull
    with(actual!!) {
      if (source != expected) {
        failWithMessage("Expected source to be $expected, but was $source")
      }
    }
    return this
  }

  fun hasComment(expected: String): ImprisonmentStatusResponseAssert {
    isNotNull
    with(actual!!) {
      if (comment != expected) {
        failWithMessage("Expected comment to be $expected, but was $comment")
      }
    }
    return this
  }

  fun hasNoComment(): ImprisonmentStatusResponseAssert {
    isNotNull
    with(actual!!) {
      if (comment != null) {
        failWithMessage("Expected no comment, but was $comment")
      }
    }
    return this
  }

  fun wasCreatedBy(expected: String): ImprisonmentStatusResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdBy != expected) {
        failWithMessage("Expected createdBy to be $expected, but was $createdBy")
      }
    }
    return this
  }

  fun wasCreatedAt(expected: LocalDateTime): ImprisonmentStatusResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdAt != expected) {
        failWithMessage("Expected createdAt to be $expected, but was $createdAt")
      }
    }
    return this
  }

  fun wasUpdatedBy(expected: String): ImprisonmentStatusResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdBy != expected) {
        failWithMessage("Expected createdBy to be $expected, but was $createdBy")
      }
    }
    return this
  }

  fun wasUpdatedAt(expected: LocalDateTime): ImprisonmentStatusResponseAssert {
    isNotNull
    with(actual!!) {
      if (updatedAt != expected) {
        failWithMessage("Expected updatedAt to be $expected, but was $updatedAt")
      }
    }
    return this
  }
}
