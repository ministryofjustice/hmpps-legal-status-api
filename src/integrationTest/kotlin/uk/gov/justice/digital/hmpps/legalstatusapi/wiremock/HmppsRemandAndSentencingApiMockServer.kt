package uk.gov.justice.digital.hmpps.legalstatusapi.wiremock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonCharge
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonCharges
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.SentenceTypeDetails
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_DATE

class HmppsRemandAndSentencingApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val hmppsRemandAndSentencingApi = HmppsRemandAndSentencingApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    hmppsRemandAndSentencingApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    hmppsRemandAndSentencingApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    hmppsRemandAndSentencingApi.stop()
  }
}

class HmppsRemandAndSentencingApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8093
    private val objectMapper = ObjectMapper()
      .registerModule(JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
  }

  fun stubGetPrisonerCharges(prisonNumber: String, fromDate: LocalDate?, toDate: LocalDate?, response: PersonCharges) {
    stubFor(
      get(urlPathMatching("/person/$prisonNumber/charges"))
        .withQueryParam(
          "fromDate",
          equalTo(ISO_DATE.format(fromDate)),
        )
        .withQueryParam(
          "toDate",
          equalTo(ISO_DATE.format(toDate)),
        )
        .willReturn(
          responseDefinition()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writeValueAsString(PersonCharges(charges = response))),
        ),
    )
  }

  fun stubGetSentenceTypeDetails(response: SentenceTypeDetails) {
    stubFor(
      get(urlPathMatching("/sentence-type/${response.sentenceTypeUuid}/details"))
        .willReturn(
          responseDefinition()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writeValueAsString(response)),
        ),
    )
  }
}
