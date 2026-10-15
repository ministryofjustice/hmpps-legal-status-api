import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "11.0.12"
  kotlin("plugin.spring") version "2.4.21"
  kotlin("plugin.jpa") version "2.4.21"

  id("org.openapi.generator") version "7.26.0"

  id("name.remal.integration-tests") version "5.0.9"
  `java-test-fixtures`
}

val buildDirectory: Directory = layout.buildDirectory.get()

val postgresqlVersion = "42.7.14"
val kotlinLoggingVersion = "3.0.5"
val testContainersVersion = "2.0.5"
val springdocOpenapiVersion = "3.1.1"
val hmppsKotlinSpringBootStarterVersion = "3.0.3"
val awaitilityVersion = "4.3.0"
val swaggerParserVersion = "2.1.48"
val wiremockVersion = "3.13.2"
val hmppsSqsStarterVersion = "7.4.1"

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:$hmppsKotlinSpringBootStarterVersion")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:$hmppsSqsStarterVersion")
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

tasks.named<Jar>("jar") {
  enabled = true
}

tasks.named("check") {
  dependsOn("integrationTest")
}

tasks.named<Test>("integrationTest") {
  dependsOn("copyAgent")
}

tasks.register<GenerateTask>("buildManageOffencesApiModel") {
  dependsOn("processResources")
  group = "openapi tools"
  description = "Generate model from Manage Offences API YAML definition"
  validateSpec.set(true)
  generatorName.set("kotlin-spring")
  inputSpec.set("$projectDir/src/main/resources/static/openapi/manage-offences-api-2026-10-15.json")
  outputDir.set("$buildDirectory/generated")
  modelPackage.set("uk.gov.justice.digital.hmpps.manageoffences.resource.model")
  configOptions.set(
    mapOf(
      "dateLibrary" to "java8",
      "serializationLibrary" to "jackson",
      "useBeanValidation" to "true",
      "useSpringBoot4" to "true",
      "useJackson3" to "true",
      "openApiNullable" to "false",
      "enumPropertyNaming" to "UPPERCASE",
    ),
  )
  globalProperties.set(
    mapOf(
      "models" to "",
    ),
  )
}

tasks.register<GenerateTask>("buildRemandAndSentencingApiModel") {
  dependsOn("processResources")
  group = "openapi tools"
  description = "Generate model from Remand and Sentencing API YAML definition"
  validateSpec.set(true)
  generatorName.set("kotlin-spring")
  inputSpec.set("$projectDir/src/main/resources/static/openapi/remand-and-sentencing-api-2026-10-13.json")
  outputDir.set("$buildDirectory/generated")
  modelPackage.set("uk.gov.justice.digital.hmpps.remandandsentencing.resource.model")
  configOptions.set(
    mapOf(
      "dateLibrary" to "java8",
      "serializationLibrary" to "jackson",
      "useBeanValidation" to "true",
      "useSpringBoot4" to "true",
      "useJackson3" to "true",
      "openApiNullable" to "false",
      "enumPropertyNaming" to "UPPERCASE",
    ),
  )
  globalProperties.set(
    mapOf(
      "models" to "",
    ),
  )
}

tasks.register<GenerateTask>("buildPrisonerSearchApiModel") {
  dependsOn("processResources")
  group = "openapi tools"
  description = "Generate model from Prisoner Search API YAML definition"
  validateSpec.set(true)
  generatorName.set("kotlin-spring")
  inputSpec.set("$projectDir/src/main/resources/static/openapi/prisoner-search-api-2026-10-13.json")
  outputDir.set("$buildDirectory/generated")
  modelPackage.set("uk.gov.justice.digital.hmpps.prisonersearch.resource.model")
  configOptions.set(
    mapOf(
      "dateLibrary" to "java8",
      "serializationLibrary" to "jackson",
      "useBeanValidation" to "true",
      "useSpringBoot4" to "true",
      "useJackson3" to "true",
      "openApiNullable" to "false",
      "enumPropertyNaming" to "UPPERCASE",
    ),
  )
  globalProperties.set(
    mapOf(
      "models" to "Prisoner, PersonalCareNeed, Language, PrisonerAlias, PrisonerAlert, MainOffence, Offence, CurrentIncentive, IncentiveLevel, BodyPartDetail, Address, EmailAddress, PhoneNumber, Identifier",
    ),
  )
}

tasks {
  withType<KtLintCheckTask> {
    // We must declare the dependency here, even if we're not going to be linting the model
    mustRunAfter("buildManageOffencesApiModel", "buildRemandAndSentencingApiModel", "buildPrisonerSearchApiModel")
  }
  withType<KtLintFormatTask> {
    // We must declare the dependency here, even if we're not going to be linting the model
    mustRunAfter("buildManageOffencesApiModel", "buildRemandAndSentencingApiModel", "buildPrisonerSearchApiModel")
  }
}

tasks.named("compileKotlin") {
  dependsOn("buildManageOffencesApiModel", "buildRemandAndSentencingApiModel", "buildPrisonerSearchApiModel")
}

kotlin {
  sourceSets["main"].apply {
    kotlin.srcDir("$buildDirectory/generated/src/main/kotlin")
  }
}

// Exclude generated code from linting
ktlint {
  filter {
    exclude { projectDir.toURI().relativize(it.file.toURI()).path.contains("/generated/") }
  }
}
