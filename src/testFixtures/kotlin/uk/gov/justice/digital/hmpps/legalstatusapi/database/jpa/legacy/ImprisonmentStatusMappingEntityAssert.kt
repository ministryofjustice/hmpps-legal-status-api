package uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy

import org.assertj.core.api.AbstractObjectAssert
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatus
import uk.gov.justice.digital.hmpps.legalstatusapi.database.jpa.legacy.entity.ImprisonmentStatusMappingEntity
import java.util.UUID

fun assertThat(actual: ImprisonmentStatusMappingEntity?) = ImprisonmentStatusMappingEntityAssert(actual)

/**
 * AssertJ custom assertion for [ImprisonmentStatusMappingEntity]
 */
class ImprisonmentStatusMappingEntityAssert(actual: ImprisonmentStatusMappingEntity?) : AbstractObjectAssert<ImprisonmentStatusMappingEntityAssert, ImprisonmentStatusMappingEntity?>(actual, ImprisonmentStatusMappingEntityAssert::class.java) {

  fun hasId(expected: UUID): ImprisonmentStatusMappingEntityAssert {
    isNotNull
    with(actual!!) {
      if (id != expected) {
        failWithMessage("Expected id to be $expected, but was $id")
      }
    }
    return this
  }

  fun isMappedTo(expected: ImprisonmentStatus): ImprisonmentStatusMappingEntityAssert {
    isNotNull
    with(actual!!) {
      if (imprisonmentStatus.imprisonmentStatus != expected) {
        failWithMessage("Expected imprisonmentStatus to be $expected, but was ${imprisonmentStatus.imprisonmentStatus}")
      }
    }
    return this
  }

  fun hasSentence(expectedCategory: String, expectedCalcType: String): ImprisonmentStatusMappingEntityAssert {
    isNotNull
    with(actual!!) {
      if (sentenceCategory != expectedCategory || sentenceCalcType != expectedCalcType || offenceResultCode != null) {
        failWithMessage("Expected sentence $expectedCategory/$expectedCalcType and no offence result code, but was $sentenceCategory/$sentenceCalcType and offence result code $offenceResultCode")
      }
    }
    return this
  }

  fun hasOffenceResultCode(expected: String): ImprisonmentStatusMappingEntityAssert {
    isNotNull
    with(actual!!) {
      if (offenceResultCode != expected || sentenceCategory != null || sentenceCalcType != null) {
        failWithMessage("Expected offence result code $expected and no sentence, but was offence result code $offenceResultCode and sentence $sentenceCategory/$sentenceCalcType")
      }
    }
    return this
  }
}
