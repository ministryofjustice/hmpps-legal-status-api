package uk.gov.justice.digital.hmpps.legalstatusapi.service.legacy

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.legalstatusapi.client.remandandsentencing.RemandAndSentencingApiClient
import uk.gov.justice.digital.hmpps.legalstatusapi.config.CacheConfiguration.Companion.SENTENCE_TYPE_DETAILS
import uk.gov.justice.digital.hmpps.remandandsentencing.resource.model.SentenceTypeDetails
import java.util.UUID

/**
 * Provides the details of a RaS sentence type, including the NOMIS sentence calc type and category it maps to.
 */
interface SentenceTypeDetailsProvider {

  fun getSentenceTypeDetails(sentenceTypeUuid: UUID): SentenceTypeDetails
}

/**
 * Gets sentence type details from RaS. They are reference data, so are cached (see CacheConfiguration).
 */
@Component
class RemandAndSentencingSentenceTypeDetailsProvider(
  private val remandAndSentencingApiClient: RemandAndSentencingApiClient,
) : SentenceTypeDetailsProvider {

  @Cacheable(SENTENCE_TYPE_DETAILS)
  override fun getSentenceTypeDetails(sentenceTypeUuid: UUID): SentenceTypeDetails = remandAndSentencingApiClient.getSentenceTypeDetails(sentenceTypeUuid)
}
