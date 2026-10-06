package uk.gov.justice.digital.hmpps.legalstatusapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LegalStatusApi

fun main(args: Array<String>) {
  runApplication<LegalStatusApi>(*args)
}
