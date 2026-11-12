package uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import org.hibernate.Hibernate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity(name = "IMPRISONMENT_STATUS_CALCULATED_AT")
@EntityListeners(value = [AuditingEntityListener::class])
class ImprisonmentStatusCalculatedAtEntity(
  @Id
  @Column(name = "PRISON_NUMBER", nullable = false, updatable = false)
  val prisonNumber: String,
) {
  @LastModifiedDate
  @Column(name = "CALCULATED_AT", nullable = false, updatable = true)
  var calculatedAt: Instant = Instant.EPOCH

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as ImprisonmentStatusCalculatedAtEntity

    return prisonNumber == other.prisonNumber
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String = this::class.simpleName + "(prisonNumber = $prisonNumber, calculatedAt = $calculatedAt)"
}
