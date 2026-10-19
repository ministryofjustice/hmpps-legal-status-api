package uk.gov.justice.digital.hmpps.legalstatusapi.uk.gov.justice.digital.hmpps.prisonersearch.resource.model

import uk.gov.justice.digital.hmpps.legalstatusapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.prisonersearch.resource.model.Prisoner
import java.time.LocalDate

fun aValidPrisoner(
  prisonNumber: String = randomValidPrisonNumber(),
  receptionDate: LocalDate? = LocalDate.now().minusMonths(6),
  imprisonmentStatus: String? = "AFIXED",
): Prisoner = Prisoner(
  prisonerNumber = prisonNumber,
  receptionDate = receptionDate,
  imprisonmentStatus = imprisonmentStatus,
)
