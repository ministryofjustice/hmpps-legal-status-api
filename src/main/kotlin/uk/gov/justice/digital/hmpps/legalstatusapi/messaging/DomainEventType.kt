package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import com.fasterxml.jackson.annotation.JsonValue

/**
 * An enumeration of HMPPS Domain Events that this service consumes and processes.
 */
enum class DomainEventType(@JsonValue val eventType: String) {
  IMPRISONMENT_STATUS_CHANGED("prison-offender-events.prisoner.imprisonment-status-changed"),
}
