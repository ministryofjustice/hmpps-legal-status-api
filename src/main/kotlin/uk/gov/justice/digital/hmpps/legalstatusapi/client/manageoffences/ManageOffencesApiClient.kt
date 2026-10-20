package uk.gov.justice.digital.hmpps.legalstatusapi.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import uk.gov.justice.digital.hmpps.legalstatusapi.client.manageoffences.ManageOffencesApiException
import uk.gov.justice.digital.hmpps.manageoffences.resource.model.OffenceSeverityRating

@Component
class ManageOffencesApiClient(
  @Qualifier("manageOffencesApiWebClient")
  private val webClient: WebClient,
) {
  fun getOffenceSeverities(offenceCode: String): List<OffenceSeverityRating> = getOffenceSeverities(listOf(offenceCode))

  fun getOffenceSeverities(offenceCodes: List<String>): List<OffenceSeverityRating> = try {
    webClient
      .get()
      .uri {
        it.path("/offences/severity-ratings")
          .queryParam("offenceCodes", *offenceCodes.toTypedArray())
          .build()
      }
      .retrieve()
      .bodyToFlux<OffenceSeverityRating>()
      .collectList()
      .block()!!
  } catch (e: Exception) {
    throw ManageOffencesApiException(
      "Error retrieving offence severities for offenceCodes ${offenceCodes.joinToString(",")}",
      e,
    )
  }
}
