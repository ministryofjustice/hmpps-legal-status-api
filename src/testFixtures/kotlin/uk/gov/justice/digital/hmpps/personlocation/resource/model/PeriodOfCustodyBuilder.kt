package uk.gov.justice.digital.hmpps.personlocation.resource.model

import java.time.OffsetDateTime

fun aValidPeriodOfCustody(
  commencedAt: OffsetDateTime = OffsetDateTime.parse("2026-03-01T10:15:00Z"),
  status: CodedDescription = aValidCodedDescription(),
  concludedAt: OffsetDateTime? = null, // null means the period of custody is current
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
  concludedAt: OffsetDateTime = OffsetDateTime.parse("2025-01-20T09:00:00Z"),
): PreviousPeriod = PreviousPeriod(
  concludedAt = concludedAt,
)
