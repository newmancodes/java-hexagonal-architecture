# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Spring Boot 4.0 application scaffolded to implement **Hexagonal Architecture** (Ports & Adapters pattern). Currently in early stages — the domain, ports, and adapters layers are yet to be built out.

- **Group ID:** `digital.newman`
- **Java version:** 25
- **Database:** PostgreSQL (via JPA)

## Commands

```bash
# Build and run all tests
./mvnw verify

# Run tests only
./mvnw test

# Run a single test class
./mvnw test -Dtest=JavaHexagonalArchitectureWithSpringApplicationTests

# Run the application (requires a running PostgreSQL instance)
./mvnw spring-boot:run

# Run with Testcontainers (spins up a Postgres container automatically)
./mvnw spring-boot:test-run
# or directly: mvn exec:java -Dexec.mainClass=digital.newman.hexagonal.TestJavaHexagonalArchitectureWithSpringApplication

# OWASP dependency vulnerability scan
./mvnw org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=9
```

## Architecture

This project is intended to follow **Hexagonal Architecture** (also known as Ports & Adapters). The intended package structure under `digital.newman.hexagonal` should be:

- **`domain`** — Pure business logic and domain models. No framework dependencies.
- **`application`** — Use cases / application services. Defines **ports** (interfaces):
  - **`port.in`** — Driving ports (use case interfaces called by inbound adapters)
  - **`port.out`** — Driven ports (interfaces the domain calls for persistence/external services)
- **`adapter`** — Implements ports using frameworks:
  - **`adapter.in.web`** — REST controllers (driving/inbound adapters)
  - **`adapter.out.persistence`** — JPA repositories and entities (driven/outbound adapters)

**Key principle:** The `domain` and `application` layers must not depend on Spring or JPA. All framework coupling lives in the `adapter` layer.

## Testing

Tests require Docker (Testcontainers spins up a `postgres:latest` container automatically via `TestcontainersConfiguration`). The `@ServiceConnection` annotation wires the container's connection details into Spring's datasource automatically — no manual configuration needed.

The CI pipeline runs `./mvnw verify` on push to `main` using JDK 25 (Temurin distribution).
