package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.cache.support.NoOpCacheManager
import uk.gov.justice.digital.hmpps.legalstatusapi.client.manageoffences.ManageOffencesApiClient
import uk.gov.justice.digital.hmpps.legalstatusapi.client.manageoffences.ManageOffencesApiException
import uk.gov.justice.digital.hmpps.legalstatusapi.config.CacheConfiguration.Companion.OFFENCE_SEVERITIES
import uk.gov.justice.digital.hmpps.manageoffences.resource.model.OffenceSeverityRating

@ExtendWith(MockitoExtension::class)
class ManageOffencesOffenceSeverityProviderTest {

  @Mock
  private lateinit var manageOffencesApiClient: ManageOffencesApiClient

  // No caching, so that each test (other than the caching tests below) gets the severities from Manage Offences
  private lateinit var provider: ManageOffencesOffenceSeverityProvider

  @BeforeEach
  fun setUp() {
    provider = ManageOffencesOffenceSeverityProvider(manageOffencesApiClient, NoOpCacheManager())
  }

  @Test
  fun `should get severities for the distinct offence codes in sorted order`() {
    // Given
    given(manageOffencesApiClient.getOffenceSeverities(any<Set<String>>())).willReturn(
      listOf(OffenceSeverityRating("AB12345", "20"), OffenceSeverityRating("TH68010", "46")),
    )

    // When
    val actual = provider.getSeverities(listOf("TH68010", "AB12345", "TH68010"))

    // Then
    assertThat(actual).isEqualTo(mapOf("AB12345" to 20, "TH68010" to 46))
    verify(manageOffencesApiClient).getOffenceSeverities(setOf("AB12345", "TH68010"))
  }

  @Test
  fun `should not include offences with no severity or a non-numeric severity`() {
    // Given
    given(manageOffencesApiClient.getOffenceSeverities(any<Set<String>>())).willReturn(
      listOf(OffenceSeverityRating("AB12345", "not a number"), OffenceSeverityRating("TH68010", "46")),
    )

    // When
    val actual = provider.getSeverities(listOf("AB12345", "SX56025-029N", "TH68010"))

    // Then
    assertThat(actual).isEqualTo(mapOf("TH68010" to 46))
  }

  @Test
  fun `should not call Manage Offences given no offence codes`() {
    // Given

    // When
    val actual = provider.getSeverities(emptyList())

    // Then
    assertThat(actual).isEmpty()
    verifyNoInteractions(manageOffencesApiClient)
  }

  @Nested
  inner class Caching {

    private lateinit var cachingProvider: ManageOffencesOffenceSeverityProvider

    @BeforeEach
    fun setUp() {
      cachingProvider = ManageOffencesOffenceSeverityProvider(manageOffencesApiClient, ConcurrentMapCacheManager(OFFENCE_SEVERITIES))
    }

    @Test
    fun `should not call Manage Offences again given the same offence codes`() {
      // Given
      given(manageOffencesApiClient.getOffenceSeverities(any<Set<String>>())).willReturn(
        listOf(OffenceSeverityRating("AB12345", "20"), OffenceSeverityRating("TH68010", "46")),
      )

      // When
      val first = cachingProvider.getSeverities(listOf("TH68010", "AB12345"))
      val second = cachingProvider.getSeverities(listOf("TH68010", "AB12345"))

      // Then
      assertThat(first).isEqualTo(mapOf("AB12345" to 20, "TH68010" to 46))
      assertThat(second).isEqualTo(first)
      verify(manageOffencesApiClient, times(1)).getOffenceSeverities(any<Set<String>>())
    }

    @Test
    fun `should only request the offence codes that are not cached`() {
      // Given
      given(manageOffencesApiClient.getOffenceSeverities(setOf("TH68010"))).willReturn(listOf(OffenceSeverityRating("TH68010", "46")))
      given(manageOffencesApiClient.getOffenceSeverities(setOf("CJ88116"))).willReturn(listOf(OffenceSeverityRating("CJ88116", "50")))
      cachingProvider.getSeverities(listOf("TH68010"))

      // When
      val actual = cachingProvider.getSeverities(listOf("TH68010", "CJ88116"))

      // Then
      assertThat(actual).isEqualTo(mapOf("TH68010" to 46, "CJ88116" to 50))
      verify(manageOffencesApiClient).getOffenceSeverities(setOf("TH68010"))
      verify(manageOffencesApiClient).getOffenceSeverities(setOf("CJ88116"))
    }

    @Test
    fun `should use the cache given a different list containing the same offence codes`() {
      // Given
      given(manageOffencesApiClient.getOffenceSeverities(any<Set<String>>())).willReturn(
        listOf(OffenceSeverityRating("AB12345", "20"), OffenceSeverityRating("TH68010", "46")),
      )
      cachingProvider.getSeverities(listOf("AB12345", "TH68010"))

      // When
      val actual = cachingProvider.getSeverities(listOf("TH68010", "AB12345", "AB12345"))

      // Then
      assertThat(actual).isEqualTo(mapOf("AB12345" to 20, "TH68010" to 46))
      verify(manageOffencesApiClient, times(1)).getOffenceSeverities(any<Set<String>>())
    }

    @Test
    fun `should cache that Manage Offences has no severity for an offence`() {
      // Given
      given(manageOffencesApiClient.getOffenceSeverities(any<Set<String>>())).willReturn(emptyList())

      // When
      val first = cachingProvider.getSeverities(listOf("SX56025-029N"))
      val second = cachingProvider.getSeverities(listOf("SX56025-029N"))

      // Then
      assertThat(first).isEmpty()
      assertThat(second).isEmpty()
      verify(manageOffencesApiClient, times(1)).getOffenceSeverities(setOf("SX56025-029N"))
    }

    @Test
    fun `should cache a non-numeric severity as no severity`() {
      // Given
      given(manageOffencesApiClient.getOffenceSeverities(any<Set<String>>())).willReturn(listOf(OffenceSeverityRating("AB12345", "not a number")))

      // When
      val first = cachingProvider.getSeverities(listOf("AB12345"))
      val second = cachingProvider.getSeverities(listOf("AB12345"))

      // Then
      assertThat(first).isEmpty()
      assertThat(second).isEmpty()
      verify(manageOffencesApiClient, times(1)).getOffenceSeverities(setOf("AB12345"))
    }

    @Test
    fun `should not cache anything given Manage Offences returns an error`() {
      // Given
      val exception = ManageOffencesApiException("Error retrieving offence severities for offenceCodes TH68010", RuntimeException())
      given(manageOffencesApiClient.getOffenceSeverities(any<Set<String>>()))
        .willThrow(exception)
        .willReturn(listOf(OffenceSeverityRating("TH68010", "46")))

      // When
      val error = catchThrowableOfType(ManageOffencesApiException::class.java) { cachingProvider.getSeverities(listOf("TH68010")) }
      val actual = cachingProvider.getSeverities(listOf("TH68010"))

      // Then
      assertThat(error).isSameAs(exception)
      assertThat(actual).isEqualTo(mapOf("TH68010" to 46))
      verify(manageOffencesApiClient, times(2)).getOffenceSeverities(setOf("TH68010"))
    }
  }
}
