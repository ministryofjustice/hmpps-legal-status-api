package uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatusEntity

@Repository
interface ImprisonmentStatusRepository : JpaRepository<ImprisonmentStatusEntity, String>
