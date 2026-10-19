package uk.gov.justice.digital.hmpps.legalstatusapi.client.prisonersearch

import org.assertj.core.api.AbstractObjectAssert

fun assertThat(actual: PrisonerSearchApiException?) = PrisonerSearchApiExceptionAssert(actual)

/**
 * AssertJ custom assertion for [PrisonerSearchApiException]
 */
class PrisonerSearchApiExceptionAssert(actual: PrisonerSearchApiException?) : AbstractObjectAssert<PrisonerSearchApiExceptionAssert, PrisonerSearchApiException?>(actual, PrisonerSearchApiExceptionAssert::class.java) {

  fun hasMessage(expected: String): PrisonerSearchApiExceptionAssert {
    isNotNull
    with(actual!!) {
      if (message != expected) {
        failWithMessage("Expected message to be $expected, but was $message")
      }
    }
    return this
  }

  fun hasCause(expected: Throwable): PrisonerSearchApiExceptionAssert {
    isNotNull
    with(actual!!) {
      if (cause != expected) {
        failWithMessage("Expected cause to be $expected, but was $cause")
      }
    }
    return this
  }
}
