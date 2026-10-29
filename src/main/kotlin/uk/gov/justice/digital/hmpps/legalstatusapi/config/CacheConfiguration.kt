package uk.gov.justice.digital.hmpps.legalstatusapi.config

import mu.KotlinLogging
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {}

/**
 * Enables caching of reference data from other services. The caches themselves are created by Spring Boot from the
 * `spring.cache` properties, which allows caching to be turned off (e.g. in integration tests) with
 * `spring.cache.type: none`.
 *
 * The caches are simple maps with no expiry of their own, so each is emptied every [TTL_HOURS] hours.
 */
@Configuration
@EnableCaching
@EnableScheduling
class CacheConfiguration {

  companion object {
    const val OFFENCE_SEVERITIES = "offenceSeverities"
    const val SENTENCE_TYPE_DETAILS = "sentenceTypeDetails"
    const val TTL_HOURS: Long = 24
  }

  @CacheEvict(cacheNames = [OFFENCE_SEVERITIES], allEntries = true)
  @Scheduled(fixedDelay = TTL_HOURS, initialDelay = TTL_HOURS, timeUnit = TimeUnit.HOURS)
  fun evictOffenceSeverities() {
    log.info { "Evicting cache [$OFFENCE_SEVERITIES] after $TTL_HOURS hours" }
  }

  @CacheEvict(cacheNames = [SENTENCE_TYPE_DETAILS], allEntries = true)
  @Scheduled(fixedDelay = TTL_HOURS, initialDelay = TTL_HOURS, timeUnit = TimeUnit.HOURS)
  fun evictSentenceTypeDetails() {
    log.info { "Evicting cache [$SENTENCE_TYPE_DETAILS] after $TTL_HOURS hours" }
  }
}
