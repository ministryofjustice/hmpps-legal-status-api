package uk.gov.justice.digital.hmpps.remandandsentencing.resource.model

import uk.gov.justice.digital.hmpps.legalstatusapi.aValidReference
import java.util.UUID

fun aValidPersonPeriodLength(
  years: Int? = null,
  months: Int? = null,
  weeks: Int? = null,
  days: Int? = null,
  periodLengthType: PersonPeriodLength.PeriodLengthType = PersonPeriodLength.PeriodLengthType.SENTENCE_LENGTH,
  periodOrder: String = "years,months,weeks,days",
  periodLengthUuid: UUID = aValidReference(),
  legacyData: PeriodLengthLegacyData? = null,
): PersonPeriodLength = PersonPeriodLength(
  years = years,
  months = months,
  weeks = weeks,
  days = days,
  periodLengthType = periodLengthType,
  periodOrder = periodOrder,
  periodLengthUuid = periodLengthUuid,
  legacyData = legacyData,
)
