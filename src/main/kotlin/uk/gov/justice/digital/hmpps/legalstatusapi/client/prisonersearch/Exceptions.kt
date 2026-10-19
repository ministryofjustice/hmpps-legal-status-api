package uk.gov.justice.digital.hmpps.legalstatusapi.client.prisonersearch

class PrisonerSearchApiException(message: String, throwable: Throwable) : RuntimeException(message, throwable)

/**
 * Thrown when a specific prisoner is not returned by Prisoner Search API
 */
class PrisonerNotFoundException(val prisonNumber: String) : RuntimeException("Prisoner [$prisonNumber] not returned by Prisoner Search API")
