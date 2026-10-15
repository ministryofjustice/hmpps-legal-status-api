package uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatusMappingEntity
import java.util.UUID

@Repository
interface ImprisonmentStatusMappingRepository : JpaRepository<ImprisonmentStatusMappingEntity, UUID> {
  fun findBySentenceCategoryAndSentenceCalcType(sentenceCategory: String, sentenceCalcType: String): ImprisonmentStatusMappingEntity?

  fun findByOffenceResultCode(offenceResultCode: String): ImprisonmentStatusMappingEntity?
}
