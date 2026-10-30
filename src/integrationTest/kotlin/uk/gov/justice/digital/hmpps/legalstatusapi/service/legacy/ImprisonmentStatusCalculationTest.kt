package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.legalstatusapi.IntegrationTestBase
import uk.gov.justice.digital.hmpps.legalstatusapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.manageoffences.resource.model.OffenceSeverityRating
import uk.gov.justice.digital.hmpps.prisonersearch.resource.model.aValidPrisoner
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonCharges
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.SentenceTypeDetails
import java.time.LocalDate

/**
 * Integration tests with scenarios taken from production to prove the [LegacyDataService]
 *
 * This test is specifically for Jira epic IS-64, which is about proving this service can calculate the same
 * Imprisonment Status as NOMIS.
 * For each mismatched calculation, a subtask will be created under IS-87 which will be used to write a new test
 * scenario and to implement code to make the test pass.
 */
class ImprisonmentStatusCalculationTest : IntegrationTestBase() {
  @Autowired
  private lateinit var legacyDataService: LegacyDataService

  val today: LocalDate = LocalDate.now()

  @ParameterizedTest
  @CsvSource(
    value = [
      // each record is a Jira ticket number, with the corresponding json data file in resources/uk/gov/justice/digital/hmpps/legalstatusapi/service/legacy
      "IS-00-example",
      "IS-01-example",
      "IS-120",
    ],
  )
  fun `should calculate Imprisonment Status`(jiraReference: String) {
    // Given
    val scenarioFile = this::class.java.getResourceAsStream("$jiraReference.json")
    val scenario = objectMapper.readValue(scenarioFile, Scenario::class.java)

    scenario.rasSentenceTypeDetails.forEach { stubGetSentenceTypeDetails(it) }

    val scenarioOffenceCodes = scenario.rasChargeData.charges.map { it.offenceCode }.toSet()
    stubGetOffenceSeverities(scenarioOffenceCodes, scenario.offenceSeverityRatings)

    val prisonNumber = randomValidPrisonNumber()
    val prisoner = aValidPrisoner(prisonNumber, receptionDate = scenario.receptionDate)
    stubGetPrisonerFromPrisonerSearchApi(prisoner)

    stubGetChargesFromRemandAndSentencingApi(prisonNumber, fromDate = scenario.receptionDate, toDate = today, scenario.rasChargeData)

    stubGetTokenFromHmppsAuth()

    // When
    val actual = legacyDataService.calculateImprisonmentStatus(prisonNumber)

    // Then
    assertThat(actual).isEqualTo(scenario.expectedStatus)
  }

  data class Scenario(
    val jiraReference: String,
    val receptionDate: LocalDate,
    val expectedStatus: String,
    val rasChargeData: PersonCharges,
    val rasSentenceTypeDetails: List<SentenceTypeDetails>,
    val offenceSeverityRatings: List<OffenceSeverityRating>,
  ) {
    override fun toString() = jiraReference
  }
}
