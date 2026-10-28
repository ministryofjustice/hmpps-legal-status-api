package uk.gov.justice.digital.hmpps.legalstatusapi.resource.legacy

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.legalstatusapi.IntegrationTestBase
import uk.gov.justice.digital.hmpps.legalstatusapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.LegacySentencingData
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.aSentence
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.anOffence
import uk.gov.justice.digital.hmpps.legalstatusapi.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.legalstatusapi.wiremock.HmppsManageOffencesApiExtension.Companion.hmppsManageOffencesApi
import uk.gov.justice.digital.hmpps.legalstatusapi.wiremock.HmppsPrisonerSearchApiExtension.Companion.hmppsPrisonerSearchApi
import uk.gov.justice.digital.hmpps.legalstatusapi.wiremock.HmppsRemandAndSentencingApiExtension.Companion.hmppsRemandAndSentencingApi
import uk.gov.justice.digital.hmpps.manageoffences.resource.model.OffenceSeverityRating
import uk.gov.justice.digital.hmpps.prisonersearch.resource.model.aValidPrisoner
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonCharges
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.aValidChargeOutcome
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.aValidPersonCharge
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.aValidPersonSentence
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.aValidSentenceType
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.aValidSentenceTypeDetails
import java.time.LocalDate
import java.util.UUID

/**
 * Tests steps 3 to 5 of the calculation against the real Imprisonment Status and mapping reference data.
 *
 * TODO - Delete along with TempImprisonmentStatusResource, and move the scenarios to a test of the full calculation.
 */
class TempImprisonmentStatusResourceTest : IntegrationTestBase() {

  @BeforeEach
  fun stubAuth() = hmppsAuth.stubGrantToken()

  @Test
  fun `should calculate status from the sentence given a prisoner with a sentence and a remand charge`() {
    // Given
    hmppsManageOffencesApi.stubGetOffenceSeverities(
      offenceCodes = setOf("CJ88116", "TH68010"),
      response = listOf(OffenceSeverityRating("CJ88116", "50"), OffenceSeverityRating("TH68010", "46")),
    )
    val sentencingData = LegacySentencingData(
      sentences = listOf(aSentence(sentenceCategory = "2003", sentenceCalcType = "ADIMP", offenceCode = "TH68010")),
      offences = listOf(
        anOffence(offenceCode = "TH68010", outcomeCode = "1002"),
        anOffence(offenceCode = "CJ88116", outcomeCode = "4565"),
      ),
    )

    // When
    val response = postCalculation(sentencingData)

    // Then
    response.expectStatus().isOk
      .expectBody()
      .jsonPath("$.imprisonmentStatus").isEqualTo("SENT03")
      .jsonPath("$.mainOffenceCode").isEqualTo("TH68010")
      .jsonPath("$.calculatedFrom").isEqualTo("SENTENCE")
  }

  @Test
  fun `should calculate status from the charge outcome with the lowest ranked status given a prisoner with no sentences`() {
    // Given
    hmppsManageOffencesApi.stubGetOffenceSeverities(
      offenceCodes = setOf("CJ88116", "TH68010"),
      response = listOf(OffenceSeverityRating("CJ88116", "50"), OffenceSeverityRating("TH68010", "46")),
    )
    val sentencingData = LegacySentencingData(
      offences = listOf(
        // 4565 maps to TRL (rank 560), 1501 maps to LR (rank 200)
        anOffence(offenceCode = "TH68010", outcomeCode = "4565"),
        anOffence(offenceCode = "CJ88116", outcomeCode = "1501"),
      ),
    )

    // When
    val response = postCalculation(sentencingData)

    // Then
    response.expectStatus().isOk
      .expectBody()
      .jsonPath("$.imprisonmentStatus").isEqualTo("LR")
      .jsonPath("$.mainOffenceCode").isEqualTo("CJ88116")
      .jsonPath("$.calculatedFrom").isEqualTo("CHARGE_OUTCOME")
  }

  @Test
  fun `should return UNKNOWN given a prisoner with no charges or sentences`() {
    // Given

    // When
    val response = postCalculation(LegacySentencingData())

    // Then
    response.expectStatus().isOk
      .expectBody()
      .jsonPath("$.imprisonmentStatus").isEqualTo("UNKNOWN")
      .jsonPath("$.mainOffenceCode").doesNotExist()
      .jsonPath("$.calculatedFrom").isEqualTo("CHARGE_OUTCOME")
  }

  @Test
  fun `should calculate status from the prisoner's RaS charges and sentences`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    val receptionDate = LocalDate.now().minusMonths(3)
    hmppsPrisonerSearchApi.stubGetPrisoner(prisonNumber, aValidPrisoner(prisonNumber = prisonNumber, receptionDate = receptionDate))
    val sentenceTypeUuid = UUID.randomUUID()
    hmppsRemandAndSentencingApi.stubGetPrisonerCharges(
      prisonNumber,
      fromDate = receptionDate,
      toDate = LocalDate.now(),
      response = PersonCharges(
        charges = listOf(
          aValidPersonCharge(
            offenceCode = "TH68010",
            outcome = aValidChargeOutcome(nomisCode = "1002"),
            sentence = aValidPersonSentence(sentenceType = aValidSentenceType(sentenceTypeUuid = sentenceTypeUuid)),
          ),
          aValidPersonCharge(offenceCode = "CJ88116", outcome = aValidChargeOutcome(nomisCode = "4565"), sentence = null),
        ),
      ),
    )
    hmppsRemandAndSentencingApi.stubGetSentenceTypeDetails(aValidSentenceTypeDetails(sentenceTypeUuid = sentenceTypeUuid, nomisCjaCode = "2003", nomisSentenceCalcType = "ADIMP"))
    hmppsManageOffencesApi.stubGetOffenceSeverities(
      offenceCodes = setOf("CJ88116", "TH68010"),
      response = listOf(OffenceSeverityRating("CJ88116", "50"), OffenceSeverityRating("TH68010", "46")),
    )

    // When
    val response = webTestClient.get().uri("/temp/legacy/imprisonment-status/$prisonNumber")
      .headers(setAuthorisation())
      .exchange()

    // Then
    response.expectStatus().isOk
      .expectBody(String::class.java).isEqualTo("SENT03")
  }

  @Test
  fun `should return unauthorized given no auth token`() {
    webTestClient.post().uri("/temp/legacy/imprisonment-status/calculate")
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(LegacySentencingData())
      .exchange()
      .expectStatus().isUnauthorized
  }

  private fun postCalculation(sentencingData: LegacySentencingData) = webTestClient.post().uri("/temp/legacy/imprisonment-status/calculate")
    .headers(setAuthorisation())
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(sentencingData)
    .exchange()
}
