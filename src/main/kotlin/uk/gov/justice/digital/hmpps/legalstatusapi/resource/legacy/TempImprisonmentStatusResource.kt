package uk.gov.justice.digital.hmpps.legalstatusapi.resource.legacy

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.LegacyDataService

/**
 * TEMPORARY development endpoint for invoking the imprisonment status calculation on demand.
 *
 * TODO - Delete once calculateImprisonmentStatus is wired into the domain events listener.
 */
@Hidden
@RestController
@RequestMapping("/temp/legacy")
class TempImprisonmentStatusResource(
  private val legacyDataService: LegacyDataService,
) {

  @GetMapping("/imprisonment-status/{prisonNumber}")
  fun calculateImprisonmentStatus(@PathVariable prisonNumber: String): String = legacyDataService.calculateImprisonmentStatus(prisonNumber)
}
