package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import uk.gov.justice.digital.hmpps.legalstatusapi.messaging.DomainEventType.IMPRISONMENT_STATUS_CHANGED
import uk.gov.justice.digital.hmpps.legalstatusapi.randomValidPrisonNumber
import java.time.Instant

fun aValidImprisonmentStatusChangedDomainEvent(
  prisonNumber: String = randomValidPrisonNumber(),
  bookingId: String = "1242230",
) = aValidDomainEvent(
  prisonNumber = prisonNumber,
  domainEventType = IMPRISONMENT_STATUS_CHANGED,
  description = "A prisoner's imprisonment status has been changed",
  additionalInformation = aValidImprisonmentStatusChangedAdditionalInformation(bookingId),
)

fun aValidDomainEvent(
  prisonNumber: String = randomValidPrisonNumber(),
  domainEventType: DomainEventType,
  occurredAt: Instant = Instant.now().minusSeconds(10),
  publishedAt: Instant = Instant.now(),
  description: String,
  version: String = "1.0",
  additionalInformation: AdditionalInformation,
): DomainEvent = DomainEvent(
  eventType = domainEventType,
  personReference = PersonReference(listOf(Identifier(type = "NOMS", value = prisonNumber))),
  occurredAt = occurredAt,
  publishedAt = publishedAt,
  description = description,
  version = version,
  additionalInformation = JsonMapper().registerModule(JavaTimeModule()).writeValueAsString(additionalInformation),
)
