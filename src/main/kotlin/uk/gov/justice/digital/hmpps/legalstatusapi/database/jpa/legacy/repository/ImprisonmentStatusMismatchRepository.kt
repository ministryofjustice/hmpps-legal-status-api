package uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatusMismatchEntity
import java.util.UUID

@Repository
interface ImprisonmentStatusMismatchRepository : JpaRepository<ImprisonmentStatusMismatchEntity, UUID>
