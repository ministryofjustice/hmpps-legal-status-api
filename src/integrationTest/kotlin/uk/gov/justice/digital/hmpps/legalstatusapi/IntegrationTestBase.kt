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
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.legalstatusapi.container.LocalStackContainer
import uk.gov.justice.digital.hmpps.legalstatusapi.container.PostgresContainer
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.DOMAIN_EVENTS_QUEUE_ID
import uk.gov.justice.digital.hmpps.legalstatusapi.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.legalstatusapi.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.legalstatusapi.wiremock.HmppsManageOffencesApiExtension
import uk.gov.justice.digital.hmpps.legalstatusapi.wiremock.HmppsPrisonerSearchApiExtension
import uk.gov.justice.digital.hmpps.legalstatusapi.wiremock.HmppsRemandAndSentencingApiExtension
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.MissingTopicException
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

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
  protected lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthorisationHelper

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  protected val domainEventsQueue by lazy {
    hmppsQueueService.findByQueueId(DOMAIN_EVENTS_QUEUE_ID)
      ?: throw MissingQueueException("HmppsQueue $DOMAIN_EVENTS_QUEUE_ID not found")
  }

  protected val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("hmppseventtopic")
      ?: throw MissingTopicException("HmppsTopic hmppseventtopic not found")
  }

  protected fun purgeDomainEventsQueues() {
    domainEventsQueue.sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(domainEventsQueue.queueUrl).build()).get()
    domainEventsQueue.sqsDlqClient!!.purgeQueue(PurgeQueueRequest.builder().queueUrl(domainEventsQueue.dlqUrl).build()).get()
  }

  protected fun messagesOnDomainEventsQueue(): Int = domainEventsQueue.sqsClient.countAllMessagesOnQueue(domainEventsQueue.queueUrl).get()

  protected fun messagesOnDomainEventsDlq(): Int = domainEventsQueue.sqsDlqClient!!.countAllMessagesOnQueue(domainEventsQueue.dlqUrl!!).get()

  internal fun setAuthorisation(
    username: String? = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(username = username, scope = scopes, roles = roles)

  protected fun stubPingWithResponse(status: Int) {
    hmppsAuth.stubHealthPing(status)
  }

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
