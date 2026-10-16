package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import org.springframework.stereotype.Service

/**
 * Service class for handling NOMIS legacy data operations.
 */
@Service
class LegacyDataService {

  /**
   * Calculates the NOMIS legacy Imprisonment Status for the prisoner identified by their prison number.
   *
   * @param prisonNumber The prison number of the prisoner.
   * @return The calculated Imprisonment Status.
   */
  fun calculateImprisonmentStatus(prisonNumber: String): String {
    TODO("Implement logic to calculate imprisonment status based on NOMIS legacy data for the given prison number.")

    /*
    Steps to implement:
    1. Retrieve the prisoner's receptionDate (which we assume at this point to be the booking start date)
    2. Get the prisoner's sentence(s) and offence(s) from RaS, using the prisonNumber to identify the prisoner along with a date range comprised of receptionDate to current date
    3. Find the correct sentenceCalcType & sentenceCategory from the list of sentences
      1. If there are no sentences, select the correct offenceType from the list of offences
    4. Use the sentenceCalcType and sentenceCategory, or offenceType, to lookup the correct Imprisonment Status Mapping
    5. Return the Imprisonment Status from the Imprisonment Status Mapping record
     */
  }
}
