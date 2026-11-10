package uk.gov.justice.digital.hmpps.legalstatusapi.client.personlocation

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import uk.gov.justice.digital.hmpps.personlocation.resource.model.PeriodOfCustody
import java.time.Duration

private val log = KotlinLogging.logger {}

@Component
class PersonLocationApiClient(
  @param:Qualifier("personLocationApiWebClient")
  private val personLocationApiWebClient: WebClient,
) {

  /**
   * Returns the most recent period of custody for the prisoner, identified by their prisonNumber.
   * The period is current when `concludedAt` is null.
   *
   * Returns null when Person Location API has no period of custody for the prisoner (404).
   */
  fun getLatestPeriodOfCustody(prisonNumber: String): PeriodOfCustody? = try {
    personLocationApiWebClient
      .get()
      .uri("/integrations/people/{personIdentifier}/periods-of-custody/latest", prisonNumber)
      .retrieve()
      .bodyToMono<PeriodOfCustody>()
      .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
      .retryWhen(
        Retry.backoff(3, Duration.ofMillis(500))
          .filter { ex -> ex is WebClientRequestException }
          .doBeforeRetry { retrySignal ->
            log.warn(
              "Retrying request for latest period of custody with prisonNumber:{} due to {} (attempt #{})",
              prisonNumber,
              retrySignal.failure().javaClass.simpleName,
              retrySignal.totalRetries() + 1,
            )
          },
      )
      .block()
  } catch (e: Exception) {
    throw PersonLocationApiException("Error retrieving latest period of custody for prisonNumber $prisonNumber", e)
  }
}
