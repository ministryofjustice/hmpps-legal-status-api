package uk.gov.justice.digital.hmpps.legalstatusapi.client.prisonersearch

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.util.retry.Retry
import uk.gov.justice.digital.hmpps.prisonersearch.resource.model.Prisoner
import java.time.Duration

private val log = KotlinLogging.logger {}

@Component
class PrisonerSearchApiClient(
  @param:Qualifier("prisonerSearchApiWebClient")
  private val prisonerSearchApiWebClient: WebClient,
) {

  private companion object {
    val RESPONSE_FIELDS = listOf(
      "prisonerNumber",
      "receptionDate",
      "imprisonmentStatus",
    ).joinToString(",")
  }

  /**
   * Returns a specific prisoner from prisoner-search-api, identified by their prisonNumber
   */
  fun getPrisoner(prisonNumber: String): Prisoner = try {
    prisonerSearchApiWebClient
      .get()
      .uri {
        it.path("/prisoner/{prisonNumber}")
          .queryParam("responseFields", RESPONSE_FIELDS)
          .build(prisonNumber)
      }
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .retrieve()
      .bodyToMono(Prisoner::class.java)
      .retryWhen(
        Retry.backoff(3, Duration.ofMillis(500))
          .filter { ex -> ex is WebClientRequestException }
          .doBeforeRetry { retrySignal ->
            log.warn(
              "Retrying request for prisoner with prisonNumber:{} due to {} (attempt #{})",
              prisonNumber,
              retrySignal.failure().javaClass.simpleName,
              retrySignal.totalRetries() + 1,
            )
          },
      )
      .block()!!
  } catch (e: WebClientResponseException.NotFound) {
    throw PrisonerNotFoundException(prisonNumber)
  } catch (e: Exception) {
    throw PrisonerSearchApiException("Error retrieving prisoner by prisonNumber $prisonNumber", e)
  }
}
