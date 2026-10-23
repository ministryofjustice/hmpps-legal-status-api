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
    val slackMessage = """
      {
        "blocks": [
          {
            "type": "header",
            "text": {
              "type": "plain_text",
              "text": "Mismatched Imprisonment Status Calculation",
              "emoji": true
            }
          },
          {
            "type": "section",
            "fields": [
              {
                "type": "mrkdwn",
                "text": "*PRN:*\n$prisonNumber"
              }
            ]
          },
          {
            "type": "section",
            "fields": [
              {
                "type": "mrkdwn",
                "text": "*NOMIS:*\n$nomisStatus"
              },
              {
                "type": "mrkdwn",
                "text": "*DPS Legal Status:*\n$dpsStatus"
              }
            ]
          },
          {
            "type": "section",
            "fields": [
              {
                "type": "mrkdwn",
                "text": "*When:*\n${LocalDateTime.now(clock).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}"
              }
            ]
          },
          {
            "type": "actions",
            "elements": [
              {
                "type": "button",
                "text": {
                  "type": "plain_text",
                  "emoji": true,
                  "text": "Create Jira ticket"
                },
                "style": "primary",
                "value": "$dbReference"
              }
            ]
          }
        ]  
      }
    """.trimIndent()

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
