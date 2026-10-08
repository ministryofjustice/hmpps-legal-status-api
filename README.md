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

The application comes with a `dev` spring profile that includes default settings for running locally. This is not
necessary when deploying to kubernetes as these values are included in the helm configuration templates -
e.g. `values-dev.yaml`.

There is also a `docker-compose.yml` that can be used to run a local instance of the template in docker and also an
instance of HMPPS Auth (required if your service calls out to other services using a token).

```bash
docker compose pull && docker compose up
```

will run the application and HMPPS Auth within a local docker instance.

### Running the application in Intellij

```bash
docker compose pull && docker compose up --scale hmpps-legal-status-api=0
```

will just start a docker instance of HMPPS Auth and the `legal-status-db` PostgreSQL database. The application should then be started with a `dev` active profile
in Intellij.

### Database

The service uses PostgreSQL. Locally, a Postgres 18 container is provided by `docker-compose.yml`
(`legal-status-db`, port 5432, user/password/db `legal_status`, local only).
In Cloud Platform, an RDS instance is provisioned per namespace and its connection details are injected
from the `rds-postgresql-instance-output` secret. Schema changes are managed by Flyway
(`src/main/resources/migration/common`).

### Running tests

```bash
./gradlew check
```

The integration tests start their own PostgreSQL container using Testcontainers, so Docker must be running.

### Building and running the docker image locally

The `Dockerfile` relies on the application being built first. Steps to build the docker image:
1. Build the jar files
```
./gradlew clean assemble
```
2. Copy the jar files to the base directory so that the docker build can find them
```
cp build/libs/*.jar .
```
3. Build the docker image with required arguments
```
docker build --build-arg GIT_REF=21345 --build-arg GIT_BRANCH=bob --build-arg BUILD_NUMBER=$(date '+%Y-%m-%d') .
```
4. Run the docker image, setting the auth url so that it starts up
```
docker run -e HMPPS_AUTH_URL="https://sign-in-dev.hmpps.service.justice.gov.uk/auth" <sha from step 3>
```
