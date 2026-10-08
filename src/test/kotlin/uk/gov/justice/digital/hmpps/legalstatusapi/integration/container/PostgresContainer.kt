package uk.gov.justice.digital.hmpps.legalstatusapi.integration.container

import org.testcontainers.postgresql.PostgreSQLContainer

object PostgresContainer {
  val instance: PostgreSQLContainer by lazy {
    PostgreSQLContainer("postgres:18").apply {
      withDatabaseName("legal_status")
      withUsername("legal_status")
      withPassword("legal_status")
      start()
    }
  }
}
