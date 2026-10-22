package uk.gov.justice.digital.hmpps.remandandsentencing.resource.model

import uk.gov.justice.digital.hmpps.legalstatusapi.aValidReference
import java.util.*

fun aValidSentenceTypeDetails(
  sentenceTypeUuid: UUID = aValidReference(),
  description: String = "ORA SDS (Offender rehabilitation act standard determinate sentence)",
  classification: SentenceTypeDetails.Classification = SentenceTypeDetails.Classification.STANDARD,
  nomisCjaCode: String = "2020", // Used in calculation
  nomisSentenceCalcType: String = "ADIMP_ORA", // Used in calculation
  displayOrder: Int = 40,
  status: SentenceTypeDetails.Status = SentenceTypeDetails.Status.ACTIVE,
  isRecallable: Boolean = true,
): SentenceTypeDetails = SentenceTypeDetails(
  sentenceTypeUuid = sentenceTypeUuid,
  description = description,
  classification = classification,
  nomisCjaCode = nomisCjaCode,
  nomisSentenceCalcType = nomisSentenceCalcType,
  displayOrder = displayOrder,
  status = status,
  isRecallable = isRecallable,
)
