package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import tools.jackson.databind.annotation.JsonDeserialize
import uk.gov.justice.digital.hmpps.legalstatusapi.config.RawJsonDeserializer
import java.time.Instant

/**
 * An HMPPS domain event (the SNS `Message`). Only the fields we currently use are mapped.
 */
data class HmppsDomainEvent(
  val eventType: EventType,
  val personReference: PersonReference,
  @param:JsonDeserialize(using = RawJsonDeserializer::class) val additionalInformation: String,
  val occurredAt: Instant,
  val publishedAt: Instant,
  val description: String,
  val version: String,
) {
  fun prisonNumber(): String? = personReference.identifiers.firstOrNull { it.type == "NOMS" }?.value
}

data class PersonReference(val identifiers: List<Identifier>)

data class Identifier(val type: String, val value: String)
