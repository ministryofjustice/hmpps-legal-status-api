package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import uk.gov.justice.digital.hmpps.legalstatusapi.config.RawJsonDeserializer
import java.time.Instant

/**
 * An HMPPS domain event (the SNS `Message`).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class DomainEvent(
  val eventType: DomainEventType,
  val personReference: PersonReference,
  @param:JsonDeserialize(using = RawJsonDeserializer::class) val additionalInformation: String,
  val occurredAt: Instant,
  val publishedAt: Instant,
  val description: String,
  val version: String,
) {
  fun prisonNumber(): String = personReference.identifiers.first { it.type == "NOMS" }.value
}

data class PersonReference(val identifiers: List<Identifier>)

data class Identifier(val type: String, val value: String)
