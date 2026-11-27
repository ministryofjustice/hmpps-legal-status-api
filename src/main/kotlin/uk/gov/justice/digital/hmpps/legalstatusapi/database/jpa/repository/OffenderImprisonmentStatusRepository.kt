package uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.entity.OffenderImprisonmentStatusEntity
import java.util.UUID

interface OffenderImprisonmentStatusRepository : JpaRepository<OffenderImprisonmentStatusEntity, UUID> {
  fun getAllByPrisonNumber(prisonNumber: String): List<OffenderImprisonmentStatusEntity>

  @Query("SELECT o FROM OFFENDER_IMPRISONMENT_STATUS o WHERE o.prisonNumber = :prisonNumber ORDER BY o.updatedAt DESC LIMIT 1")
  fun findLatestByPrisonNumber(prisonNumber: String): OffenderImprisonmentStatusEntity?

  fun getAllByPrisonId(prisonId: String): List<OffenderImprisonmentStatusEntity>
}
