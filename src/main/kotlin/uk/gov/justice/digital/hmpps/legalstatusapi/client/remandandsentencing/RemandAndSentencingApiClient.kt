package uk.gov.justice.digital.hmpps.legalstatusapi.client.remandandsentencing

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonCharge
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonCharges
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.SentenceTypeDetails
import java.time.LocalDate
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class RemandAndSentencingApiClient(
  @Qualifier("remandAndSentencingApiWebClient") private val webClient: WebClient,
) {

  fun getPrisonerCharges(prisonNumber: String, fromDate: LocalDate?, toDate: LocalDate?): List<PersonCharge> = try {
    webClient
      .get()
      .uri { uri ->
        uri.path("/person/{prisonNumber}/charges")
        fromDate?.apply { uri.queryParam("fromDate", this) }
        toDate?.apply { uri.queryParam("toDate", this) }
        uri.build(prisonNumber)
      }
      .retrieve()
      .bodyToMono<PersonCharges>()
      .block()!!
      .charges
  } catch (e: Exception) {
    throw RemandAndSentencingApiException("Failed to retrieve charges for prisoner: $prisonNumber", e)
  }

  fun getSentenceTypeDetails(sentenceTypeId: UUID): SentenceTypeDetails = try {
    webClient.get()
      .uri("/sentence-type/{sentenceTypeId}/details", sentenceTypeId)
      .retrieve()
      .bodyToMono<SentenceTypeDetails>()
      .block()!!
  } catch (e: Exception) {
    throw RemandAndSentencingApiException(
      "Failed to retrieve sentence type details for sentence type ID: $sentenceTypeId",
      e,
    )
  }
}
