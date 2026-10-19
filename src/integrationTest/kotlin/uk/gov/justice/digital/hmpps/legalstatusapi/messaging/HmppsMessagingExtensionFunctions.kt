package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsTopic
import uk.gov.justice.hmpps.sqs.publish
import uk.gov.justice.hmpps.sqs.sendMessage

fun HmppsTopic.publish(domainEvent: DomainEvent) {
  this.publish(domainEvent.eventType.eventType, JsonMapper().writeValueAsString(domainEvent))
}

fun HmppsQueue.sendMessage(sqsMessage: SqsMessage) {
  this.sendMessage("Notification", JsonMapper().writeValueAsString(sqsMessage))
}
