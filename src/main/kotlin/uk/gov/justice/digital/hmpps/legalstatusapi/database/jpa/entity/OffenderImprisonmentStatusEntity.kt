package uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.UUID

@Entity(name = "OFFENDER_IMPRISONMENT_STATUS")
@EntityListeners(value = [AuditingEntityListener::class])
class OffenderImprisonmentStatusEntity(
  @Column(nullable = false, updatable = false)
  var prisonNumber: String,

  @Column(nullable = false, updatable = false)
  var prisonId: String,

  @Column(nullable = false, updatable = false)
  var imprisonmentStatus: String,

  @Column(nullable = false, updatable = false)
  @Enumerated(EnumType.STRING)
  var source: SourceSystem,

  @Column(nullable = true, updatable = false)
  var comment: String? = null,
) {
  @Id
  @Column(nullable = false, updatable = false)
  lateinit var id: UUID

  @Column(nullable = false, updatable = false)
  @CreatedDate
  lateinit var createdAt: Instant

  @Column(nullable = false, updatable = false)
  @CreatedBy
  lateinit var createdBy: String

  @Column(nullable = false, updatable = false)
  @LastModifiedDate
  lateinit var updatedAt: Instant

  @Column(nullable = false, updatable = false)
  @LastModifiedBy
  lateinit var updatedBy: String
}
