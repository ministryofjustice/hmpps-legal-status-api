package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.Instant

/**
 * An HMPPS domain event (the SNS `Message`). Only the fields we currently use are mapped.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class HmppsDomainEvent(
  val eventType: EventType,
  val personReference: PersonReference,
  val additionalInformation: Map<String, Any?>,
  val occurredAt: Instant,
  val publishedAt: Instant,
  val description: String,
  val version: String,
) {
  fun prisonNumber(): String? = personReference.identifiers.firstOrNull { it.type == "NOMS" }?.value
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
