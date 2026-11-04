package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

fun anOffence(
  offenceCode: String = "TH68010",
  outcomeCode: String? = "1002",
): Offence = Offence(
  offenceCode = offenceCode,
  outcomeCode = outcomeCode,
)
