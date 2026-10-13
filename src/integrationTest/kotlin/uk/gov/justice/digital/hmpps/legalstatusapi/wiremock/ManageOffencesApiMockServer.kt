package uk.gov.justice.digital.hmpps.legalstatusapi.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class ManageOffencesApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val manageOffencesApi = ManageOffencesApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    manageOffencesApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    manageOffencesApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    manageOffencesApi.stop()
  }
}

class ManageOffencesApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8091
  }

  fun stubHealthPing(status: Int) {
    stubFor(
      get("/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(if (status == 200) """{"status":"UP"}""" else """{"status":"DOWN"}""")
          .withStatus(status),
      ),
    )
  }
}
