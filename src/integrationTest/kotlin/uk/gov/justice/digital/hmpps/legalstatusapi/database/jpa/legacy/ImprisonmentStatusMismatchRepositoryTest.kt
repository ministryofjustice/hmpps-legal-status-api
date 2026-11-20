package uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import uk.gov.justice.digital.hmpps.legalstatusapi.IntegrationTestBase
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatusEntity
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatusMismatchEntity
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.repository.ImprisonmentStatusMismatchRepository
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.CalculatedFrom
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.NotCalculatedReason
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Temporary test class for ImprisonmentStatusMismatchRepository whilst we do not have any other tests that test the
 * repository and entity implicitly.
 *
 * TODO - remove this test class once we have other tests that cover the repository and entity implicitly.
 */
class ImprisonmentStatusMismatchRepositoryTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: ImprisonmentStatusMismatchRepository

  @Autowired
  private lateinit var imprisonmentStatusRepository: ImprisonmentStatusRepository

  @AfterEach
  fun tearDown() {
    repository.deleteAll()
  }

  @Test
  fun `should save and retrieve an Imprisonment Status mismatch`() {
    // Given
    val mismatch = aValidImprisonmentStatusMismatch()

    // When
    val saved = repository.save(mismatch)
    val actual = repository.findById(saved.id!!).get()

    // Then
    with(actual) {
      assertThat(prisonNumber).isEqualTo("A1234BC")
      assertThat(bookingId).isEqualTo("1234567")
      assertThat(receptionDate).isEqualTo(LocalDate.of(2026, 9, 1))
      assertThat(previousPeriodConcludedAt).isEqualTo(LocalDateTime.parse("2025-01-20T09:00:00"))
      assertThat(chargesWindowStart).isEqualTo(LocalDate.of(2025, 1, 21))
      assertThat(nomisImprisonmentStatus).isEqualTo("SENT03")
      assertThat(nomisCalculatedAt).isEqualTo(Instant.parse("2026-10-26T09:00:00Z"))
      assertThat(dpsImprisonmentStatus?.id).isEqualTo("RECEP_REM")
      assertThat(notCalculatedReason).isNull()
      assertThat(dpsMainOffenceCode).isEqualTo("TH68010")
      assertThat(dpsCalculatedFrom).isEqualTo(CalculatedFrom.CHARGE_OUTCOME)
      assertThat(dpsCalculatedAt).isEqualTo(Instant.parse("2026-10-26T09:00:05Z"))
      assertThat(createdAt).isEqualTo(Instant.parse("2026-10-26T09:00:06Z"))
      assertThat(offenceSeverities).isEqualTo(mapOf("TH68010" to 3, "SX56025" to 1))
      // jsonb normalises whitespace and key order, so compare the JSON trees
      assertThat(objectMapper.readTree(rasData)).isEqualTo(objectMapper.readTree(RAS_DATA))
      assertThat(objectMapper.readTree(nomisData)).isEqualTo(objectMapper.readTree(NOMIS_DATA))
    }
  }

  @Test
  fun `should generate an ID when saving`() {
    // Given
    val mismatch = aValidImprisonmentStatusMismatch()

    // When
    val saved = repository.save(mismatch)

    // Then
    assertThat(saved.id).isNotNull()
    assertThat(repository.existsById(saved.id!!)).isTrue()
  }

  @Test
  fun `should save an Imprisonment Status mismatch without the optional fields`() {
    // Given
    val mismatch = aValidImprisonmentStatusMismatch(
      bookingId = null,
      receptionDate = null,
      previousPeriodConcludedAt = null,
      chargesWindowStart = null,
      nomisCalculatedAt = null,
      dpsMainOffenceCode = null,
    )

    // When
    val saved = repository.save(mismatch)
    val actual = repository.findById(saved.id!!).get()

    // Then
    with(actual) {
      assertThat(bookingId).isNull()
      assertThat(receptionDate).isNull()
      assertThat(previousPeriodConcludedAt).isNull()
      assertThat(chargesWindowStart).isNull()
      assertThat(nomisCalculatedAt).isNull()
      assertThat(dpsMainOffenceCode).isNull()
    }
  }

  @Test
  fun `should save a NOMIS Imprisonment Status that is not in the IMPRISONMENT_STATUS table`() {
    // Given
    val inactiveNomisStatus = "UNK_SENT"
    val mismatch = aValidImprisonmentStatusMismatch(nomisImprisonmentStatus = inactiveNomisStatus)

    // When
    val saved = repository.save(mismatch)
    val actual = repository.findById(saved.id!!).get()

    // Then
    assertThat(actual.nomisImprisonmentStatus).isEqualTo(inactiveNomisStatus)
  }

  @Test
  fun `should save and retrieve a row flagged as not calculated`() {
    // Given
    val notCalculated = aValidImprisonmentStatusMismatch(
      dpsImprisonmentStatus = null,
      dpsMainOffenceCode = null,
      dpsCalculatedFrom = null,
      notCalculatedReason = NotCalculatedReason.NO_RAS_CHARGES,
    )

    // When
    val saved = repository.save(notCalculated)
    val actual = repository.findById(saved.id!!).get()

    // Then
    with(actual) {
      assertThat(notCalculatedReason).isEqualTo(NotCalculatedReason.NO_RAS_CHARGES)
      assertThat(dpsImprisonmentStatus).isNull()
      assertThat(dpsMainOffenceCode).isNull()
      assertThat(dpsCalculatedFrom).isNull()
      assertThat(nomisImprisonmentStatus).isEqualTo("SENT03")
    }
  }

  @Test
  fun `should not save a row with a not calculated reason and a DPS Imprisonment Status`() {
    // Given
    val invalid = aValidImprisonmentStatusMismatch(notCalculatedReason = NotCalculatedReason.NO_RAS_CHARGES)

    // When / Then
    assertThatThrownBy { repository.saveAndFlush(invalid) }.isInstanceOf(DataIntegrityViolationException::class.java)
  }

  @Test
  fun `should not save a row with no not calculated reason and no DPS Imprisonment Status`() {
    // Given
    val invalid = aValidImprisonmentStatusMismatch(dpsImprisonmentStatus = null, dpsCalculatedFrom = null)

    // When / Then
    assertThatThrownBy { repository.saveAndFlush(invalid) }.isInstanceOf(DataIntegrityViolationException::class.java)
  }

  private fun aValidImprisonmentStatusMismatch(
    nomisImprisonmentStatus: String = "SENT03",
    bookingId: String? = "1234567",
    receptionDate: LocalDate? = LocalDate.of(2026, 9, 1),
    previousPeriodConcludedAt: LocalDateTime? = LocalDateTime.parse("2025-01-20T09:00:00"),
    chargesWindowStart: LocalDate? = LocalDate.of(2025, 1, 21),
    nomisCalculatedAt: Instant? = Instant.parse("2026-10-26T09:00:00Z"),
    dpsMainOffenceCode: String? = "TH68010",
    dpsImprisonmentStatus: ImprisonmentStatusEntity? = imprisonmentStatusRepository.findById("RECEP_REM").get(),
    dpsCalculatedFrom: CalculatedFrom? = CalculatedFrom.CHARGE_OUTCOME,
    notCalculatedReason: NotCalculatedReason? = null,
  ) = ImprisonmentStatusMismatchEntity(
    prisonNumber = "A1234BC",
    bookingId = bookingId,
    receptionDate = receptionDate,
    previousPeriodConcludedAt = previousPeriodConcludedAt,
    chargesWindowStart = chargesWindowStart,
    nomisImprisonmentStatus = nomisImprisonmentStatus,
    nomisCalculatedAt = nomisCalculatedAt,
    dpsImprisonmentStatus = dpsImprisonmentStatus,
    dpsMainOffenceCode = dpsMainOffenceCode,
    dpsCalculatedFrom = dpsCalculatedFrom,
    dpsCalculatedAt = Instant.parse("2026-10-26T09:00:05Z"),
    notCalculatedReason = notCalculatedReason,
    rasData = RAS_DATA,
    offenceSeverities = mapOf("TH68010" to 3, "SX56025" to 1),
    nomisData = NOMIS_DATA,
    createdAt = Instant.parse("2026-10-26T09:00:06Z"),
  )

  private companion object {
    const val RAS_DATA = """
      [
        {
          "chargeUuid": "8f0c3d0e-4b1a-4d7e-9a5e-2f6b8c1d3e4f",
          "offenceCode": "TH68010",
          "aggravatingFactors": [],
          "outcome": { "nomisCode": "4565", "outcomeName": "Commit to Crown Court for Trial" },
          "legacyData": null,
          "sentence": null
        }
      ]
    """

    const val NOMIS_DATA = """
      {
        "bookingId": 1234567,
        "sentences": [],
        "charges": [
          { "offenceCode": "TH68010", "statuteCode": "TH68", "resultCode1": "4565" }
        ]
      }
    """
  }
}
