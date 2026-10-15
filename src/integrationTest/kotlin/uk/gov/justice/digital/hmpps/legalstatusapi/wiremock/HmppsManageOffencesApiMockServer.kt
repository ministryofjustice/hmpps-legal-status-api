package uk.gov.justice.digital.hmpps.legalstatusapi.wiremock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import com.github.tomakehurst.wiremock.matching.ExactMatchMultiValuePattern
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.manageoffences.resource.model.OffenceSeverityRating

class HmppsManageOffencesApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val hmppsManageOffencesApi = HmppsManageOffencesApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    hmppsManageOffencesApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    hmppsManageOffencesApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    hmppsManageOffencesApi.stop()
  }
}

class HmppsManageOffencesApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8091
    private val objectMapper = ObjectMapper()
      .registerModule(JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
  }

  fun stubGetOffenceSeverities(vararg offenceCodes: String, response: List<OffenceSeverityRating>) {
    stubFor(
      get(urlPathMatching("/offences/severity-ratings"))
        .withQueryParam(
          "offenceCodes",
          ExactMatchMultiValuePattern(offenceCodes.map { EqualToPattern(it) }),
        )
        .willReturn(
          responseDefinition()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writeValueAsString(response)),
        ),
    )
  }
}
