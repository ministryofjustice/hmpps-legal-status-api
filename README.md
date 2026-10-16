# HMPPS Legal Status API

[![Ministry of Justice Repository Compliance Badge](https://github-community.service.justice.gov.uk/repository-standards/api/hmpps-legal-status-api/badge?style=flat)](https://github-community.service.justice.gov.uk/repository-standards/hmpps-legal-status-api)
[![Docker Repository on ghcr](https://img.shields.io/badge/ghcr.io-repository-2496ED.svg?logo=docker)](https://ghcr.io/ministryofjustice/hmpps-legal-status-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://legal-status-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html)

## About

This service is responsible for calculating and being the single source of truth for legal status data relating to a prisoner's reason for being in prison.

This replaces the existing NOMIS calculation of data including Imprisonment Status with a new source of truth in DPS, and reduces the dependency on NOMIS.  
The service uses relevant inputs from other DPS service to generate a prisoner's legal status. This can be surfaced across DPS, equipping staff with the information that they need to drive a range of operational decisions.

### Team

This application is in development by the Version 1 `Move and Improve team - Squad 4`. They can be contacted on MOJ Slack channel `#public_move-and-improve`.

### Health

The application has a health endpoint found at `/health` which indicates if the app is running and is healthy.

### Ping

The application has a ping endpoint found at `/ping` which indicates that the app is responding to requests.

## Development and maintenance

### Running the application locally

#### Preparation

Obtain API client credentials

- populate those value from kubernetes secrets `hmpps-legal-status-api`.

  ```shell
  kubectl -n hmpps-legal-status-dev get secret hmpps-legal-status-api-client-creds -o json | jq '.data | map_values(@base64d)' 
  ```

- fill in the API client credentials in these files: `*_CLIENT_ID` and `*_CLIENT_SECRET`
    - `.env` for running outside docker
    - `.env.docker` for running in docker

---

#### Running with Docker Compose

The easiest way to run the app is to use docker compose to create the service and all dependencies.

1. Prepare `.env.docker` (from `.env.docker.sample`)

    ```shell
    cp .env.docker.sample .env.docker
    ```

    - fill in the API client credentials in `.env.docker`
      see above to obtain these
    - in case of `$` in value, escape them (with `$$`)
2. Then run

   ```shell
   docker compose --profile api up
   ```

   will run the application (from latest image) and PostgreSQL within a local docker instance.
3. Check if application is up and running
    - See `http://localhost:8081/health` to check the app is running.
    - See `http://localhost:8081/swagger-ui/index.html?configUrl=/v3/api-docs` to explore the OpenAPI spec document.
    - See `http://localhost:8081/info` to check the app info

It connects HMPPS Auth and other upstream APIs in `dev` environment. Thus, a set of valid dev API clients are required to run the application.

#### Running the application in IntelliJ

1. Prepare `.env` (from `.env.local.sample`)

    ```shell
    cp .env.local.sample .env
    ```

    - fill in the API client credentials in `.env`:
      see above to obtain these
2. Run this

    ```shell
   docker compose up -d 
    ```

    - will start dependencies only without the API application
    - `-d` for detached run
3. Run `bootRun` with  `.env` file prepared above
    - either IntelliJ
        - in the run configuration for `bootRun`
            - tick `enable EnvFile`
            - tick `Substitute Environment Variables`
            - tick `Enable experimental integrations`
            - add a new item to the run configuration list and point it to your `.env` file
        - run `bootRun`
    - or Gradle wrapper

      ```shell
      export $(grep -v '^#' .env | xargs)
      ./gradlew bootRun
      ```

---

#### Database

The service uses PostgreSQL. Locally, a Postgres 18 container is provided by `docker-compose.yml`
(`legal-status-db`, port 5432, user/password/db `legal_status`, local only).
In Cloud Platform, an RDS instance is provisioned per namespace and its connection details are injected
from the `rds-postgresql-instance-output` secret. Schema changes are managed by Flyway
(`src/main/resources/migration/common`).

#### Running tests

To run the test suite (unit and integration):

```bash
./gradlew clean check
```

#### Integration test dependencies

The integration tests spin up the following docker containers, therefore a docker runtime is required on local
development environments in order to run the tests.

- postgres - A real postgres instance is used rather than h2 in order to properly test various constraints etc. (h2 is more forgiving that postgres in this respect)

### Configuring the project

#### JDK

To develop and build the application locally you will need JDK 25 installed and configured.

#### Ktlint formatting

Ktlint is used to format the source code and a task runs in the GitHub Actions build to check the formatting.

You should run the following commands to make sure that the source code is formatted locally before it breaks the GHA build.

##### Apply ktlint formatting rules to Intellij

`./gradlew ktlintApplyToIdea`

Or to apply to all Intellij projects:

`./gradlew ktlintApplyToIdeaGlobally`

##### Run ktlint formatter on git commit

`./gradlew addKtlintFormatGitPreCommitHook`

### Environment variables

The following environment variables are required in order for the app to start:

#### General

| Name           | Description                                |
|----------------|--------------------------------------------|
| SERVER_PORT    | The port that the application will run on  |
| HMPPS_AUTH_URL | The URL for OAuth 2.0 authorisation server |

#### Database

| Name      | Description                       |
|-----------|-----------------------------------|
| DB_SERVER | The host of the DB server         |
| DB_NAME   | The name of the database instance |
| DB_USER   | The application's DB username     |
| DB_PASS   | The DB user's password            |

#### Application Insights

| Name                                   | Description                              |
|----------------------------------------|------------------------------------------|
| APPINSIGHTS_INSTRUMENTATIONKEY         | The instrumentation key for App Insights |
| APPLICATIONINSIGHTS_CONNECTION_STRING  | The connection string for App Insights   |
| APPLICATIONINSIGHTS_CONFIGURATION_FILE | A configuration file for App Insights    |

#### APIs

| Name                                    | Description                                                                     |
|-----------------------------------------|---------------------------------------------------------------------------------|
| PRISONER_SEARCH_API_URL                 | The URL of the Prisoner Search API                                              |
| PRISONER_SEARCH_API_CLIENT_ID           | hmpps-auth oauth2 client-id for connecting to the Prisoner Search API           |
| PRISONER_SEARCH_API_CLIENT_SECRET       | hmpps-auth oauth2 client-secret for connecting to the Prisoner Search API       |
| MANAGE_OFFENCES_API_URL                 | The URL of the Manage Offences API                                              |
| MANAGE_OFFENCES_API_CLIENT_ID           | hmpps-auth oauth2 client-id for connecting to the Manage Offences API           |
| MANAGE_OFFENCES_API_CLIENT_SECRET       | hmpps-auth oauth2 client-secret for connecting to the Manage Offences API       |
| REMAND_AND_SENTENCING_API_URL           | The URL of the Remand and Sentencing API                                        |
| REMAND_AND_SENTENCING_API_CLIENT_ID     | hmpps-auth oauth2 client-id for connecting to the Remand and Sentencing API     |
| REMAND_AND_SENTENCING_API_CLIENT_SECRET | hmpps-auth oauth2 client-secret for connecting to the Remand and Sentencing API |
