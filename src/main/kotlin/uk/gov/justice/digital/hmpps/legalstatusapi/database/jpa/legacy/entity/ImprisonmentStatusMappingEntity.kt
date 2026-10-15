package uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.hibernate.Hibernate

/**
 * Maps either a sentence (category + calc type) or an offence result code to an Imprisonment Status.
 *
 * The records were taken from NOMIS preprod data on 15/10/2026 from the table OMS_OWNER.IMPRISON_STATUS_MAPPINGS
 */
@Entity(name = "IMPRISONMENT_STATUS_MAPPING")
class ImprisonmentStatusMappingEntity(
  @Id
  @Column(name = "ID", nullable = false, updatable = false)
  val id: Int,

  @ManyToOne(optional = false)
  @JoinColumn(name = "IMPRISONMENT_STATUS", nullable = false, updatable = false)
  val imprisonmentStatus: ImprisonmentStatusEntity,

  @Column(name = "SENTENCE_CATEGORY", nullable = true, updatable = false)
  val sentenceCategory: String? = null,

  @Column(name = "SENTENCE_CALC_TYPE", nullable = true, updatable = false)
  val sentenceCalcType: String? = null,

  @Column(name = "OFFENCE_RESULT_CODE", nullable = true, updatable = false)
  val offenceResultCode: String? = null,
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as ImprisonmentStatusMappingEntity

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String = this::class.simpleName + "(id = $id, sentenceCategory = $sentenceCategory, sentenceCalcType = $sentenceCalcType, offenceResultCode = $offenceResultCode)"
}
