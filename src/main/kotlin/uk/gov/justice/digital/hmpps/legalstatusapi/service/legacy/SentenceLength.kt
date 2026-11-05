package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonPeriodLength
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Derives a sentence length, in days, from a RaS sentence's period lengths. The length is only used to choose between
 * sentences whose statuses have the same rank (the longest sentence wins), so it only needs to order sentences the same
 * way NOMIS does. It is not a real sentence length or end date.
 *
 * How NOMIS does it: the NOMIS query uses OFFENDER_SENTENCES.END_DATE - START_DATE. RaS gives us neither date, but on
 * preprod (Nov 2026) END_DATE is START_DATE plus all of the sentence's terms (OFFENDER_SENTENCE_TERMS, which are the RaS
 * period lengths) minus 1 day. So the length is the sum of the terms.
 *
 * How this works it out:
 * - All period lengths are summed, whatever their [PersonPeriodLength.periodLengthType]. NOMIS's END_DATE includes every
 *   term, e.g. the licence period of an extended sentence. Filtering to SENTENCE_LENGTH would also drop TERM_LENGTH (DTO, fines, civil), CUSTODIAL_TERM
 *   and TARIFF_LENGTH sentences entirely.
 * - The months are added to a fixed date, [ANCHOR], rather than the sentence start date, which RaS does not give us.
 *   The start date only changes the length through month lengths and leap years. The anchor is 1 January, so Oracle's
 *   ADD_MONTHS end-of-month rule (e.g. 30 Apr + 1 month = 31 May) never applies, and Java's plusMonths gives the same
 *   result.
 * - NOMIS's "minus 1 day" is left out, as it is the same for every sentence and so does not change the order.
 *
 * Returns null when there are no period lengths. NOMIS then has no END_DATE either, and the calculator puts sentences
 * with no length last, as NOMIS does. RaS says DPS-created sentences always have a period length; only NOMIS-created
 * ones may have none.
 *
 * - Catch 22: NOMIS has no END_DATE for most DPS-created or DPS-updated sentences, because the DPS to NOMIS sync does not
 *   send one, and updating a sentence clears it. NOMIS gives those sentences no length and so drops them whenever another sentence of the same rank has one. 
 *   Here they get a length from their period lengths, because RaS cannot tell us whether NOMIS has an END_DATE. That is most of the
 *   remaining difference. It only goes away if the sync sends NOMIS an END_DATE worked out the same way.
 */
object SentenceLength {

  val ANCHOR: LocalDate = LocalDate.of(2000, 1, 1)

  fun inDays(periodLengths: List<PersonPeriodLength>): Long? {
    if (periodLengths.isEmpty()) return null
    val months = periodLengths.sumOf { (it.years ?: 0) * 12L + (it.months ?: 0) }
    val days = periodLengths.sumOf { (it.weeks ?: 0) * 7L + (it.days ?: 0) }
    return ChronoUnit.DAYS.between(ANCHOR, ANCHOR.plusMonths(months).plusDays(days))
  }
}
