plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "11.0.11"
  kotlin("plugin.spring") version "2.4.20"
  kotlin("plugin.jpa") version "2.4.20"

  id("name.remal.integration-tests") version "5.0.7"
  `java-test-fixtures`
}

val buildDirectory: Directory = layout.buildDirectory.get()

val postgresqlVersion = "42.7.13"
val kotlinLoggingVersion = "3.0.5"
val testContainersVersion = "2.0.5"
val springdocOpenapiVersion = "3.1.1"
val hmppsKotlinSpringBootStarterVersion = "3.0.3"
val awaitilityVersion = "4.3.0"
val swaggerParserVersion = "2.1.48"
val wiremockVersion = "3.13.2"

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:$hmppsKotlinSpringBootStarterVersion")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocOpenapiVersion")

  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-flyway")

  implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")

  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:$postgresqlVersion")

  // Integration test dependencies
  integrationTestImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:$hmppsKotlinSpringBootStarterVersion")
  integrationTestImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
  integrationTestImplementation("org.awaitility:awaitility-kotlin:$awaitilityVersion")
  integrationTestImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
  integrationTestImplementation("org.testcontainers:testcontainers-postgresql:$testContainersVersion")
  integrationTestImplementation("io.swagger.parser.v3:swagger-parser:$swaggerParserVersion") {
    exclude(group = "io.swagger.core.v3")
  }

  // Test fixtures dependencies
  testFixturesImplementation("org.assertj:assertj-core")
  testFixturesImplementation("org.springframework.boot:spring-boot-starter-test")
  testFixturesImplementation("io.projectreactor:reactor-core")
  testFixturesImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}

kotlin {
  jvmToolchain(25)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
  }
}

tasks.named("check") {
  dependsOn("integrationTest")
}

tasks.named<Test>("integrationTest") {
  dependsOn("copyAgent")
}
