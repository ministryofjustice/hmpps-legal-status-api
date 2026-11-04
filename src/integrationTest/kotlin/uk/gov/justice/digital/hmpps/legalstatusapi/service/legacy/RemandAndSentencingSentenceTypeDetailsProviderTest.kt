package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import uk.gov.justice.digital.hmpps.legalstatusapi.client.remandandsentencing.RemandAndSentencingApiClient
import uk.gov.justice.digital.hmpps.legalstatusapi.client.remandandsentencing.RemandAndSentencingApiException
import uk.gov.justice.digital.hmpps.legalstatusapi.config.CacheConfiguration.Companion.SENTENCE_TYPE_DETAILS
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.aValidSentenceTypeDetails
import java.util.UUID

/**
 * Uses a minimal Spring context, as the caching is applied by a Spring proxy.
 */
@SpringJUnitConfig(RemandAndSentencingSentenceTypeDetailsProviderTest.CachingConfiguration::class)
class RemandAndSentencingSentenceTypeDetailsProviderTest {

  @Configuration
  @EnableCaching
  class CachingConfiguration {
    @Bean
    fun cacheManager(): CacheManager = ConcurrentMapCacheManager(SENTENCE_TYPE_DETAILS)

    @Bean
    fun remandAndSentencingApiClient(): RemandAndSentencingApiClient = mock()

    @Bean
    fun sentenceTypeDetailsProvider(remandAndSentencingApiClient: RemandAndSentencingApiClient): SentenceTypeDetailsProvider = RemandAndSentencingSentenceTypeDetailsProvider(remandAndSentencingApiClient)
  }

  @Autowired
  private lateinit var sentenceTypeDetailsProvider: SentenceTypeDetailsProvider

  @Autowired
  private lateinit var remandAndSentencingApiClient: RemandAndSentencingApiClient

  @Autowired
  private lateinit var cacheManager: CacheManager

  @AfterEach
  fun tearDown() {
    cacheManager.getCache(SENTENCE_TYPE_DETAILS)!!.clear()
    reset(remandAndSentencingApiClient)
  }

  @Test
  fun `should only get the sentence type details from RaS once given the same sentence type`() {
    // Given
    val sentenceTypeUuid = UUID.randomUUID()
    val sentenceTypeDetails = aValidSentenceTypeDetails(sentenceTypeUuid = sentenceTypeUuid)
    given(remandAndSentencingApiClient.getSentenceTypeDetails(any())).willReturn(sentenceTypeDetails)

    // When
    val first = sentenceTypeDetailsProvider.getSentenceTypeDetails(sentenceTypeUuid)
    val second = sentenceTypeDetailsProvider.getSentenceTypeDetails(sentenceTypeUuid)

    // Then
    assertThat(first).isEqualTo(sentenceTypeDetails)
    assertThat(second).isEqualTo(sentenceTypeDetails)
    verify(remandAndSentencingApiClient, times(1)).getSentenceTypeDetails(sentenceTypeUuid)
  }

  @Test
  fun `should get the sentence type details from RaS for each different sentence type`() {
    // Given
    val firstSentenceTypeUuid = UUID.randomUUID()
    val secondSentenceTypeUuid = UUID.randomUUID()
    given(remandAndSentencingApiClient.getSentenceTypeDetails(firstSentenceTypeUuid)).willReturn(aValidSentenceTypeDetails(sentenceTypeUuid = firstSentenceTypeUuid, nomisSentenceCalcType = "ADIMP"))
    given(remandAndSentencingApiClient.getSentenceTypeDetails(secondSentenceTypeUuid)).willReturn(aValidSentenceTypeDetails(sentenceTypeUuid = secondSentenceTypeUuid, nomisSentenceCalcType = "LIFE"))

    // When
    val first = sentenceTypeDetailsProvider.getSentenceTypeDetails(firstSentenceTypeUuid)
    val second = sentenceTypeDetailsProvider.getSentenceTypeDetails(secondSentenceTypeUuid)

    // Then
    assertThat(first.nomisSentenceCalcType).isEqualTo("ADIMP")
    assertThat(second.nomisSentenceCalcType).isEqualTo("LIFE")
    verify(remandAndSentencingApiClient).getSentenceTypeDetails(firstSentenceTypeUuid)
    verify(remandAndSentencingApiClient).getSentenceTypeDetails(secondSentenceTypeUuid)
  }

  @Test
  fun `should not cache anything given RaS returns an error`() {
    // Given
    val sentenceTypeUuid = UUID.randomUUID()
    val exception = RemandAndSentencingApiException("Failed to retrieve sentence type details for sentence type ID: $sentenceTypeUuid", RuntimeException())
    given(remandAndSentencingApiClient.getSentenceTypeDetails(any()))
      .willThrow(exception)
      .willReturn(aValidSentenceTypeDetails(sentenceTypeUuid = sentenceTypeUuid))

    // When
    val error = catchThrowableOfType(RemandAndSentencingApiException::class.java) { sentenceTypeDetailsProvider.getSentenceTypeDetails(sentenceTypeUuid) }
    val actual = sentenceTypeDetailsProvider.getSentenceTypeDetails(sentenceTypeUuid)

    // Then
    assertThat(error).isSameAs(exception)
    assertThat(actual.sentenceTypeUuid).isEqualTo(sentenceTypeUuid)
    verify(remandAndSentencingApiClient, times(2)).getSentenceTypeDetails(sentenceTypeUuid)
  }
}
