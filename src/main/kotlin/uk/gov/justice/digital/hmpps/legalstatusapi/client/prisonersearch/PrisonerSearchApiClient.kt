package uk.gov.justice.digital.hmpps.legalstatusapi.client.prisonersearch

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.prisonersearch.resource.model.Prisoner

@Component
class PrisonerSearchApiClient(
  @param:Qualifier("prisonerSearchApiWebClient")
  private val prisonerSearchApiWebClient: WebClient,
) {

  /**
   * Returns a specific prisoner from prisoner-search-api, identified by their prisonNumber
   */
  fun getPrisoner(prisonNumber: String): Prisoner = try {
    prisonerSearchApiWebClient
      .get()
      .uri("/prisoner/{prisonNumber}", prisonNumber)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .retrieve()
      .bodyToMono(Prisoner::class.java)
      .block()!!
  } catch (e: WebClientResponseException.NotFound) {
    throw PrisonerNotFoundException(prisonNumber)
  } catch (e: Exception) {
    throw PrisonerSearchApiException("Error retrieving prisoner by prisonNumber $prisonNumber", e)
  }
}
