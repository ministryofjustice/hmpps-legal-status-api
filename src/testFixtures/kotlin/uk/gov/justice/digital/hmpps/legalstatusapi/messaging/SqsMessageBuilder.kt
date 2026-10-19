package uk.gov.justice.digital.hmpps.legalstatusapi.messaging

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import java.util.UUID

fun aValidSqsMessage(
  messageId: UUID = UUID.randomUUID(),
  message: Any,
): SqsMessage = SqsMessage(
  type = "Notification",
  message = JsonMapper().registerModule(JavaTimeModule()).writeValueAsString(message),
  messageId = messageId,
)
