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
import java.util.UUID

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
      "IS-121",
      "IS-124",
      "IS-127",
      "IS-128",
      "IS-134",
      "IS-136",
      "IS-137",
      "IS-142",
      "IS-144",
      "IS-147",
      "IS-149",
      "IS-151",
    ],
  )
  fun `should calculate Imprisonment Status`(jiraReference: String) {
    // Given
    val scenarioFile = this::class.java.getResourceAsStream("$jiraReference.json")
    val scenario = objectMapper.readValue(scenarioFile, Scenario::class.java)

    scenario.rasData.sentenceTypeDetails.values.forEach { stubGetSentenceTypeDetails(it) }

    val scenarioOffenceCodes = scenario.rasData.personCharges.charges.map { it.offenceCode }.toSet()
    stubGetOffenceSeverities(
      offenceCodes = scenarioOffenceCodes,
      response = scenario.offenceSeverities.map { (offenceCode, severityRating) -> OffenceSeverityRating(offenceCode, severityRating.toString()) },
    )

    val prisonNumber = randomValidPrisonNumber()
    val prisoner = aValidPrisoner(prisonNumber, receptionDate = scenario.receptionDate)
    stubGetPrisonerFromPrisonerSearchApi(prisoner)

    stubGetChargesFromRemandAndSentencingApi(
      prisonNumber = prisonNumber,
      fromDate = scenario.receptionDate,
      toDate = today,
      response = scenario.rasData.personCharges,
    )

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
    val rasData: RasData,
    val offenceSeverities: Map<String, Int>,
  ) {
    override fun toString() = jiraReference
  }

  data class RasData(
    val personCharges: PersonCharges,
    val sentenceTypeDetails: Map<UUID, SentenceTypeDetails>,
  )
}
