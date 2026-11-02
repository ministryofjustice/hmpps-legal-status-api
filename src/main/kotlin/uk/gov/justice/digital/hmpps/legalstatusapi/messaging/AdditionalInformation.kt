package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import java.util.UUID

/**
 * Classes modeling the different structures of Additional Information data for different HMPPS Domain Events.
 *
 * HMPPS Domain Events support additional information pertaining to the event through an untyped property `additionalInformation`
 * in the event message. Each event type can model additional information as relevant to the event.
 *
 * The classes here are used when deserializing the `additionalInformation` property for each event type.
 */
sealed class AdditionalInformation {

  /**
   * Additional Information for IMPRISONMENT_STATUS_CHANGED (prison-offender-events.prisoner.imprisonment-status-changed) HMPPS Domain Event
   */
  data class ImprisonmentStatusChangedAdditionalInformation(
    val bookingId: Long,
  ) : AdditionalInformation()

  /**
   * Additional Information for SENTENCE_INSERTED (sentence.inserted), SENTENCE_DELETED (sentence.deleted),
   * SENTENCE_UPDATED (sentence.updated) HMPPS Domain Event
   */
  data class SentenceAdditionalInformation(
    val sentenceId: UUID,
    val courtChargeId: UUID,
    val courtCaseId: UUID,
    val courtAppearanceId: UUID,
    val source: MessageSource,
    val isBreach: Boolean,
  ) : AdditionalInformation()

  /**
   * Additional Information for CHARGE_INSERTED (charge.inserted), CHARGE_DELETED (charge.deleted),
   * CHARGE_UPDATED (charge.updated) HMPPS Domain Event
   */
  data class ChargeAdditionalInformation(
    val courtChargeId: UUID,
    val courtCaseId: UUID,
    val source: MessageSource,
    val courtAppearanceId: UUID,
    val isOnFutureAppearance: Boolean,
    val isBreach: Boolean,
  ) : AdditionalInformation()
}
