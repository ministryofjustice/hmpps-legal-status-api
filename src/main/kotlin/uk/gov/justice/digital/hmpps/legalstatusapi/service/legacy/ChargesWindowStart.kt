package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import uk.gov.justice.digital.hmpps.personlocation.resource.model.PeriodOfCustody
import java.time.LocalDate
import java.time.ZoneId

private val UK_ZONE: ZoneId = ZoneId.of("Europe/London")

/**
 * Returns the start of the date range for the prisoner's charges: the `fromDate` to use when we get the charges from
 * RaS. Returns null when RaS must not be given a `fromDate`.
 *
 * The date comes from the Person Location API (the prisoner's latest period of custody), not from RaS.
 *
 * Rule:
 * - If there is a previous period of custody, the `fromDate` is the date on which it concluded, plus 1 day.
 * - If there is no previous period of custody, there is no `fromDate`. RaS then returns the charges from all court
 *   appearances on the prisoner's active court cases.
 *
 * Why the previous period, and not [PeriodOfCustody.commencedAt]: a court case can have hearings before the prisoner
 * comes into custody (for example, bail, then remand at a later hearing). `commencedAt` excludes those hearings.
 *
 * Why plus 1 day: RaS includes appearances on the `fromDate` itself. A hearing on the day that the previous period
 * concluded (for example, a release from court) belongs to the previous period, so we exclude it.
 *
 * Assumption about recalls: Person Location starts a new period of custody when a prisoner is recalled, because the
 * prisoner was not in custody while on licence. After a recall, the previous period is the one that concluded at the
 * release on licence. Thus the original sentencing hearing is before the `fromDate`, and RaS does not return it:
 * - A recall migrated from NOMIS is a court appearance in RaS, inside the date range. RaS returns its charges.
 * - A recall recorded only in DPS has no court appearance. RaS does not return the recalled sentence, and the
 *   calculation can give a mismatch. This is a known limit. The earlier `fromDate` (the Prisoner Search
 *   `receptionDate`) had the same limit.
 *
 * Known limit: if a prisoner is released and then remanded on a new matter on the same day, that hearing is excluded.
 */
fun chargesWindowStart(periodOfCustody: PeriodOfCustody): LocalDate? = periodOfCustody.previousPeriod?.concludedAt
  ?.atZoneSameInstant(UK_ZONE)
  ?.toLocalDate()
  ?.plusDays(1)
