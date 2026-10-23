package uk.gov.justice.digital.hmpps.legalstatusapi.config

import org.springframework.boot.jackson.ObjectValueDeserializer
import tools.jackson.core.JacksonException
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ValueDeserializer
import tools.jackson.module.kotlin.jacksonObjectMapper

/**
 * Jackson [ValueDeserializer] that deserializes an object property into a string.
 *
 * EG. Given the following JSON where the `additionalInformation` property is an object:
 * ```
 *         {
 *           "description": "A prisoner has been received into prison",
 *           "version": "1.0",
 *           "additionalInformation": { "nomsNumber": "A6099EA", "reason": "ADMISSION", "details": "ACTIVE IN:ADM-N", "currentLocation": "IN_PRISON", "prisonId": "SWI", "nomisMovementReasonCode": "N", "currentPrisonStatus": "UNDER_PRISON_CARE" }
 *         }
 * ```
 * The `additionalInformation` can be deserialized into a simple string property as follows:
 * ```
 * data class SomeType(
 *   @JsonDeserialize(using = RawJsonDeserializer::class) val additionalInformation: String,
 *   val description: String,
 *   val version: String,
 * )
 * ```
 *
 */
class RawJsonDeserializer : ObjectValueDeserializer<String>() {
  private companion object {
    @JvmStatic
    val jsonMapper = jacksonObjectMapper()
  }

  @Throws(JacksonException::class)
  override fun deserializeObject(jp: JsonParser, ctxt: DeserializationContext, tree: JsonNode): String = jsonMapper.writeValueAsString(tree)
}
