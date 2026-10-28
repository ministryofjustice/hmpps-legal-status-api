package uk.gov.justice.digital.hmpps.legalstatusapi.client.remandandsentencing

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonCharges
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.SentenceTypeDetails
import java.time.LocalDate
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class RemandAndSentencingApiClient(
  @Qualifier("remandAndSentencingApiWebClient") private val webClient: WebClient,
  private val jsonMapper: JsonMapper,
) {

  /**
   * Returns the prisoner's charges, along with the raw response body so that it can be recorded exactly as RaS returned
   * it (our generated models ignore fields they don't know about).
   */
  fun getPrisonerCharges(prisonNumber: String, fromDate: LocalDate?, toDate: LocalDate?): PrisonerCharges = try {
    val rawJson = webClient
      .get()
      .uri { uri ->
        uri.path("/person/{prisonNumber}/charges")
        fromDate?.apply { uri.queryParam("fromDate", this) }
        toDate?.apply { uri.queryParam("toDate", this) }
        uri.build(prisonNumber)
      }
      .retrieve()
      .bodyToMono<String>()
      .block()!!
    PrisonerCharges(
      charges = jsonMapper.readValue(rawJson, PersonCharges::class.java).charges,
      rawJson = rawJson,
    )
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
