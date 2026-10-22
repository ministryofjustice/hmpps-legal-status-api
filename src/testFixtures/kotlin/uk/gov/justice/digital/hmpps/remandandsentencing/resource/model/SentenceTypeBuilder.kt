package uk.gov.justice.digital.hmpps.remandandsentencing.resource.model

import uk.gov.justice.digital.hmpps.legalstatusapi.aValidReference
import java.util.UUID

fun aValidSentenceType(
  sentenceTypeUuid: UUID = aValidReference(), // Used in calculation
  description: String = "ORA SDS (Offender rehabilitation act standard determinate sentence)",
  classification: SentenceType.Classification = SentenceType.Classification.STANDARD,
  displayOrder: Int = 40,
): SentenceType = SentenceType(
  sentenceTypeUuid = sentenceTypeUuid,
  description = description,
  classification = classification,
  displayOrder = displayOrder,
)