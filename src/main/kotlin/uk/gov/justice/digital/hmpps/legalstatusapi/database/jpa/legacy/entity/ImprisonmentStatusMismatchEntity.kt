package uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.hibernate.Hibernate
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy.CalculatedFrom
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * An Imprisonment Status calculation whose result did not match the NOMIS Imprisonment Status, with a point-in-time
 * snapshot of the NOMIS and RaS data so that the mismatch can be reproduced after the prisoner's data has changed.
 *
 * The [id] is the reference sent to Slack (and so the resulting Jira ticket).
 *
 * @property rasData The raw Remand and Sentencing API responses used in the calculation.
 * @property offenceSeverities The Manage Offences severity ranking of each offence used in the calculation. Offences
 * with no known severity are not in the map.
 * @property nomisData The NOMIS sentence and offence data at the time of the mismatch, to compare with [rasData].
 */
@Entity(name = "IMPRISONMENT_STATUS_MISMATCH")
class ImprisonmentStatusMismatchEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "ID", nullable = false, updatable = false)
  val id: UUID? = null,

  @Column(name = "PRISON_NUMBER", nullable = false, updatable = false)
  val prisonNumber: String,

  @Column(name = "BOOKING_ID", nullable = true, updatable = false)
  val bookingId: Long? = null,

  @Column(name = "RECEPTION_DATE", nullable = true, updatable = false)
  val receptionDate: LocalDate? = null,

  // A NOMIS code rather than an ImprisonmentStatus, as NOMIS may return statuses that are not in IMPRISONMENT_STATUS
  @Column(name = "NOMIS_IMPRISONMENT_STATUS", nullable = false, updatable = false)
  val nomisImprisonmentStatus: String,

  @Column(name = "NOMIS_CALCULATED_AT", nullable = true, updatable = false)
  val nomisCalculatedAt: Instant? = null,

  @ManyToOne(optional = false)
  @JoinColumn(name = "DPS_IMPRISONMENT_STATUS", nullable = false, updatable = false)
  val dpsImprisonmentStatus: ImprisonmentStatusEntity,

  @Column(name = "DPS_MAIN_OFFENCE_CODE", nullable = true, updatable = false)
  val dpsMainOffenceCode: String? = null,

  @Enumerated(EnumType.STRING)
  @Column(name = "DPS_CALCULATED_FROM", nullable = false, updatable = false)
  val dpsCalculatedFrom: CalculatedFrom,

  @Column(name = "DPS_CALCULATED_AT", nullable = false, updatable = false)
  val dpsCalculatedAt: Instant,

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "RAS_DATA", nullable = false, updatable = false)
  val rasData: String,

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "OFFENCE_SEVERITIES", nullable = false, updatable = false)
  val offenceSeverities: Map<String, Int>,

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "NOMIS_DATA", nullable = false, updatable = false)
  val nomisData: String,

  @Column(name = "CREATED_AT", nullable = false, updatable = false)
  val createdAt: Instant,
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as ImprisonmentStatusMismatchEntity

    return id != null && id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  // The JSON snapshots are deliberately excluded as they are large and contain offence data
  override fun toString(): String = this::class.simpleName + "(id = $id, prisonNumber = $prisonNumber, nomisImprisonmentStatus = $nomisImprisonmentStatus, dpsImprisonmentStatus = ${dpsImprisonmentStatus.id})"
}
