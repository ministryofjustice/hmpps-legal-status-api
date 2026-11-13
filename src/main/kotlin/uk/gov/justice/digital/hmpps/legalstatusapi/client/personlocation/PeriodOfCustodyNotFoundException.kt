package uk.gov.justice.digital.hmpps.legalstatusapi.client.personlocation

/**
 * Thrown when the Person Location API has no period of custody for a prisoner. Without a period of custody, we cannot
 * select the date range for the prisoner's charges.
 */
class PeriodOfCustodyNotFoundException(val prisonNumber: String) : RuntimeException("Period of custody for prisoner [$prisonNumber] not returned by Person Location API")
