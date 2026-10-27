package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.legalstatusapi.client.manageoffences.ManageOffencesApiClient
import uk.gov.justice.digital.hmpps.manageoffences.resource.model.OffenceSeverityRating

@ExtendWith(MockitoExtension::class)
class ManageOffencesOffenceSeverityProviderTest {

  @InjectMocks
  private lateinit var provider: ManageOffencesOffenceSeverityProvider

  @Mock
  private lateinit var manageOffencesApiClient: ManageOffencesApiClient

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
}
