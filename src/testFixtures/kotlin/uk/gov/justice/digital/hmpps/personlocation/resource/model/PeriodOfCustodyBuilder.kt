package uk.gov.justice.digital.hmpps.personlocation.resource.model

import java.time.LocalDateTime

fun aValidPeriodOfCustody(
  commencedAt: LocalDateTime = LocalDateTime.parse("2026-03-01T10:15:00"),
  status: CodedDescription = aValidCodedDescription(),
  concludedAt: LocalDateTime? = null, // null means the period of custody is current
  previousPeriod: PreviousPeriod? = null,
): PeriodOfCustody = PeriodOfCustody(
  commencedAt = commencedAt,
  status = status,
  concludedAt = concludedAt,
  previousPeriod = previousPeriod,
)

// The Person Location API spec does not list the status codes, so these values are examples only
fun aValidCodedDescription(
  code: String = "ACTIVE",
  description: String = "Active",
): CodedDescription = CodedDescription(
  code = code,
  description = description,
)

fun aValidPreviousPeriod(
  concludedAt: LocalDateTime = LocalDateTime.parse("2025-01-20T09:00:00"),
): PreviousPeriod = PreviousPeriod(
  concludedAt = concludedAt,
)
