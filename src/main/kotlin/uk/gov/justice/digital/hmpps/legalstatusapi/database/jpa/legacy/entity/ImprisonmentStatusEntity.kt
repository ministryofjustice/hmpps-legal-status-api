package uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import org.hibernate.Hibernate

@Entity(name = "IMPRISONMENT_STATUS")
class ImprisonmentStatusEntity(
  @Id
  @Column(name = "IMPRISONMENT_STATUS", nullable = false, updatable = false)
  val id: String,

  @Column(name = "DESCRIPTION", nullable = false, updatable = false)
  val description: String,

  @Column(name = "BAND_CODE", nullable = false, updatable = false)
  val band: Int,

  @Column(name = "RANK_VALUE", nullable = false, updatable = false)
  val rank: Int,

  @Column(name = "IMPRISONMENT_STATUS_SEQ", nullable = true, updatable = false)
  val sequence: Int? = null,
) {

  val imprisonmentStatus: ImprisonmentStatus
    get() = ImprisonmentStatus.fromNomisCode(id)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as ImprisonmentStatusEntity

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String = this::class.simpleName + "(id = $id, description = $description)"
}
