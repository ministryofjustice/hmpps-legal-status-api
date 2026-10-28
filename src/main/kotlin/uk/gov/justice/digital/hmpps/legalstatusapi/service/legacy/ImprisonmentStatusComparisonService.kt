package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.legalstatusapi.client.slack.SlackApiClient
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.ImprisonmentStatusRepository
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatusMismatchEntity
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.repository.ImprisonmentStatusMismatchRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

/**
 * Compares the Imprisonment Status we calculate for a prisoner with the one NOMIS holds. When they differ, the mismatch
 * is recorded in IMPRISONMENT_STATUS_MISMATCH with a snapshot of the data used, and the development team is notified
 * on Slack with the ID of that record.
 *
 * The NOMIS Imprisonment Status comes from Prisoner Search, which is updated from NOMIS asynchronously. If it is read
 * soon after NOMIS recalculates the status it may not have been updated yet, which will show as a mismatch.
 */
@Service
class ImprisonmentStatusComparisonService(
  private val legacyDataService: LegacyDataService,
  private val offenceSeverityProvider: OffenceSeverityProvider,
  private val imprisonmentStatusRepository: ImprisonmentStatusRepository,
  private val imprisonmentStatusMismatchRepository: ImprisonmentStatusMismatchRepository,
  private val slackApiClient: SlackApiClient,
  private val clock: Clock,
) {

  /**
   * Calculates the prisoner's Imprisonment Status and compares it with the one NOMIS holds.
   *
   * Errors calculating the status (e.g. from Prisoner Search or RaS) are thrown to the caller. A failure to notify Slack
   * is logged but not thrown, because the mismatch has already been recorded.
   *
   * @param prisonNumber The prison number of the prisoner.
   * @param bookingId The NOMIS booking ID, if known (e.g. from the imprisonment-status-changed event).
   * @param nomisCalculatedAt When NOMIS calculated its Imprisonment Status, if known (e.g. the event's occurredAt).
   */
  fun compare(
    prisonNumber: String,
    bookingId: Long? = null,
    nomisCalculatedAt: Instant? = null,
  ): ImprisonmentStatusComparisonResult {
    val result = legacyDataService.calculate(prisonNumber)
    val nomisStatus = result.nomisImprisonmentStatus
    val dpsStatus = result.calculation.imprisonmentStatus.nomisCode

    if (nomisStatus == null) {
      log.warn { "Not comparing Imprisonment Status for prisoner [$prisonNumber] as Prisoner Search has no NOMIS Imprisonment Status" }
      return ImprisonmentStatusComparisonResult.NoNomisStatus
    }

    if (nomisStatus == dpsStatus) {
      log.info { "Imprisonment Status [$dpsStatus] for prisoner [$prisonNumber] matches NOMIS" }
      return ImprisonmentStatusComparisonResult.Match(dpsStatus)
    }

    val mismatch = imprisonmentStatusMismatchRepository.save(
      ImprisonmentStatusMismatchEntity(
        prisonNumber = prisonNumber,
        bookingId = bookingId,
        receptionDate = result.receptionDate,
        nomisImprisonmentStatus = nomisStatus,
        nomisCalculatedAt = nomisCalculatedAt,
        dpsImprisonmentStatus = imprisonmentStatusRepository.getReferenceById(dpsStatus),
        dpsMainOffenceCode = result.calculation.mainOffenceCode,
        dpsCalculatedFrom = result.calculation.calculatedFrom,
        dpsCalculatedAt = result.calculatedAt,
        rasData = result.rasData,
        // The calculator does not return the severities it used, so they are looked up again. They are very unlikely to
        // have changed in Manage Offences since the calculation.
        offenceSeverities = offenceSeverityProvider.getSeverities(result.sentencingData.offenceCodes()),
        // TODO - Record the NOMIS sentence and offence data, once we know which API to get it from
        nomisData = "{}",
        createdAt = Instant.now(clock),
      ),
    )
    val mismatchId = mismatch.id!!
    log.info { "Imprisonment Status [$dpsStatus] for prisoner [$prisonNumber] does not match NOMIS [$nomisStatus], recorded as mismatch [$mismatchId]" }

    notifySlack(prisonNumber, nomisStatus, dpsStatus, mismatchId)

    return ImprisonmentStatusComparisonResult.Mismatch(mismatchId, nomisStatus, dpsStatus)
  }

  private fun notifySlack(prisonNumber: String, nomisStatus: String, dpsStatus: String, mismatchId: UUID) {
    try {
      slackApiClient.postToSlackApp(prisonNumber = prisonNumber, nomisStatus = nomisStatus, dpsStatus = dpsStatus, dbReference = mismatchId)
    } catch (e: Exception) {
      log.error(e) { "Failed to notify Slack of Imprisonment Status mismatch [$mismatchId] for prisoner [$prisonNumber]" }
    }
  }

  private fun LegacySentencingData.offenceCodes(): List<String> = sentences.map { it.offence.offenceCode } + offences.map { it.offenceCode }
}
