package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
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
 *
 * Only recording the mismatch and notifying Slack happen in a transaction, so that a database connection is not held
 * while the status is calculated. If Slack cannot be notified the mismatch is rolled back
 * and the error thrown, so that the caller (e.g. the message listener) can retry the whole comparison.
 *
 * Slack notifications can be turned off with `slack.enabled`, e.g. until the webhook URL is available. The mismatch is
 * still recorded.
 */
@Service
class ImprisonmentStatusComparisonService(
  private val legacyDataService: LegacyDataService,
  private val offenceSeverityProvider: OffenceSeverityProvider,
  private val imprisonmentStatusRepository: ImprisonmentStatusRepository,
  private val imprisonmentStatusMismatchRepository: ImprisonmentStatusMismatchRepository,
  private val slackApiClient: SlackApiClient,
  private val transactionTemplate: TransactionTemplate,
  private val clock: Clock,
  @param:Value("\${slack.enabled}") private val slackEnabled: Boolean,
) {

  /**
   * Calculates the prisoner's Imprisonment Status and compares it with the one NOMIS holds.
   *
   * Errors calculating the status (e.g. from Prisoner Search or RaS) or notifying Slack are thrown to the caller. Nothing
   * is recorded when an error is thrown.
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

    // The calculator does not return the severities it used, so they are looked up again (before the transaction starts,
    // as it calls Manage Offences). They are very unlikely to have changed since the calculation.
    val offenceSeverities = offenceSeverityProvider.getSeverities(result.sentencingData.offenceCodes())

    val mismatchId = transactionTemplate.execute {
      // Flushed so that any database error is thrown before Slack is notified, rather than when the transaction commits
      val mismatch = imprisonmentStatusMismatchRepository.saveAndFlush(
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
          offenceSeverities = offenceSeverities,
          // TODO - Record the NOMIS sentence and offence data, once we know which API to get it from
          nomisData = "{}",
          createdAt = Instant.now(clock),
        ),
      )
      mismatch.id!!.also { notifySlack(prisonNumber, nomisStatus, dpsStatus, it) }
    }!!
    log.info { "Imprisonment Status [$dpsStatus] for prisoner [$prisonNumber] does not match NOMIS [$nomisStatus], recorded as mismatch [$mismatchId]" }

    return ImprisonmentStatusComparisonResult.Mismatch(mismatchId, nomisStatus, dpsStatus)
  }

  private fun notifySlack(prisonNumber: String, nomisStatus: String, dpsStatus: String, mismatchId: UUID) {
    if (!slackEnabled) {
      log.info { "Slack notifications are disabled, so not notifying Slack of Imprisonment Status mismatch [$mismatchId] for prisoner [$prisonNumber]" }
      return
    }
    slackApiClient.postToSlackApp(prisonNumber = prisonNumber, nomisStatus = nomisStatus, dpsStatus = dpsStatus, dbReference = mismatchId)
  }

  private fun LegacySentencingData.offenceCodes(): List<String> = sentences.map { it.offence.offenceCode } + offences.map { it.offenceCode }
}
