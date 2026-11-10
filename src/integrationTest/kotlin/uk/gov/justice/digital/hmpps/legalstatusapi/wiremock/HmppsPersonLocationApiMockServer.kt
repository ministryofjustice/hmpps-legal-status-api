package uk.gov.justice.digital.hmpps.legalstatusapi.wiremock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.personlocation.resource.model.PeriodOfCustody

class HmppsPersonLocationApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val hmppsPersonLocationApi = HmppsPersonLocationApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    hmppsPersonLocationApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    hmppsPersonLocationApi.resetAll()
  }

  override fun afterAll(context: ExtensionContext) {
    hmppsPersonLocationApi.stop()
  }
}

class HmppsPersonLocationApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8095
    private val objectMapper = ObjectMapper()
      .registerModule(JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
  }

  fun stubGetLatestPeriodOfCustody(prisonNumber: String, response: PeriodOfCustody) = stubLatestPeriodOfCustody(prisonNumber, 200, objectMapper.writeValueAsString(response))

  fun stubGetLatestPeriodOfCustodyNotFound(prisonNumber: String) = stubLatestPeriodOfCustody(prisonNumber, 404, """{"status":404}""")

  fun stubGetLatestPeriodOfCustodyError(prisonNumber: String, status: Int = 500) = stubLatestPeriodOfCustody(prisonNumber, status, """{"status":$status}""")

  private fun stubLatestPeriodOfCustody(prisonNumber: String, status: Int, body: String) {
    stubFor(
      get(urlPathEqualTo("/integrations/people/$prisonNumber/periods-of-custody/latest"))
        .willReturn(
          responseDefinition()
            .withStatus(status)
            .withHeader("Content-Type", "application/json")
            .withBody(body),
        ),
    )
  }
}
