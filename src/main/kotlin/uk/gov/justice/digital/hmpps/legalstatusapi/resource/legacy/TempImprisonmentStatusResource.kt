package uk.gov.justice.digital.hmpps.legalstatusapi.resource.legacy

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.legalstatusapi.client.slack.SlackApiClient
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.ImprisonmentStatusCalculation
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.ImprisonmentStatusCalculator
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.LegacyDataService
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.LegacySentencingData
import java.util.UUID

/**
 * TEMPORARY development endpoints for invoking the imprisonment status calculation on demand.
 *
 * TODO - Delete once calculateImprisonmentStatus is wired into the domain events listener.
 */
@Hidden
@RestController
@RequestMapping("/temp/legacy")
class TempImprisonmentStatusResource(
  private val legacyDataService: LegacyDataService,
  private val imprisonmentStatusCalculator: ImprisonmentStatusCalculator,
  private val slackApiClient: SlackApiClient,
) {

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/imprisonment-status/{prisonNumber}")
  fun calculateImprisonmentStatus(@PathVariable prisonNumber: String): String = legacyDataService.calculateImprisonmentStatus(prisonNumber)

  /**
   * Runs steps 3 to 5 of the calculation against the given sentencing data, so they can be tested with scenarios that
   * are hard to set up in RaS.
   */
  @PreAuthorize("isAuthenticated()")
  @PostMapping("/imprisonment-status/calculate")
  fun calculateImprisonmentStatus(@RequestBody sentencingData: LegacySentencingData): ImprisonmentStatusCalculation = imprisonmentStatusCalculator.calculate(sentencingData)

  @PreAuthorize("isAuthenticated()")
  @PostMapping("/imprisonment-status/slack-notification")
  fun triggerSlackNotification() = slackApiClient.postToSlackApp(
    prisonNumber = "A1234BC",
    nomisStatus = "NON-CUST",
    dpsStatus = "SEC38",
    dbReference = UUID.randomUUID(),
  )
}
