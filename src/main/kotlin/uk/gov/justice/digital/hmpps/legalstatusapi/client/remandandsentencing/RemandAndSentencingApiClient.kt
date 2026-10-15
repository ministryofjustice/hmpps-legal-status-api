package uk.gov.justice.digital.hmpps.legalstatusapi.client.remandandsentencing

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

// Methods to be added once we know which Remand and Sentencing endpoint(s) the calculation needs.
// Request/response types should come from the generated uk.gov.justice.digital.hmpps.remandandsentencing.resource.model classes.
@Component
class RemandAndSentencingApiClient(@Qualifier("remandAndSentencingApiWebClient") private val webClient: WebClient)
