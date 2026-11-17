package uk.gov.justice.digital.hmpps.legalstatusapi.client.personlocation

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.legalstatusapi.IntegrationTestBase
import uk.gov.justice.digital.hmpps.legalstatusapi.wiremock.HmppsPersonLocationApiExtension.Companion.hmppsPersonLocationApi
import uk.gov.justice.digital.hmpps.personlocation.resource.model.aValidCodedDescription
import uk.gov.justice.digital.hmpps.personlocation.resource.model.aValidPeriodOfCustody
import uk.gov.justice.digital.hmpps.personlocation.resource.model.aValidPreviousPeriod
import java.time.LocalDateTime

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

  // The body is written by hand, not serialised from our model, so that it has the exact format that the real API
  // returns: dates with no offset. A model-based stub cannot catch a mismatch between the model and the real format.
  @Test
  fun `reads dates with no offset, in the format that the real API returns`() {
    hmppsPersonLocationApi.stubFor(
      get(urlPathEqualTo("/integrations/people/$prisonNumber/periods-of-custody/latest"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              {
                "commencedAt": "2026-03-30T13:48:03",
                "concludedAt": "2026-05-30T18:00:29",
                "status": { "code": "COMPLETED", "description": "Completed" },
                "previousPeriod": { "concludedAt": "2026-01-15T10:30:00" }
              }
              """.trimIndent(),
            ),
        ),
    )

    val result = personLocationApiClient.getLatestPeriodOfCustody(prisonNumber)

    assertThat(result).isEqualTo(
      aValidPeriodOfCustody(
        commencedAt = LocalDateTime.parse("2026-03-30T13:48:03"),
        concludedAt = LocalDateTime.parse("2026-05-30T18:00:29"),
        status = aValidCodedDescription(code = "COMPLETED", description = "Completed"),
        previousPeriod = aValidPreviousPeriod(concludedAt = LocalDateTime.parse("2026-01-15T10:30:00")),
      ),
    )
  }

  @Test
  fun `throws PeriodOfCustodyNotFoundException when the prisoner has no period of custody`() {
    hmppsPersonLocationApi.stubGetLatestPeriodOfCustodyNotFound(prisonNumber)

    assertThatThrownBy { personLocationApiClient.getLatestPeriodOfCustody(prisonNumber) }
      .isInstanceOf(PeriodOfCustodyNotFoundException::class.java)
      .hasMessageContaining(prisonNumber)
  }

  @Test
  fun `throws PersonLocationApiException on a server error`() {
    hmppsPersonLocationApi.stubGetLatestPeriodOfCustodyError(prisonNumber)

    assertThatThrownBy { personLocationApiClient.getLatestPeriodOfCustody(prisonNumber) }
      .isInstanceOf(PersonLocationApiException::class.java)
      .hasMessageContaining(prisonNumber)
  }
}
