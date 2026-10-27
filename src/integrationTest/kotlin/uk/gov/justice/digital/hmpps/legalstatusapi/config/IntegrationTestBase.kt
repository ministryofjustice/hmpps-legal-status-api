package uk.gov.justice.digital.hmpps.legalstatusapi

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.legalstatusapi.config.MessagingHelper
import uk.gov.justice.digital.hmpps.legalstatusapi.container.LocalStackContainer
import uk.gov.justice.digital.hmpps.legalstatusapi.container.PostgresContainer
import uk.gov.justice.digital.hmpps.legalstatusapi.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.legalstatusapi.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.legalstatusapi.wiremock.HmppsManageOffencesApiExtension
import uk.gov.justice.digital.hmpps.legalstatusapi.wiremock.HmppsManageOffencesApiExtension.Companion.hmppsManageOffencesApi
import uk.gov.justice.digital.hmpps.legalstatusapi.wiremock.HmppsPrisonerSearchApiExtension
import uk.gov.justice.digital.hmpps.legalstatusapi.wiremock.HmppsPrisonerSearchApiExtension.Companion.hmppsPrisonerSearchApi
import uk.gov.justice.digital.hmpps.legalstatusapi.wiremock.HmppsRemandAndSentencingApiExtension
import uk.gov.justice.digital.hmpps.legalstatusapi.wiremock.HmppsRemandAndSentencingApiExtension.Companion.hmppsRemandAndSentencingApi
import uk.gov.justice.digital.hmpps.manageoffences.resource.model.OffenceSeverityRating
import uk.gov.justice.digital.hmpps.prisonersearch.resource.model.Prisoner
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonCharges
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.SentenceTypeDetails
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.time.LocalDate

@ExtendWith(
  HmppsAuthApiExtension::class,
  HmppsPrisonerSearchApiExtension::class,
  HmppsManageOffencesApiExtension::class,
  HmppsRemandAndSentencingApiExtension::class,
)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("integration-test")
@AutoConfigureWebTestClient(timeout = "PT5M")
abstract class IntegrationTestBase {

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var jwtAuthHelper: JwtAuthorisationHelper

  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  lateinit var objectMapper: ObjectMapper

  val messagingHelper by lazy { MessagingHelper(hmppsQueueService, objectMapper) }

  internal fun setAuthorisation(
    username: String? = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(username = username, scope = scopes, roles = roles)

  fun stubPingWithResponse(status: Int) {
    hmppsAuth.stubHealthPing(status)
  }

  fun stubGetTokenFromHmppsAuth() = hmppsAuth.stubGrantToken()

  fun stubGetPrisonerFromPrisonerSearchApi(response: Prisoner) = hmppsPrisonerSearchApi.stubGetPrisoner(response.prisonerNumber!!, response)

  fun stubGetChargesFromRemandAndSentencingApi(prisonNumber: String, fromDate: LocalDate?, toDate: LocalDate?, response: PersonCharges) = hmppsRemandAndSentencingApi.stubGetPrisonerCharges(prisonNumber, fromDate, toDate, response)

  fun stubGetSentenceTypeDetails(response: SentenceTypeDetails) = hmppsRemandAndSentencingApi.stubGetSentenceTypeDetails(response)

  fun stubGetOffenceSeverities(offenceCodes: Set<String>, response: List<OffenceSeverityRating>) = hmppsManageOffencesApi.stubGetOffenceSeverities(offenceCodes, response)

  companion object {
    private val postgres = PostgresContainer.instance
    private val localStackContainer = LocalStackContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      registry.add("spring.datasource.url", postgres::getJdbcUrl)
      registry.add("spring.datasource.username", postgres::getUsername)
      registry.add("spring.datasource.password", postgres::getPassword)

      System.setProperty("aws.region", "eu-west-2")
      localStackContainer?.also { LocalStackContainer.setLocalStackProperties(it, registry) }
    }
  }
}
