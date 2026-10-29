package uk.gov.justice.digital.hmpps.legalstatusapi.config

import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.cache.support.NoOpCacheManager
import uk.gov.justice.digital.hmpps.legalstatusapi.IntegrationTestBase
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.OffenceSeverityProvider
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.SentenceTypeDetailsProvider
import uk.gov.justice.digital.hmpps.legalstatusapi.wiremock.HmppsManageOffencesApiExtension.Companion.hmppsManageOffencesApi
import uk.gov.justice.digital.hmpps.legalstatusapi.wiremock.HmppsRemandAndSentencingApiExtension.Companion.hmppsRemandAndSentencingApi
import uk.gov.justice.digital.hmpps.manageoffences.resource.model.OffenceSeverityRating
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.aValidSentenceTypeDetails
import java.util.UUID

/**
 * Proves that caching is turned off in the integration test context (`spring.cache.type: none`), so that tests can stub
 * different reference data for the same offences and sentence types.
 */
class CacheConfigurationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var cacheManager: CacheManager

  @Autowired
  private lateinit var offenceSeverityProvider: OffenceSeverityProvider

  @Autowired
  private lateinit var sentenceTypeDetailsProvider: SentenceTypeDetailsProvider

  @BeforeEach
  fun stubAuth() = stubGetTokenFromHmppsAuth()

  @Test
  fun `should not cache in the integration test context`() {
    assertThat(cacheManager).isInstanceOf(NoOpCacheManager::class.java)
  }

  @Test
  fun `should get offence severities from Manage Offences every time`() {
    // Given
    stubGetOffenceSeverities(offenceCodes = setOf("TH68010"), response = listOf(OffenceSeverityRating("TH68010", "46")))

    // When
    offenceSeverityProvider.getSeverities(listOf("TH68010"))
    offenceSeverityProvider.getSeverities(listOf("TH68010"))

    // Then
    hmppsManageOffencesApi.verify(2, getRequestedFor(urlPathEqualTo("/offences/severity-ratings")))
  }

  @Test
  fun `should get sentence type details from RaS every time`() {
    // Given
    val sentenceTypeUuid = UUID.randomUUID()
    stubGetSentenceTypeDetails(aValidSentenceTypeDetails(sentenceTypeUuid = sentenceTypeUuid))

    // When
    sentenceTypeDetailsProvider.getSentenceTypeDetails(sentenceTypeUuid)
    sentenceTypeDetailsProvider.getSentenceTypeDetails(sentenceTypeUuid)

    // Then
    hmppsRemandAndSentencingApi.verify(2, getRequestedFor(urlPathEqualTo("/sentence-type/$sentenceTypeUuid/details")))
  }
}
