package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

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
}
