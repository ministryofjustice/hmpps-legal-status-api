package uk.gov.justice.digital.hmpps.legalstatusapi.messaging.eventhandling

import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.HmppsDomainEvent

interface HmppsDomainEventHandler {
  fun handle(event: HmppsDomainEvent)
}
