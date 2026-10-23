package uk.gov.justice.digital.hmpps.legalstatusapi.client.slack

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class SlackApiClient(
  @Qualifier("slackApiWebClient") private val webClient: WebClient,
)
