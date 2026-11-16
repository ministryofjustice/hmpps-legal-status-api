package uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.entity

import uk.gov.justice.digital.hmpps.legalstatusapi.randomValidPrisonNumber
import java.time.Instant

fun anImprisonmentStatusCalculatedAtEntity(
  prisonNumber: String = randomValidPrisonNumber(),
  calculatedAt: Instant = Instant.now(),
): ImprisonmentStatusCalculatedAtEntity = ImprisonmentStatusCalculatedAtEntity(
  prisonNumber = prisonNumber,
  calculatedAt = calculatedAt,
)
