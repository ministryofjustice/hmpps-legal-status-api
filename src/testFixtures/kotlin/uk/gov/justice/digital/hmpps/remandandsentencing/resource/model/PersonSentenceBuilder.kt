package uk.gov.justice.digital.hmpps.remandandsentencing.resource.model

import uk.gov.justice.digital.hmpps.legalstatusapi.aValidReference
import java.util.UUID

fun aValidPersonSentence(
  sentenceUuid: UUID = aValidReference(),
  sentenceServeType: String = "CONCURRENT",
  hasRecall: Boolean = false,
  status: PersonSentence.Status = PersonSentence.Status.ACTIVE,
  periodLengths: List<PersonPeriodLength> = emptyList(),
  sentenceType: SentenceType = aValidSentenceType(),            // Used in calculation
  legacyData: SentenceLegacyData = aValidSentenceLegacyData(),  // Used in calculation
): PersonSentence = PersonSentence(
  sentenceUuid = sentenceUuid,
  sentenceServeType = sentenceServeType,
  hasRecall = hasRecall,
  status = status,
  periodLengths = periodLengths,
  legacyData = legacyData,
  sentenceType = sentenceType
)