package uk.gov.justice.digital.hmpps.legalstatusapi.client.personlocation

import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.legalstatusapi.IntegrationTestBase
import uk.gov.justice.digital.hmpps.legalstatusapi.wiremock.HmppsPersonLocationApiExtension.Companion.hmppsPersonLocationApi
import uk.gov.justice.digital.hmpps.personlocation.resource.model.aValidPeriodOfCustody
import uk.gov.justice.digital.hmpps.personlocation.resource.model.aValidPreviousPeriod

class PersonLocationApiClientTest : IntegrationTestBase() {

  @Autowired
  private lateinit var personLocationApiClient: PersonLocationApiClient

  private val prisonNumber = "A1234BC"

  @BeforeEach
  fun stubAuth() = stubGetTokenFromHmppsAuth()

  @Test
  fun `returns the latest period of custody`() {
    val periodOfCustody = aValidPeriodOfCustody(previousPeriod = aValidPreviousPeriod())
    hmppsPersonLocationApi.stubGetLatestPeriodOfCustody(prisonNumber, periodOfCustody)

    val result = personLocationApiClient.getLatestPeriodOfCustody(prisonNumber)

    assertThat(result).isEqualTo(periodOfCustody)
    hmppsPersonLocationApi.verify(getRequestedFor(urlPathEqualTo("/integrations/people/$prisonNumber/periods-of-custody/latest")))
  }

  @Test
  fun `returns null when the prisoner has no period of custody`() {
    hmppsPersonLocationApi.stubGetLatestPeriodOfCustodyNotFound(prisonNumber)

    assertThat(personLocationApiClient.getLatestPeriodOfCustody(prisonNumber)).isNull()
  }

  @Test
  fun `throws PersonLocationApiException on a server error`() {
    hmppsPersonLocationApi.stubGetLatestPeriodOfCustodyError(prisonNumber)

    assertThatThrownBy { personLocationApiClient.getLatestPeriodOfCustody(prisonNumber) }
      .isInstanceOf(PersonLocationApiException::class.java)
      .hasMessageContaining(prisonNumber)
  }
}
