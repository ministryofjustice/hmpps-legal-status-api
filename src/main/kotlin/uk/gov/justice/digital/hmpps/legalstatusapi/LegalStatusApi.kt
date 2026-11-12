package uk.gov.justice.digital.hmpps.legalstatusapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class LegalStatusApi

fun main(args: Array<String>) {
  runApplication<LegalStatusApi>(*args)
}
