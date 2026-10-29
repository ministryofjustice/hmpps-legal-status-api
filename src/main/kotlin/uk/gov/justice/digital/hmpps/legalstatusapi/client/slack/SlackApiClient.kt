package uk.gov.justice.digital.hmpps.legalstatusapi.client.slack

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Component
class SlackApiClient(
  @Qualifier("slackApiWebClient") private val webClient: WebClient,
  private val clock: Clock,
) {
  fun postToSlackApp(prisonNumber: String, nomisStatus: String, dpsStatus: String, dbReference: UUID) {
    val slackMessage = mapOf(
      "prisonNumber" to prisonNumber,
      "nomisStatus" to nomisStatus,
      "dpsStatus" to dpsStatus,
      "dbReference" to dbReference,
      "when" to LocalDateTime.now(clock).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    )

    webClient.post()
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .bodyValue(slackMessage)
      .retrieve()
      .bodyToMono<String>()
      .block()!!
  }
}
