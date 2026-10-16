package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.given
import uk.gov.justice.digital.hmpps.legalstatusapi.client.prisonersearch.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.prisonersearch.resource.model.Prisoner
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class LegacyDataServiceTest {

  @InjectMocks
  private lateinit var legacyDataService: LegacyDataService;

  @Mock
  private lateinit var prisonerSearchApiClient: PrisonerSearchApiClient

  @Nested
  @DisplayName("Given a prisoner with a reception date")
  inner class GivenPrisonerReceivedBeforePesContract {
    private lateinit var prisoner: Prisoner
    private lateinit var receptionDate: LocalDate

    @BeforeEach
    internal fun setUp() {
      val prisonNumber = "123456"
      val receptionDate = LocalDate.now()
      val prisoner = Prisoner(
        prisonerNumber = prisonNumber,
        receptionDate = receptionDate
      )
      given(prisonerSearchApiClient.getPrisoner(prisonNumber)).willReturn(prisoner)
    }

    @Test
    fun `should not be implemented`() {
      assertThrows<NotImplementedError>("Implement logic to calculate imprisonment status based on NOMIS legacy data for the given prison number.") {
        legacyDataService.calculateImprisonmentStatus("123456")
      }
    }

  }
}