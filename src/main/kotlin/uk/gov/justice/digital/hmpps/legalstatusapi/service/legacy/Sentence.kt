package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonSentence
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.SentenceTypeDetails
import java.time.LocalDate

/**
 * Abstract class representing a single prisoner sentence, encapsulating just the fields that are needed to
 * calculate an Imprisonment Status.
 *
 * There are 2 implementations - [RasSentence] and [LegacySentence]
 * Both implementations should return a NOMIS (legacy) sentence calc type and sentence category.
 *
 * @property offence The offence of the sentenced charge, used to look up the offence severity.
 * @property startDate With [endDate], gives the sentence length (NOMIS END_DATE - START_DATE), used as the second
 * tie-break when choosing between sentences.
 * TODO - RaS does not return sentence start/end dates. Confirm how to get the sentence length (e.g. from the RaS
 *   sentence periodLengths, or the NOMIS sentence dates). Until then these are null, so sentence length is not used.
 */
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "type",
)
sealed class Sentence(
  open val offence: Offence,
  open val startDate: LocalDate? = null,
  open val endDate: LocalDate? = null,
) {

  /**
   * Returns the legacy sentence calc type to be used for the Imprisonment Status calculation.
   * Depending on implementation this *may* be a different value to the value that the class was constructed with.
   */
  abstract val legacySentenceCalcTypeForImprisonmentStatusCalculation: String

  /**
   * Returns the legacy sentence calc type to be used for the Imprisonment Status calculation.
   * Depending on implementation this *may* be a different value to the value that the class was constructed with.
   */
  abstract val legacySentenceCategoryForImprisonmentStatusCalculation: String

  /**
   * Class representing a Remand and Sentencing (RaS) Sentence
   *
   * Even though a RaS Sentence is not considered legacy, for the purpose of calculating
   * Imprisonment Status it needs to model the legacy sentence calc type and sentence category properties.
   *
   * In respect of the legacy sentence calc type and sentence category properties a [RasSentence] is different
   * to a [LegacySentence] because the presence of a recall type will change the sentence calc type.
   *
   * @property classification The RaS classification of the sentence
   * @property legacySentenceCalcType The legacy sentence calc type value
   * @property legacySentenceCategory The legacy sentence category value
   * @property latestRecallType If this sentence has any recalls associated with it, this is the latest recall type
   */
  data class RasSentence(
    override val offence: Offence,
    override val startDate: LocalDate? = null,
    override val endDate: LocalDate? = null,
    val classification: SentenceTypeDetails.Classification,
    val legacySentenceCalcType: String,
    val legacySentenceCategory: String,
    val latestRecallType: PersonSentence.LatestRecallType? = null,
  ) : Sentence(offence, startDate, endDate) {

    /**
     * Returns the legacy sentence category to be used in the Imprisonment Status calculation
     * This will either be the legacy sentence category that the instance was constructed with, or if there
     * is a recall on this sentence, it will be the derived legacy sentence type based on the combination of
     * the sentence classification and recall type.
     */
    override val legacySentenceCalcTypeForImprisonmentStatusCalculation: String
      get() {
        // TODO - refactor to either return legacySentenceCalcType, or derive a new sentence calc type based on the recall type
        return legacySentenceCalcType
      }

    /**
     * Returns the legacy sentence category that the instance was constructed with
     */
    override val legacySentenceCategoryForImprisonmentStatusCalculation = legacySentenceCategory
  }

  /**
   * Class representing a Legacy (NOMIS) Sentence
   *
   * @property sentenceCalcType The legacy sentence calc type value
   * @property sentenceCategory The legacy sentence category value
   */
  data class LegacySentence(
    override val offence: Offence,
    override val startDate: LocalDate? = null,
    override val endDate: LocalDate? = null,
    val sentenceCalcType: String,
    val sentenceCategory: String,
  ) : Sentence(offence, startDate, endDate) {

    /**
     * Returns the legacy sentence calc type that the instance was constructed with
     */
    override val legacySentenceCalcTypeForImprisonmentStatusCalculation = sentenceCalcType

    /**
     * Returns the legacy sentence category that the instance was constructed with
     */
    override val legacySentenceCategoryForImprisonmentStatusCalculation = sentenceCategory
  }
}
