package uk.gov.justice.digital.hmpps.legalstatusapi.resource.model

import org.assertj.core.api.AbstractAssert

fun assertThat(actual: CreateImprisonmentStatusRequest?) = CreateImprisonmentStatusRequestAssert(actual)

class CreateImprisonmentStatusRequestAssert(actual: CreateImprisonmentStatusRequest?) : AbstractAssert<CreateImprisonmentStatusRequestAssert, CreateImprisonmentStatusRequest?>(actual, CreateImprisonmentStatusRequestAssert::class.java) {

  fun isForPrisonNumber(expected: String): CreateImprisonmentStatusRequestAssert {
    isNotNull
    with(actual!!) {
      if (prisonNumber != expected) {
        failWithMessage("Expected prisonNumber to be $expected, but was $prisonNumber")
      }
    }
    return this
  }
}
