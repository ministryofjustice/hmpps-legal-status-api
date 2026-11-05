package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import com.fasterxml.jackson.annotation.JsonTypeInfo
import mu.KotlinLogging
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.PersonSentence
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.SentenceTypeDetails

private val log = KotlinLogging.logger {}

/**
 * Abstract class representing a single prisoner sentence, encapsulating just the fields that are needed to
 * calculate an Imprisonment Status.
 *
 * There are 2 implementations - [RasSentence] and [LegacySentence]
 * Both implementations should return a NOMIS (legacy) sentence calc type and sentence category.
 *
 * @property offence The offence of the sentenced charge, used to look up the offence severity.
 * @property sentenceLengthDays The sentence length in days, used as the second tie-break when choosing between
 * sentences (the longest wins), or null if the sentence has no period lengths. NOMIS uses END_DATE - START_DATE, but
 * RaS gives us neither date, so this is derived from the RaS period lengths instead. It is only good for comparing
 * sentences, not as a real length. See [SentenceLength] for how, and its assumptions and caveats.
 */
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "type",
)
sealed class Sentence(
  open val offence: Offence,
  open val sentenceLengthDays: Long? = null,
) {

  /**
   * Returns the legacy sentence calc type to be used for the Imprisonment Status calculation.
   * Depending on implementation this *may* be a different value to the value that the class was constructed with.
   */
  abstract val legacySentenceCalcTypeForImprisonmentStatusCalculation: String

  /**
   * Returns the legacy sentence category to be used for the Imprisonment Status calculation.
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
    override val sentenceLengthDays: Long? = null,
    val classification: SentenceTypeDetails.Classification,
    val legacySentenceCalcType: String,
    val legacySentenceCategory: String,
    val latestRecallType: PersonSentence.LatestRecallType? = null,
  ) : Sentence(offence, sentenceLengthDays) {

    /**
     * Returns the legacy sentence calc type to be used in the Imprisonment Status calculation
     * This will either be the legacy sentence calc type that the instance was constructed with, or if there
     * is a recall on this sentence, it will be the derived legacy sentence type based on the combination of
     * the sentence classification and recall type.
     */
    override val legacySentenceCalcTypeForImprisonmentStatusCalculation: String
      get() =
        latestRecallType?.let {
          when (latestRecallType) {
            PersonSentence.LatestRecallType.LR -> findLicenseRecallLegacySentenceType(legacySentenceCalcType)
            PersonSentence.LatestRecallType.FTR_14 -> "14FTR_ORA"
            PersonSentence.LatestRecallType.FTR_28 -> if (legacySentenceCalcType.contains("ORA")) "FTR_ORA" else "FTR"
            PersonSentence.LatestRecallType.FTR_56 -> "FTR_56ORA"
            PersonSentence.LatestRecallType.FTR_HDC_14 -> "14FTRHDC_ORA"
            PersonSentence.LatestRecallType.FTR_HDC_28 -> "FTR_HDC"
            PersonSentence.LatestRecallType.CUR_HDC -> if (legacySentenceCalcType.contains("ORA")) "CUR_ORA" else "CUR"
            PersonSentence.LatestRecallType.IN_HDC -> if (legacySentenceCalcType.contains("ORA")) "HDR_ORA" else "HDR"
          }.also {
            log.debug { "Returning [$it] as the legacySentenceCalcType because of sentence classification [$classification] and recall type [$latestRecallType]" }
          }
        } ?: legacySentenceCalcType.also {
          log.debug { "Returning the original legacySentenceCalcType [$legacySentenceCalcType] because there is no recall for this sentence" }
        }

    /**
     * Returns the legacy sentence category that the instance was constructed with
     */
    override val legacySentenceCategoryForImprisonmentStatusCalculation = legacySentenceCategory

    private fun findLicenseRecallLegacySentenceType(sentenceCalcType: String): String = when (classification) {
      SentenceTypeDetails.Classification.STANDARD -> getStandardRecallType(sentenceCalcType)
      SentenceTypeDetails.Classification.EXTENDED -> getExtendedRecallType(sentenceCalcType)
      SentenceTypeDetails.Classification.SOPC -> getSopcExtendedRecallType(sentenceCalcType)
      SentenceTypeDetails.Classification.INDETERMINATE -> getIndeterminateRecallType(sentenceCalcType)
      else -> "LR"
    }

    private fun getIndeterminateRecallType(sentenceType: String): String = when (sentenceType) {
      "ALP" -> "LR_ALP"
      "ALP_CODE18" -> "LR_ALP_CDE18"
      "ALP_CODE21" -> "LR_ALP_CDE21"
      "ALP_LASPO" -> "LR_ALP_LASPO"
      "DLP" -> "LR_DLP"
      "IPP" -> "LR_IPP"
      "MLP" -> "LR_MLP"
      "LR_ALP" -> "LR_ALP"
      "LR_ALP_CDE18" -> "LR_ALP_CDE18"
      "LR_ALP_CDE21" -> "LR_ALP_CDE21"
      "LR_ALP_LASPO" -> "LR_ALP_LASPO"
      "LR_DLP" -> "LR_DLP"
      "LR_IPP" -> "LR_IPP"
      "LR_MLP" -> "LR_MLP"
      else -> "LR_LIFE"
    }

    private fun getSopcExtendedRecallType(sentenceType: String): String = when (sentenceType) {
      "SEC236A" -> "LR_SEC236A"
      "SOPC18" -> "LR_SOPC18"
      "SOPC21" -> "LR_SOPC21"
      "SDOPCU18" -> "LR_SOPC18"
      "LR_SEC236A" -> "LR_SEC236A"
      "LR_SOPC18" -> "LR_SOPC18"
      "LR_SOPC21" -> "LR_SOPC21"
      else -> "LR_SOPC21"
    }

    private fun getExtendedRecallType(sentenceType: String): String = when (sentenceType) {
      "LASPO_AR" -> "LR_LASPO_AR"
      "LASPO_DR" -> "LR_LASPO_DR"
      "EDS18" -> "LR_EDS18"
      "EDS21" -> "LR_EDS21"
      "EDSU18" -> "LR_EDSU18"
      "EPP" -> "LR_EPP"
      "EXT" -> "LR"
      "STS18" -> "LR"
      "STS21" -> "LR"
      "LR_LASPO_AR" -> "LR_LASPO_AR"
      "LR_LASPO_DR" -> "LR_LASPO_DR"
      "LR_EDS18" -> "LR_EDS18"
      "LR_EDS21" -> "LR_EDS21"
      "LR_EDSU18" -> "LR_EDSU18"
      "LR_EPP" -> "LR_EPP"
      else -> "LR_EDS21"
    }

    private fun getStandardRecallType(sentenceType: String): String = when (sentenceType) {
      "ADIMP" -> "LR"
      "ADIMP_ORA" -> "LR_ORA"
      "SEC250" -> "LRSEC250_ORA"
      "SEC250_ORA" -> "LRSEC250_ORA"
      "YOI" -> "LR"
      "YOI_ORA" -> "LR_YOI_ORA"
      "LRSEC250_ORA" -> "LRSEC250_ORA"
      "LR_YOI_ORA" -> "LR_YOI_ORA"
      else -> "LR"
    }
  }

  /**
   * Class representing a Legacy (NOMIS) Sentence
   *
   * @property sentenceCalcType The legacy sentence calc type value
   * @property sentenceCategory The legacy sentence category value
   */
  data class LegacySentence(
    override val offence: Offence,
    override val sentenceLengthDays: Long? = null,
    val sentenceCalcType: String,
    val sentenceCategory: String,
  ) : Sentence(offence, sentenceLengthDays) {

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
