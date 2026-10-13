package uk.gov.justice.digital.hmpps.legalstatusapi.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

// Methods to be added once we know which Manage Offences endpoint(s) the calculation needs.
// Request/response types should come from the generated uk.gov.justice.digital.hmpps.manageoffences.resource.model classes.
@Component
class ManageOffencesApiClient(@Qualifier("manageOffencesApiWebClient") private val webClient: WebClient)
