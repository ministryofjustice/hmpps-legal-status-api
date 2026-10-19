package uk.gov.justice.digital.hmpps.legalstatusapi.client.prisonersearch

import org.assertj.core.api.AbstractObjectAssert

fun assertThat(actual: PrisonerNotFoundException?) = PrisonerNotFoundExceptionAssert(actual)

/**
 * AssertJ custom assertion for [PrisonerNotFoundException]
 */
class PrisonerNotFoundExceptionAssert(actual: PrisonerNotFoundException?) : AbstractObjectAssert<PrisonerNotFoundExceptionAssert, PrisonerNotFoundException?>(actual, PrisonerNotFoundExceptionAssert::class.java) {

  fun hasPrisonNumber(expected: String): PrisonerNotFoundExceptionAssert {
    isNotNull
    with(actual!!) {
      if (prisonNumber != expected) {
        failWithMessage("Expected prisonNumber to be $expected, but was $prisonNumber")
      }
    }
    return this
  }
}
