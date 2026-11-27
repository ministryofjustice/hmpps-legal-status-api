package uk.gov.justice.digital.hmpps.legalstatusapi.resource.model

import org.assertj.core.api.AbstractAssert

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
}
