package uk.gov.justice.digital.hmpps.legalstatusapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @param:Value("\${hmpps-auth.url}") val hmppsAuthBaseUri: String,
  @param:Value("\${manage-offences.api.url}") val manageOffencesApiBaseUri: String,
  @param:Value("\${api.health-timeout:2s}") val healthTimeout: Duration,
  @param:Value("\${api.timeout:20s}") val timeout: Duration,
) {
  // HMPPS Auth health ping is required if your service calls HMPPS Auth to get a token to call other services
  @Bean
  fun hmppsAuthHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(hmppsAuthBaseUri, healthTimeout)

  @Bean
  fun manageOffencesApiHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(manageOffencesApiBaseUri, healthTimeout)

  @Bean
  fun manageOffencesApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(authorizedClientManager, "manage-offences-api", manageOffencesApiBaseUri, timeout)
}
