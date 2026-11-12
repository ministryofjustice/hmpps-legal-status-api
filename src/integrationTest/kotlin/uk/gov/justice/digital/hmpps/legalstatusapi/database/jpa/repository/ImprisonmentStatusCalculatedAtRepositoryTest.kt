package uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.repository

import jakarta.transaction.Transactional
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.legalstatusapi.IntegrationTestBase
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.entity.anImprisonmentStatusCalculatedAtEntity
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.entity.assertThat
import uk.gov.justice.digital.hmpps.legalstatusapi.randomValidPrisonNumber
import java.time.Instant

@ActiveProfiles("integration-test", "ticking-clock")
class ImprisonmentStatusCalculatedAtRepositoryTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: ImprisonmentStatusCalculatedAtRepository

  @Transactional
  @Test
  fun `should create an ImprisonmentStatusCalculatedAt record with the current timestamp`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    val imprisonmentStatusCalculatedAt = anImprisonmentStatusCalculatedAtEntity(prisonNumber)

    val now = Instant.now(clock)
    val expectedCalculatedAt = now.plusMillis(1) // Expect 'calculated at' to be now+1ms because the test is using the ticking-clock context

    // When
    repository.saveAndFlush(imprisonmentStatusCalculatedAt)

    // Then
    val persistedEntity = repository.getReferenceById(prisonNumber)
    assertThat(persistedEntity)
      .isForPrisonNumber(prisonNumber)
      .wasCalculatedAt(expectedCalculatedAt)
  }
}
