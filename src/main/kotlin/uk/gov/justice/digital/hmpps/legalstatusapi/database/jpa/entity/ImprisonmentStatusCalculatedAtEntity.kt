package uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Version
import java.time.Instant

@Entity(name = "IMPRISONMENT_STATUS_CALCULATED_AT")
class ImprisonmentStatusCalculatedAtEntity(
  @Id
  @Column(name = "PRISON_NUMBER", nullable = false, updatable = false)
  var prisonNumber: String,

  @Column(name = "CALCULATED_AT", nullable = false, updatable = true)
  var calculatedAt: Instant,
) {
  @Version
  var version: Int? = null
}
