package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * The SNS envelope around a message that reached our queue via a topic subscription.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SqsMessage(
  @param:JsonProperty("Type") val type: String,
  @param:JsonProperty("Message") val message: String,
  @param:JsonProperty("MessageId") val messageId: String,
)

/**
 * An HMPPS domain event (the SNS `Message`). Only the fields we currently use are mapped.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class HmppsDomainEvent(
  val eventType: String,
  val occurredAt: String? = null,
  val personReference: PersonReference? = null,
  val additionalInformation: Map<String, Any?>? = null,
) {
  fun prisonNumber(): String? = personReference?.identifiers?.firstOrNull { it.type == "NOMS" }?.value
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PersonReference(
  val identifiers: List<Identifier> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Identifier(
  val type: String,
  val value: String,
)
