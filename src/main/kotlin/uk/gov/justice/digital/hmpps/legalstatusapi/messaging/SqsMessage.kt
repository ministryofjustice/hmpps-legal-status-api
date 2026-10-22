package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * The SQS envelope around a message that reached our queue via a topic subscription.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SqsMessage(
  @param:JsonProperty("Type") val type: String,
  @param:JsonProperty("Message") val message: String,
  @param:JsonProperty("MessageId") val messageId: String,
)
