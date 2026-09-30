# Alerting System - Implementation Details

## 1. Document info

| Item | Value |
|---|---|
| Title | Alerting System - Implementation Details (task breakdown) |
| Version | 0.1 |
| Date | 2026-09-30 |
| Status | Draft |
| Authors | Senior Software Engineer, backend (`backend-engineer` skill) and Senior Software Engineer, Angular UI (`frontend-engineer` skill), both Claude Code agents; merged by Claude Code |
| Input documents | `docs/02-system-architecture.md` v1.0 (binding), `docs/01-requirements-specification.md` v0.3, `docs/00-project-documentation.md` Section 5, `CLAUDE.md` |

## 2. How to read this document

- The tasks are written like JIRA issues, kept in this Markdown file instead of a tracker.
- Backend tasks are `BE-nn`, UI tasks are `FE-nn`. Within each plan, IDs follow execution order: a task depends only on tasks with lower IDs. Tasks are listed grouped by epic, so IDs inside an epic are not always consecutive.
- Each task has: Type (Story, Task, Spike), Epic, Priority, Story points, Labels, Depends on, Traceability, Description, Acceptance criteria and Technical notes.
- Story points: 1 = up to half a day, 2 = about a day, 3 = about two days, 5 = about three days.
- Priority: Highest = runnable skeleton and blockers, High = Must requirements, Medium = Should requirements, Low = nice to have.
- Every task leaves the build green: `./gradlew build` for the backend; `npm run lint`, `npm run build` and `npm test` for the UI.
- Tasks labelled `api-contract` deliver an API the UI consumes. Section 5 maps them to the UI tasks.
- Open points (Section 9) need a decision from the user; each has a proposed default so work can continue.

## 3. Summary

| Plan | Tasks | Story points |
|---|---|---|
| Backend (Spring Boot core) | 49 | 124 |
| UI (Angular) | 28 | 72 |
| **Total** | **77** | **196** |

### 3.1 Backend epics

| Epic | Tasks | Count | Story points |
|---|---|---|---|
| Build and project setup | BE-01, BE-02, BE-03, BE-04 | 4 | 10 |
| Persistence | BE-06, BE-07, BE-08, BE-09 | 4 | 11 |
| Extension interfaces | BE-10, BE-11 | 2 | 5 |
| Subscription API | BE-12, BE-14, BE-15, BE-16, BE-18, BE-19 | 6 | 15 |
| Resilience | BE-17, BE-35 | 2 | 5 |
| Security and admin API | BE-13, BE-20, BE-21, BE-22, BE-23, BE-24, BE-25 | 7 | 17 |
| Scheduling and collection | BE-26, BE-28, BE-38 | 3 | 8 |
| Sources | BE-27, BE-29 | 2 | 5 |
| Delivery and channels | BE-30, BE-31, BE-32, BE-33, BE-34, BE-36, BE-37 | 7 | 21 |
| Operations and observability | BE-05, BE-39, BE-40, BE-41, BE-42 | 5 | 12 |
| Deployment | BE-43, BE-44, BE-45 | 3 | 7 |
| Quality and testing | BE-46, BE-47, BE-48, BE-49 | 4 | 8 |
| **Total** | | **49** | **124** |

### 3.2 UI epics

| Epic | Tasks | Count | Story points |
|---|---|---|---|
| E1 Workspace and tooling | FE-01, FE-02, FE-03 | 3 | 4 |
| E2 App shell and layout | FE-04, FE-05, FE-06 | 3 | 7 |
| E3 API client and security plumbing | FE-07, FE-08 | 2 | 6 |
| E4 Public sign-up | FE-09, FE-10, FE-11, FE-12 | 4 | 10 |
| E5 Admin authentication | FE-13, FE-14, FE-15, FE-16 | 4 | 10 |
| E6 Admin subscriber management | FE-17, FE-18, FE-19, FE-20 | 4 | 13 |
| E7 Accessibility and responsiveness | FE-22, FE-23 | 2 | 5 |
| E8 Build and deployment | FE-24, FE-25 | 2 | 6 |
| E9 Quality and testing | FE-21, FE-26, FE-27, FE-28 | 4 | 11 |
| **Total** | | **28** | **72** |

## 4. Delivery plan

Milestones interleave backend and UI work. No task depends on a task in a later milestone. UI tasks that call the backend can start against a mock and are finished once the backend task in the same or an earlier milestone is done.

| Milestone | Goal | Backend tasks | UI tasks | Story points |
|---|---|---|---|---|
| M1 Runnable skeletons | Both applications build, test and start; health endpoints; UI shell with routes and theme. | BE-01, BE-02, BE-03, BE-04, BE-05 | FE-01, FE-02, FE-03, FE-04, FE-05 | 21 |
| M2 Foundations | Persistence, extension interfaces, API conventions, security baseline, HTTP client; UI API client and shared form parts. | BE-06, BE-07, BE-08, BE-09, BE-10, BE-11, BE-12, BE-13, BE-17 | FE-06, FE-07, FE-08, FE-10 | 34 |
| M3 Public sign-up end to end | Email and Slack sign-up work from the browser to the database. | BE-14, BE-15, BE-16, BE-18, BE-19 | FE-09, FE-11, FE-12, FE-21 | 23 |
| M4 Admin area | Google login with allow-list, sessions, subscriber list, search and delete. | BE-20, BE-21, BE-22, BE-23, BE-24, BE-25 | FE-13, FE-14, FE-15, FE-16, FE-17, FE-18, FE-19, FE-20 | 37 |
| M5 Hourly collection and delivery | Scheduled run with lock, stub and NewsAPI.org sources, notifications through email, Slack and log channels, resilience. | BE-26, BE-27, BE-28, BE-29, BE-30, BE-31, BE-32, BE-33, BE-34, BE-35, BE-36, BE-37, BE-38 | - | 37 |
| M6 Operations, deployment and quality | Operations endpoints, logging, metrics, retention, container images, Compose stacks, end-to-end and contract tests, documentation. | BE-39, BE-40, BE-41, BE-42, BE-43, BE-44, BE-45, BE-46, BE-47, BE-48, BE-49 | FE-22, FE-23, FE-24, FE-25, FE-26, FE-27, FE-28 | 44 |

Critical path to a first end-to-end demo (sign-up, hourly run with the stub source, email to Mailpit): BE-01, BE-04, BE-06 to BE-16, BE-26 to BE-28, BE-30, BE-31, BE-33, BE-43, BE-44, with FE-01 to FE-12 and FE-24.

## 5. Backend and UI dependencies

| API or behaviour (architecture Section 9.1) | Backend task | UI tasks |
|---|---|---|
| API conventions: Problem Details, validation errors, time format | BE-12 | FE-07, FE-08, FE-09, FE-10, FE-11, FE-12 |
| CSRF cookie bootstrap and 401 for unauthenticated admin API calls (OP-01, OP-03) | BE-13 | FE-07, FE-11, FE-12, FE-15, FE-16, FE-20 |
| Public `/api/v1/subscriptions/email` - Create email subscription | BE-16 | FE-09, FE-11, FE-26 |
| Public `/api/v1/subscriptions/slack` - Create Slack subscription | BE-19 | FE-09, FE-12, FE-26 |
| Auth `/oauth2/authorization/google`, `/login/oauth2/code/google` - Start login, OIDC callback, redirect targets (OP-02) | BE-20 | FE-13, FE-14, FE-26 |
| Auth `/logout` - Sign out (OP-02) | BE-21 | FE-15, FE-26 |
| Admin `/api/v1/admin/me` - Get current admin | BE-22 | FE-13, FE-26 |
| Admin `/api/v1/admin/subscribers` - List with paging and text search | BE-23 | FE-16, FE-17, FE-18, FE-19, FE-26 |
| Admin `/api/v1/admin/subscribers/{id}` - Delete | BE-24 | FE-17, FE-20, FE-26 |
| Test OIDC provider and `e2e` profile (OP-06) | BE-25 | FE-26 |
| Docker Compose demo stack (OP-05) | BE-44 | FE-25, FE-26 |
| Committed OpenAPI contract | BE-47 | FE-27 |
| UI container image | FE-24 | BE-44 depends on it |

## 6. Backend tasks

Modules as in architecture Section 6.2: `core/alerting-spi`, `core/alerting-app`, `core/source-newsapi`, `core/source-stub`, `core/channel-email`, `core/channel-slack`, `core/channel-log`. Plugins are runtime dependencies of `alerting-app` only. Configuration groups use the prefix `alerting.*`, each bound to a validated `@ConfigurationProperties` class. "Integration test" means a Spring Boot test on H2 unless PostgreSQL (Testcontainers) is named.

### Epic: Build and project setup

#### BE-01: Scaffold the Gradle multi-module core with a runnable Spring Boot application

- **Type:** Task
- **Epic:** Build and project setup
- **Priority:** Highest
- **Story points:** 3
- **Labels:** backend, build, skeleton
- **Depends on:** none
- **Traceability:** CON-01, NFR-14, NFR-15, ADR-01, ADR-15, architecture Sections 3, 6.2

**Description**
Create the `core/` Gradle build with the Gradle wrapper (9.x), a version catalog and the seven modules from Section 6.2. `alerting-app` is a Spring Boot 4.1.x application on Java 17 that starts and answers a health check; the plugin modules are empty Java libraries wired into the application as runtime dependencies. This is the runnable skeleton every later task builds on.

**Acceptance criteria**
- [ ] `./gradlew build` succeeds from `core/` on a clean checkout with JDK 17; the toolchain is pinned to Java 17.
- [ ] `./gradlew :alerting-app:bootRun` starts the application and `/actuator/health` returns UP.
- [ ] All dependency versions live in the version catalog; Spring Boot manages the versions it covers.
- [ ] Plugin modules (`source-newsapi`, `source-stub`, `channel-email`, `channel-slack`, `channel-log`) depend only on `alerting-spi`; `alerting-app` references them only in a runtime configuration.
- [ ] A context-loads test runs in the build.
- [ ] `.gitignore` covers Gradle build outputs.

**Technical notes**
- Use the Spring Boot 4 modular starters (for example `spring-boot-starter-webmvc`, not the deprecated `spring-boot-starter-web`).
- Gradle DSL and base package are open (OP-08); the proposal is the Kotlin DSL and one base package for all modules.
- Shared build logic (Java toolchain, test setup, compiler flags such as `-parameters`) goes into a convention plugin in `buildSrc` or an included build, not copied per module.
- No virtual threads and no Java 21+ APIs (Java 17 language level).

#### BE-02: Verify and pin third-party library versions for Spring Boot 4.1 and Java 17

- **Type:** Spike
- **Epic:** Build and project setup
- **Priority:** Highest
- **Story points:** 2
- **Labels:** backend, build, spike
- **Depends on:** BE-01
- **Traceability:** ADR-05, ADR-07, ADR-08, ADR-13, ADR-15, architecture Section 3

**Description**
Several libraries were chosen in the architecture with a condition "verify before adoption". Check each one against Spring Boot 4.1.x and Java 17 using current documentation (context7 or the web), add the verified versions to the version catalog, and record the result with sources. This removes version risk before the feature tasks start.

**Acceptance criteria**
- [ ] Verified and pinned: springdoc-openapi 3.x (Java 17 baseline confirmed), Resilience4j `resilience4j-spring-boot4` 2.4.x, ShedLock 7.x (Spring JDBC provider), AWS SDK for Java v2 (SESv2), ArchUnit, Testcontainers (PostgreSQL), WireMock (compatible with the Jetty version of Boot 4), GreenMail or the Mailpit Testcontainers image.
- [ ] Each version has a short note in the task result with its source and the date of the check.
- [ ] A minimal smoke test or dependency resolution for each library passes in the build.
- [ ] Any incompatibility is reported to the user as a decision point instead of silently choosing an alternative.

**Technical notes**
- Known flag from ADR-07: the first 2.4.0 release missed a BOM entry, so pin the exact artifact version.
- Also check the springdoc Swagger UI toggle per profile and the Spring Security 7 SPA CSRF support used in BE-13.

#### BE-03: Enforce module dependency rules with ArchUnit

- **Type:** Task
- **Epic:** Build and project setup
- **Priority:** High
- **Story points:** 2
- **Labels:** backend, build, architecture-test
- **Depends on:** BE-01, BE-02
- **Traceability:** NFR-14, FR-13, FR-18, ADR-01, ADR-10, architecture Section 6.2

**Description**
Add architecture tests that make the module rules of Section 6.2 verifiable: plugin modules depend only on `alerting-spi` (plus common Spring and library packages), and `alerting-app` never uses a concrete plugin class. Add feature-package rules inside `alerting-app` so the package-by-feature structure stays clean.

**Acceptance criteria**
- [ ] A test fails when a plugin module imports a class from `alerting-app`.
- [ ] A test fails when `alerting-app` main code imports a class from a plugin module.
- [ ] A test fails when `alerting-spi` depends on Spring Boot beyond the annotations it allows.
- [ ] Web controllers do not use JPA entities in their signatures (no internal entities exposed).
- [ ] The tests run in `./gradlew build`.

**Technical notes**
- The Gradle configuration (runtime-only plugins) is the first guard; ArchUnit is the second. Keep the rule set small and readable.

#### BE-04: Establish configuration groups, profiles and secret handling

- **Type:** Task
- **Epic:** Build and project setup
- **Priority:** Highest
- **Story points:** 3
- **Labels:** backend, config, security
- **Depends on:** BE-01
- **Traceability:** FR-31, FR-33, NFR-03, NFR-16, NFR-18, CON-10, ASM-03, architecture Sections 12.2, 13.2, 13.5

**Description**
Create the profiles `local`, `demo`, `postgres`, `aws` and `test` and the validated `@ConfigurationProperties` classes for the configuration groups of Section 13.5 that the application itself owns (schedule, delivery, retention, security, management). Secrets are referenced only by environment variables or the secret store. Turn on structured (JSON) console logging so every later task logs in the target format.

**Acceptance criteria**
- [ ] Each group binds to a typed class with bean validation; the application fails at start-up with a clear message on invalid values (for example a bad cron expression or zone).
- [ ] The configured zone defaults to `Europe/Budapest` and is available as one bean used for scheduling and rendering.
- [ ] No secret value appears in any committed file; a test scans the resources for known secret property names with literal values.
- [ ] A `.env.example` (or equivalent) lists all required environment variables with placeholder values.
- [ ] Logs are JSON in `demo`, `postgres` and `aws`; human-readable in `local` is allowed.

**Technical notes**
- Plugin configuration groups (`alerting.sources.<key>`, `alerting.channels.<key>`) are added by the plugin tasks in their own modules.
- Spring Boot structured logging (ECS or Logstash format) is enough; no extra logging library.
- The `aws` profile only holds configuration placeholders; the cloud target is not provisioned (AQ-07).

---

### Epic: Persistence

#### BE-06: Set up JPA, Flyway, H2 in-memory and the PostgreSQL profile

- **Type:** Task
- **Epic:** Persistence
- **Priority:** Highest
- **Story points:** 3
- **Labels:** backend, persistence
- **Depends on:** BE-04
- **Traceability:** Section 5 constraint (in-memory database, persistent later), ADR-04, ADR-05, ADR-03, architecture Sections 8.3, 13.5

**Description**
Add Spring Data JPA (Hibernate 7) and Flyway, with H2 in-memory in PostgreSQL compatibility mode for `local`, `demo` and `test`, and PostgreSQL for the `postgres` profile. Flyway owns the schema and Hibernate only validates it. The first migrations create the technical tables: the ShedLock lock table and the Spring Session JDBC tables. Add a reusable Testcontainers PostgreSQL test base.

**Acceptance criteria**
- [ ] The application starts on H2 with no external database and on PostgreSQL with the `postgres` profile; only configuration differs.
- [ ] Flyway runs at start-up on both databases; `ddl-auto` is `validate`.
- [ ] The same migrations apply to H2 and PostgreSQL; database-specific scripts, if needed (Spring Session tables), live in vendor folders.
- [ ] A migration test runs all migrations on H2 and on PostgreSQL (Testcontainers) and passes.
- [ ] Readiness health reports DOWN when the database is unreachable.

**Technical notes**
- Spring Boot 4 ships Flyway support as a separate module; include the Flyway starter explicitly.
- Spring Session and ShedLock schemas must not be auto-initialised; they come only from Flyway.
- Datasource credentials for PostgreSQL come from environment variables.

#### BE-07: Create the subscriber and audit schema with entities and repositories

- **Type:** Task
- **Epic:** Persistence
- **Priority:** High
- **Story points:** 2
- **Labels:** backend, persistence
- **Depends on:** BE-06
- **Traceability:** FR-01, FR-03, FR-05, FR-25, FR-26, FR-30, ASM-08, ADR-11, NFR-11, architecture Section 8.1

**Description**
Add the SUBSCRIBER and AUDIT_ENTRY tables as Flyway migrations, their JPA entities and Spring Data repositories, following the generic subscriber model (ADR-11). The unique constraint on `address_fingerprint` enforces "one subscription per address".

**Acceptance criteria**
- [ ] SUBSCRIBER has all attributes of Section 8.1 with UUID keys, UTC timestamps, a unique fingerprint and status ACTIVE or INACTIVE.
- [ ] Indexes support the admin search (display name, email address) and ordering by subscription date.
- [ ] AUDIT_ENTRY has admin email, action, subscriber type and time, and no personal data of the deleted subscriber.
- [ ] Repository tests (data JPA slice) on H2 and PostgreSQL prove the unique fingerprint constraint and the paging query.

**Technical notes**
- The optional JSON attributes column from ADR-11 is not added now (no current type needs it).
- Timestamps are `Instant` in entities.

#### BE-08: Create the event, run and notification schema with entities and repositories

- **Type:** Task
- **Epic:** Persistence
- **Priority:** High
- **Story points:** 3
- **Labels:** backend, persistence
- **Depends on:** BE-06
- **Traceability:** CON-05, FR-09, FR-10, FR-14, FR-19, FR-20, NFR-16, ADR-06, architecture Sections 8.1, 8.2

**Description**
Add EVENT, RUN, RUN_SOURCE_RESULT, SOURCE_STATE and NOTIFICATION as Flyway migrations with entities and repositories. The unique `event_key` and the unique (`event_id`, `subscriber_id`) pair are the database-level guarantees against duplicates. NOTIFICATION keeps only the opaque `subscriber_id`, without a cascading foreign key to SUBSCRIBER, so history survives subscriber deletion (Section 8.2).

**Acceptance criteria**
- [ ] All attributes and status values of Section 8.1 exist; mandatory event fields are NOT NULL, `link` is nullable.
- [ ] Inserting a second event with the same `event_key`, or a second notification with the same event and subscriber, fails with a constraint violation.
- [ ] Indexes support the delivery query (status, next attempt, lease), retention queries (by time) and the run history (by start time).
- [ ] Repository tests on H2 and PostgreSQL cover the unique constraints and the "claimable notifications" query.

**Technical notes**
- Batch inserts must be possible for notifications (UUIDs generated in the application, JDBC batch size configured).
- Choose "no foreign key" for `subscriber_id`; `event_id` and `run_id` keep foreign keys.

#### BE-09: Encrypt Slack webhook URLs at rest and fingerprint subscriber addresses

- **Type:** Task
- **Epic:** Persistence
- **Priority:** High
- **Story points:** 3
- **Labels:** backend, persistence, security
- **Depends on:** BE-07
- **Traceability:** NFR-03, NFR-04, FR-05, RSK-06, ADR-09, architecture Sections 8.1, 13.1

**Description**
Implement AES-GCM attribute encryption with a key ID stored alongside the ciphertext, applied to the subscriber address when the subscriber type declares it secret, and a keyed HMAC fingerprint of the normalised address for uniqueness. Keys come from the secret store (environment variables locally) and support rotation: new writes use the active key, reads accept any configured key.

**Acceptance criteria**
- [ ] A stored Slack webhook URL is not readable in the database table; it decrypts correctly through the entity.
- [ ] Email addresses stay in plain form (searchable); fingerprints are computed the same way for all types.
- [ ] Data written with an old key is still readable after a new active key is configured.
- [ ] The application refuses to start in `demo`, `postgres` and `aws` when encryption or fingerprint keys are missing; `local` and `test` may use generated throw-away keys with a warning.
- [ ] Unit tests cover round trip, tamper detection, wrong key and key rotation.

**Technical notes**
- The converter decides "encrypt or not" from a flag on the row or the type, not from the column content.
- Never log plaintext, keys or ciphertext.

---

### Epic: Extension interfaces

#### BE-10: Define the EventSource, SubscriberType and NotificationChannel SPIs

- **Type:** Task
- **Epic:** Extension interfaces
- **Priority:** Highest
- **Story points:** 3
- **Labels:** backend, spi, extensibility
- **Depends on:** BE-01
- **Traceability:** FR-13, FR-18, NFR-14, CON-05, FR-09, FR-21, ADR-10, architecture Section 7

**Description**
Write the three SPIs and their value types in `alerting-spi` as plain Java interfaces and immutable types, following Section 7 exactly: source key and "fetch new items" with a last-success hint and a maximum number of events; subscriber type key, validation, normalisation, optional verification, masking, secret flag and channel key; channel key, render and send with a delivered / transient / permanent result, and pacing limits. Document each contract in Javadoc so plugin authors can implement it without reading the application.

**Acceptance criteria**
- [ ] The module compiles with no dependency on `alerting-app` and no Spring Boot dependency beyond what Section 6.2 allows.
- [ ] Value types cover: event draft (occurred time, title, content, source name, optional link, optional stable identity), fetch context and fetch failure classification (transient or permanent), subscriber input and validation errors per field, normalised address, verification result, notification content, delivery result with optional retry-after, pacing limits (global rate, per-recipient rate).
- [ ] Javadoc states what each SPI is not responsible for (for example sources do not deduplicate or retry).
- [ ] Unit tests cover the value-type invariants (for example a draft without a title is rejected).

**Technical notes**
- Use Java 17 records for value types.
- Keep the rendering input channel-neutral: event data plus the configured zone.

#### BE-11: Collect plugins into registries with enabled flags and fail-fast key checks

- **Type:** Task
- **Epic:** Extension interfaces
- **Priority:** Highest
- **Story points:** 2
- **Labels:** backend, spi, extensibility, config
- **Depends on:** BE-10, BE-04
- **Traceability:** FR-13, FR-18, FR-33, NFR-14, NFR-18, ADR-10, architecture Section 7

**Description**
In `alerting-app`, add registries that collect all beans of each SPI type and expose them by key. Plugins register themselves through Spring Boot auto-configuration in their own module, switched on or off by `alerting.sources.<key>.enabled` or `alerting.channels.<key>.enabled`. Start-up fails fast on duplicate keys or on a subscriber type whose channel is not available.

**Acceptance criteria**
- [ ] Two beans with the same key stop the application with a clear message.
- [ ] A subscriber type pointing to a missing or disabled channel is reported at start-up (fail or warn, as documented in the task).
- [ ] Disabling a plugin by configuration removes it from the registry without code change.
- [ ] Start-up logs the enabled sources, subscriber types and channels (keys only).
- [ ] Tests use small test plugins defined in the test sources.

**Technical notes**
- Auto-configuration registration file per plugin module; conditional on the `enabled` property.

---

### Epic: Subscription API

#### BE-12: Define API conventions: Problem Details, validation errors, time format and OpenAPI

- **Type:** Story
- **Epic:** Subscription API
- **Priority:** Highest
- **Story points:** 3
- **Labels:** backend, api, api-contract
- **Depends on:** BE-02, BE-04
- **Traceability:** FR-02, NFR-05, CON-10, ADR-13, architecture Sections 9.2, 13.1

**Description**
Set the conventions every `/api/v1` endpoint follows: RFC 9457 Problem Details for all errors with a field-level `errors` list for validation problems, stable problem `type` identifiers, ISO-8601 time values with the Central European offset, and JSON only. Add springdoc-openapi so the contract is generated from the controllers, with Swagger UI only in `local` and `demo`. The UI builds its error mapping on this task.

**Acceptance criteria**
- [ ] Validation errors return 400 with `application/problem+json` and one entry per invalid field (field name and message).
- [ ] Unknown routes, wrong methods and unexpected errors return Problem Details without stack traces or internal details.
- [ ] Error bodies never echo submitted secrets (for example the webhook URL).
- [ ] Time values in responses are ISO-8601 with offset in `Europe/Budapest`, including summer time (test for a January and a July instant).
- [ ] `/v3/api-docs` publishes the contract; Swagger UI is reachable only in `local` and `demo`.

**Technical notes**
- Spring Boot 4 uses Jackson 3; configure the mapping of `Instant` to the zoned offset in the DTO mapping or one Jackson module, not per endpoint.
- The list of problem types (validation, webhook not verified, not found, access denied) is part of the contract and documented in OpenAPI.

#### BE-14: Implement the subscription service with fingerprint-based uniqueness

- **Type:** Story
- **Epic:** Subscription API
- **Priority:** High
- **Story points:** 3
- **Labels:** backend, subscription
- **Depends on:** BE-09, BE-11
- **Traceability:** FR-01, FR-03, FR-05, ASM-08, NFR-04, ADR-09, ADR-11, architecture Sections 6.1, 10.1, 10.2

**Description**
Implement the subscription service that takes a type key and input, asks the matching `SubscriberType` to validate and normalise, computes the fingerprint, checks for an existing subscriber, runs the optional verification step for new addresses only, and stores the subscriber with its masked form. The outcome for a new and for an existing address is the same "accepted" result (FR-05).

**Acceptance criteria**
- [ ] A new address is stored ACTIVE with the subscription time, masked form and fingerprint; a secret address is stored encrypted.
- [ ] An existing address (same fingerprint) creates nothing and returns the same result as a new one; verification is not run again.
- [ ] Two concurrent requests for the same address create exactly one subscriber (the unique-constraint violation is treated as "accepted").
- [ ] A failed verification stores nothing and returns a "not verified" outcome.
- [ ] Logs show only type, subscriber ID and masked address.

**Technical notes**
- The verification call (Slack welcome) must not run inside an open database transaction.
- Unit tests with a fake subscriber type; integration test for the concurrency case.

#### BE-15: Implement the email subscriber type

- **Type:** Story
- **Epic:** Subscription API
- **Priority:** High
- **Story points:** 2
- **Labels:** backend, subscription, email
- **Depends on:** BE-10, BE-11
- **Traceability:** FR-01, FR-02, FR-05, NFR-05, architecture Section 7.2

**Description**
In `channel-email`, implement the `email` subscriber type: required name (trimmed, length-limited), valid email format, lower-case normalisation, masking for logs, not secret, channel key `email`. Register it by auto-configuration with `alerting.channels.email.enabled`.

**Acceptance criteria**
- [ ] Empty name, too long name, missing or malformed email return field-level validation errors.
- [ ] `Alice@Example.COM ` and `alice@example.com` normalise to the same address and fingerprint.
- [ ] The masked form hides most of the local part (for example `a***@example.com`).
- [ ] Unit tests cover valid, invalid and edge cases (plus sign, subdomains, internationalised domain handling as documented).

**Technical notes**
- Field limits must match the UI validators; they are published through the OpenAPI schema (BE-16).

#### BE-16: Deliver POST email subscription

- **Type:** Story
- **Epic:** Subscription API
- **Priority:** High
- **Story points:** 2
- **Labels:** backend, api, api-contract, subscription
- **Depends on:** BE-12, BE-13, BE-14, BE-15
- **Traceability:** FR-01, FR-02, FR-05, NFR-05, architecture Sections 9.1, 9.2, 10.1

**Description**
Expose `POST /api/v1/subscriptions/email` with name and email. It returns 202 with the same generic confirmation for new and existing addresses, 400 Problem Details for invalid input, and requires the CSRF token. This is the contract behind the public email sign-up page.

**Acceptance criteria**
- [ ] Valid input returns 202 with a generic confirmation body; the subscriber is stored.
- [ ] Submitting the same address again returns an identical 202 response and stores nothing new.
- [ ] Invalid input returns 400 with field errors for `name` and/or `email`; nothing is stored.
- [ ] A request without a valid CSRF token returns 403.
- [ ] The operation, request and response schemas and field constraints appear in the OpenAPI document.
- [ ] Web slice tests and one integration test cover all cases.

**Technical notes**
- The request DTO carries bean validation annotations that mirror the subscriber type rules; the type stays the final authority.

#### BE-18: Implement the Slack webhook client and the Slack subscriber type

- **Type:** Story
- **Epic:** Subscription API
- **Priority:** High
- **Story points:** 3
- **Labels:** backend, subscription, slack, security
- **Depends on:** BE-10, BE-11, BE-17
- **Traceability:** FR-03, FR-04, FR-05, NFR-04, NFR-05, CON-04, ARSK-05, architecture Sections 7.2, 10.2, 13.1

**Description**
In `channel-slack`, build a small Slack webhook client on `RestClient` (shared later by the Slack channel) and the `slack` subscriber type: strict check against the Slack incoming webhook URL pattern, canonical form, optional label, masking, secret flag, channel key `slack`, and verification by posting a short welcome message with a short timeout. Only URLs matching the pattern are ever called, which blocks server-side request forgery.

**Acceptance criteria**
- [ ] Non-Slack URLs, other hosts, `http`, extra paths or query strings are rejected with a field error; no outbound call is made.
- [ ] Equivalent URL spellings normalise to one canonical form and one fingerprint.
- [ ] The masked form shows only the host and the last few characters.
- [ ] Verification succeeds on a Slack `200 ok` answer and fails on 4xx, 5xx, timeout or connection error, with the failure classified.
- [ ] WireMock tests cover success, 404 `no_service`, 403, 410, 429, 500 and timeout.

**Technical notes**
- Timeouts come from `alerting.channels.slack.*` (short connect and read timeout for the welcome message).
- Tests point the client at WireMock through a test-only base-URL override; production code accepts only the Slack host.
- Welcome text is short and static (English only, ASM-09).

#### BE-19: Deliver POST Slack subscription

- **Type:** Story
- **Epic:** Subscription API
- **Priority:** High
- **Story points:** 2
- **Labels:** backend, api, api-contract, subscription, slack
- **Depends on:** BE-12, BE-13, BE-14, BE-18
- **Traceability:** FR-03, FR-04, FR-05, NFR-04, NFR-05, architecture Sections 9.1, 9.2, 10.2

**Description**
Expose `POST /api/v1/subscriptions/slack` with the webhook URL and an optional label. It returns 202 with the generic confirmation for new and existing webhooks, 400 for format errors, and 422 with a "webhook could not be verified" problem when the welcome message fails; in the 422 case nothing is stored. Requires the CSRF token.

**Acceptance criteria**
- [ ] A valid new webhook gets a welcome message, is stored encrypted and returns 202.
- [ ] An already registered webhook returns the identical 202 without a new welcome message.
- [ ] An invalid URL returns 400 with a field error; the URL is not echoed in the response or logs.
- [ ] A failing welcome message returns 422 with a stable problem type; nothing is stored.
- [ ] The label is optional and length-limited.
- [ ] Missing CSRF token returns 403. The OpenAPI document lists 202, 400, 403 and 422.

**Technical notes**
- Integration test with WireMock standing in for Slack.

---

### Epic: Resilience

#### BE-17: Configure outbound HTTP clients and the Resilience4j foundation

- **Type:** Task
- **Epic:** Resilience
- **Priority:** High
- **Story points:** 2
- **Labels:** backend, resilience
- **Depends on:** BE-02, BE-04
- **Traceability:** NFR-08, FR-12, FR-19, ADR-07, architecture Section 13.3

**Description**
Provide a common way for plugins to build `RestClient` instances with connect and read timeouts from their own configuration group, and add Resilience4j (Spring Boot 4 module) with named retry configurations for sources and channels: exponential backoff with jitter, retry on transient errors (timeouts, 5xx, 429), no retry on permanent errors. Resilience metrics are exported through Micrometer.

**Acceptance criteria**
- [ ] Every outbound client has explicit timeouts; a test fails if a client is built without them.
- [ ] A retry configuration retries transient failures the configured number of times with growing waits and does not retry permanent failures (unit test with a fake call).
- [ ] A 429 with `Retry-After` waits at least the given time before the next attempt.
- [ ] Retry settings are configurable per source and channel without code change.

**Technical notes**
- The transient/permanent classification comes from the SPI result types (BE-10), so the retry predicate is generic.
- Rate limiters and circuit breakers follow in BE-35.

#### BE-35: Add per-channel and per-webhook rate limiting and circuit breakers

- **Type:** Story
- **Epic:** Resilience
- **Priority:** High
- **Story points:** 3
- **Labels:** backend, resilience, delivery
- **Depends on:** BE-31, BE-33, BE-34
- **Traceability:** FR-35, NFR-08, CON-08, ADR-07, architecture Sections 7.3, 13.3

**Description**
Apply Resilience4j rate limiters from each channel's declared pacing: a global limiter for email equal to the configured send rate, and a per-recipient limiter for Slack (one message per second per webhook). Add an optional circuit breaker per external service so a dead service does not use up the run time; an open breaker leaves notifications PENDING rather than FAILED.

**Acceptance criteria**
- [ ] With a configured email rate of R per second, the sending rate does not exceed R (timed test with a fake gateway).
- [ ] Two Slack notifications to the same webhook are at least one second apart; different webhooks are not blocked by each other.
- [ ] A 429 from Slack pauses that webhook for the `Retry-After` time.
- [ ] With the circuit breaker open, notifications stay PENDING with a next attempt time and the run is PARTIAL.
- [ ] Limits and breaker settings are configurable per channel.

**Technical notes**
- Per-webhook limiters are created on demand keyed by subscriber ID and evicted when idle.

---

### Epic: Security and admin API

#### BE-13: Configure web security baseline with CSRF for the SPA

- **Type:** Story
- **Epic:** Security and admin API
- **Priority:** Highest
- **Story points:** 3
- **Labels:** backend, security, api-contract
- **Depends on:** BE-04, BE-12
- **Traceability:** NFR-01, NFR-05, FR-23, ADR-02, ADR-03, architecture Sections 9.1, 11, 13.1

**Description**
Configure the application security filter chain: CSRF protection for all state-changing requests with the cookie-to-header pattern that Angular supports (`XSRF-TOKEN` cookie, `X-XSRF-TOKEN` header), public subscription endpoints open, `/api/v1/admin/**` requiring an authenticated admin and returning 401 (not a redirect) to API calls, forwarded-header support behind the edge, and secure cookie flags. Because the SPA is served by Nginx, the UI needs a way to obtain the CSRF cookie before its first POST; this task delivers that bootstrap (see OP-01).

**Acceptance criteria**
- [ ] A state-changing request without a matching token gets 403 Problem Details; with the token it passes.
- [ ] The UI can obtain the `XSRF-TOKEN` cookie with one call that has no side effects (proposal: `GET /api/v1/csrf` returning 204 and setting the cookie; final form per OP-01).
- [ ] Unauthenticated calls to `/api/v1/admin/**` get 401 Problem Details.
- [ ] Behind the proxy, generated redirects and cookies use `https` (forwarded headers honoured).
- [ ] Cookies are `Secure`, `SameSite=Lax`; the session cookie is `HttpOnly`.
- [ ] Security tests with MockMvc cover these cases.

**Technical notes**
- Check the Spring Security 7 SPA CSRF support (BE-02) before writing a custom handler.
- Security headers such as CSP and HSTS are set at the edge (BE-44); the core adds only defaults that do not conflict.

#### BE-20: Implement Google OIDC login with the administrator allow-list

- **Type:** Story
- **Epic:** Security and admin API
- **Priority:** High
- **Story points:** 3
- **Labels:** backend, security, admin, api-contract
- **Depends on:** BE-13
- **Traceability:** FR-23, FR-24, NFR-02, CON-02, ASM-05, RSK-07, ADR-03, architecture Sections 9.1, 10.4, 13.1

**Description**
Configure Spring Security OAuth2 Client login with Google (authorization code flow with state and PKCE) at `/oauth2/authorization/google` and `/login/oauth2/code/google`. After the callback, accept only ID tokens with a verified email that is on the configured allow-list (case-insensitive); successful logins redirect to the admin area of the UI, refused logins redirect to the UI's access-denied page without creating a session.

**Acceptance criteria**
- [ ] An allowed, verified Google account ends up authenticated and is redirected to the admin UI route.
- [ ] An account not on the list, or with an unverified email, is redirected to the access-denied route and has no session.
- [ ] Allow-list entries match regardless of letter case.
- [ ] Google client ID and secret come from environment variables; the allow-list comes from `alerting.security`.
- [ ] Tests use a mocked OIDC login (Spring Security test support) for allowed and refused accounts.

**Technical notes**
- Redirect targets (`/admin` and the access-denied route) must match the UI routes (OP-02).
- Log refused logins with a masked email only.

#### BE-21: Store sessions in the database with inactivity timeout and logout

- **Type:** Story
- **Epic:** Security and admin API
- **Priority:** High
- **Story points:** 2
- **Labels:** backend, security, admin, api-contract
- **Depends on:** BE-06, BE-20
- **Traceability:** FR-29, NFR-02, ADR-03, architecture Sections 9.1, 10.4, 12.2, 13.1

**Description**
Use Spring Session JDBC so any core instance can serve an admin request, set the inactivity timeout from configuration (default 30 minutes), and provide `POST /logout` protected by CSRF that invalidates the session and clears the cookie.

**Acceptance criteria**
- [ ] Sessions are stored in the database tables created by BE-06.
- [ ] After the configured inactivity period, admin API calls return 401.
- [ ] `POST /logout` with the CSRF token invalidates the session; afterwards admin API calls return 401. The response suits an XHR call from the SPA (proposal: 204; see OP-02).
- [ ] `POST /logout` without the CSRF token is rejected.
- [ ] Integration test: two application contexts sharing one PostgreSQL database accept the same session (or equivalent proof of shared sessions).

**Technical notes**
- Session timeout configurable in `alerting.security`; test with a short timeout.

#### BE-22: Deliver GET current admin

- **Type:** Story
- **Epic:** Security and admin API
- **Priority:** High
- **Story points:** 1
- **Labels:** backend, api, api-contract, admin
- **Depends on:** BE-20
- **Traceability:** FR-23, FR-24, architecture Sections 9.1, 10.4, 11

**Description**
Expose `GET /api/v1/admin/me` returning the signed-in administrator's email and name. The UI's admin route guard uses it: 200 means signed in, 401 means start the login.

**Acceptance criteria**
- [ ] An authenticated admin gets 200 with email and name.
- [ ] Without a session the endpoint returns 401 Problem Details.
- [ ] The operation is in the OpenAPI document.

**Technical notes**
- Data comes from the OIDC principal; nothing is stored.

#### BE-23: Deliver GET subscriber list with paging and search

- **Type:** Story
- **Epic:** Security and admin API
- **Priority:** High
- **Story points:** 3
- **Labels:** backend, api, api-contract, admin
- **Depends on:** BE-07, BE-09, BE-20
- **Traceability:** FR-25, FR-26, FR-21, NFR-04, NFR-11, CON-10, architecture Sections 6.1, 9.1, 9.2, 10.4

**Description**
Expose `GET /api/v1/admin/subscribers` with `page` (zero-based), `size` (capped, for example 100) and `q` (text search), returning a page of subscribers with ID, type, name or Slack label, address (email address, masked webhook), subscription time and status (ACTIVE or INACTIVE), plus paging metadata. Results are sorted by subscription date in a stable way.

**Acceptance criteria**
- [ ] The list shows every stored subscriber with the fields of FR-25; webhook URLs appear only masked.
- [ ] `q` filters by part of the name, Slack label or email address, case-insensitively.
- [ ] Paging returns correct totals; a size above the cap is limited to the cap; invalid parameters return 400.
- [ ] Inactive subscribers (FR-21) are listed with status INACTIVE.
- [ ] Times use the Central European offset (BE-12).
- [ ] Responses for 1,000 subscribers stay well under 2 seconds in an integration test on PostgreSQL.

**Technical notes**
- Whether admins see the full email or a masked email is OP-04; the proposal follows Section 10.4 (only webhooks masked).
- Use a DTO projection; do not decrypt webhook URLs for the list.

#### BE-24: Deliver DELETE subscriber with personal data removal and audit entry

- **Type:** Story
- **Epic:** Security and admin API
- **Priority:** High
- **Story points:** 3
- **Labels:** backend, api, api-contract, admin, privacy
- **Depends on:** BE-08, BE-23
- **Traceability:** FR-27, FR-28, FR-30, architecture Sections 6.1, 8.2, 9.1, 10.4

**Description**
Expose `DELETE /api/v1/admin/subscribers/{id}` (CSRF protected). In one transaction the subscriber admin service deletes the SUBSCRIBER row and its PENDING notifications, and the audit service writes an AUDIT_ENTRY with admin, action, time and subscriber type. Historical notifications keep only the opaque subscriber ID.

**Acceptance criteria**
- [ ] Deleting an existing subscriber returns 204; it no longer appears in the list and receives no further notifications.
- [ ] After deletion, no name, email, webhook URL or label of the subscriber remains in any table.
- [ ] An audit entry exists with admin email, `SUBSCRIBER_DELETED`, time and type, and no personal data of the subscriber.
- [ ] Unknown ID returns 404 Problem Details; missing CSRF token returns 403; no session returns 401.
- [ ] If the audit write fails, the delete is rolled back.

**Technical notes**
- Section 6.1 says "cancellation" and Section 8.2 says "deletes" pending notifications; this task follows Section 8.2 (OP-11).
- A notification that is IN_PROGRESS at delete time may still be sent once; document this edge case.

#### BE-25: Provide a test OIDC provider for integration and end-to-end tests

- **Type:** Task
- **Epic:** Security and admin API
- **Priority:** High
- **Story points:** 2
- **Labels:** backend, security, test-support
- **Depends on:** BE-20
- **Traceability:** NFR-15, FR-23, FR-24, architecture Section 13.6

**Description**
Add an `e2e` profile in which Google login is replaced by a local test OIDC provider, so UI end-to-end tests (Playwright) can sign in as an allowed and as a refused administrator without real Google accounts. Provide the provider as a container in a Compose override used by the end-to-end run.

**Acceptance criteria**
- [ ] With the `e2e` profile, the login flow runs against the test provider with the same allow-list logic as production.
- [ ] Two test identities exist: one on the allow-list, one not.
- [ ] The profile cannot be activated together with `aws`.
- [ ] A short how-to lists the steps and identities for the UI team.

**Technical notes**
- Provider choice is OP-06 (proposal: a mock OAuth2/OIDC server container).

---

### Epic: Scheduling and collection

#### BE-26: Schedule the hourly run with a cluster-wide lock and run records

- **Type:** Story
- **Epic:** Scheduling and collection
- **Priority:** High
- **Story points:** 3
- **Labels:** backend, scheduling
- **Depends on:** BE-08, BE-04
- **Traceability:** FR-11, FR-31, FR-32, NFR-16, NFR-20, CON-06, CON-10, ADR-05, architecture Sections 10.3, 13.2

**Description**
Implement the run scheduler: a cron trigger with zone from `alerting.schedule` (default top of every hour, `Europe/Budapest`), a ShedLock JDBC lock named `collection-run` whose maximum hold time is slightly shorter than the interval, a RUN record per run (trigger, status, times, counts) and a run coordinator that calls collection and then delivery. A run that cannot get the lock, or finds a RUNNING run, is skipped and logged.

**Acceptance criteria**
- [ ] With the default configuration, one run starts per hour in the configured zone; changing the cron changes the schedule without code change.
- [ ] Two scheduler instances on one PostgreSQL database never run at the same time (integration test with two lock clients).
- [ ] An overlapping trigger is skipped and logged (NFR-20).
- [ ] Each run creates a RUN row that ends as COMPLETED, PARTIAL or FAILED with counts and a short error summary.
- [ ] Delivery is called by the coordinator right after collection; there is no delivery schedule setting.
- [ ] Each run logs a start and an end line with run ID and counts.

**Technical notes**
- Collection and delivery are stubs (interfaces) until BE-28 and BE-31; the coordinator is the single entry point also used by the manual trigger (BE-39).
- Daylight-saving nights: the lock and the "last run" check prevent a double run (Section 13.2); add a unit test for the fall-back hour.

#### BE-28: Implement the collection service with event keys and source isolation

- **Type:** Story
- **Epic:** Scheduling and collection
- **Priority:** High
- **Story points:** 3
- **Labels:** backend, collection
- **Depends on:** BE-26, BE-27, BE-17
- **Traceability:** FR-08, FR-09, FR-10, FR-12, FR-13, NFR-07, NFR-08, ASM-07, architecture Sections 6.1, 7.1, 10.3

**Description**
Implement the collection service: call each enabled source in isolation with its retry policy, validate and map drafts to the standard event format, compute the event key from source key plus stable identity, insert only events whose key is new, and record a RUN_SOURCE_RESULT per source and the SOURCE_STATE last-success time. A failing source is recorded and does not stop other sources or delivery.

**Acceptance criteria**
- [ ] Running collection twice with the same source data stores each event once (FR-10).
- [ ] Drafts without a title are dropped; every stored event has time, title, content and source; the link is kept when present.
- [ ] A source that fails after its retries gets a failure record with attempts and error summary; other sources still store events and the run continues to delivery (FR-12).
- [ ] The source receives the last successful fetch time as a hint.
- [ ] The run records the number of new events; zero new events leads to no notifications (ASM-07).
- [ ] Tests use the stub source and a failing test source.

**Technical notes**
- Event key: normalised URL, or title plus published time when there is no URL, unless the source supplies its own identity (Section 7.1).
- Treat a unique-key violation on insert as "already known", not as an error.

#### BE-38: Detect missed runs and resume after downtime

- **Type:** Story
- **Epic:** Scheduling and collection
- **Priority:** Medium
- **Story points:** 2
- **Labels:** backend, scheduling, reliability
- **Depends on:** BE-28, BE-31
- **Traceability:** NFR-09, AQ-05, ADR-05, architecture Sections 6.1, 10.3, 13.4

**Description**
Detect gaps in the run history: when the last successful run is older than one interval (plus a grace period), record the gap on the next run and, per OP-10, start a catch-up run (trigger CATCH_UP) after start-up. The next run passes the last-success time to sources and picks up PENDING notifications of earlier runs, so nothing is sent twice.

**Acceptance criteria**
- [ ] A gap of more than one interval is detected and recorded in the run record and the log.
- [ ] After a simulated downtime, the next run collects what the source still returns and delivers pending notifications without duplicates.
- [ ] The catch-up run uses the same lock and cannot overlap with a scheduled run.
- [ ] The known limitation (NewsAPI.org returns only current headlines, AQ-05) is written in the task notes and operator documentation.

**Technical notes**
- The data model has no gap field on RUN; OP-10 proposes using the error summary or adding a column.
- In the in-memory profile there is no history after restart, so the test runs on PostgreSQL.

---

### Epic: Sources

#### BE-27: Implement the stub event source

- **Type:** Story
- **Epic:** Sources
- **Priority:** High
- **Story points:** 2
- **Labels:** backend, source, extensibility
- **Depends on:** BE-10, BE-11
- **Traceability:** FR-13, NFR-14, ARSK-07, architecture Sections 6.2, 7.1, 12.1

**Description**
In `source-stub`, implement an `EventSource` that produces synthetic events for demos and tests, configured under `alerting.sources.stub.*` (enabled flag, number of events per run, optional fixed seed). It lets the whole flow run without a NewsAPI.org key and without using its quota.

**Acceptance criteria**
- [ ] Enabling the stub source by configuration makes its events appear in the next run; disabling it stops them.
- [ ] Events have stable identities, so a configured "repeat previous items" mode proves deduplication.
- [ ] The maximum events per run setting is respected.
- [ ] The module depends only on `alerting-spi` (ArchUnit passes).

**Technical notes**
- Produces new items on each run by default so demos show traffic; a test mode returns a fixed set.

#### BE-29: Implement the NewsAPI.org top-headlines source

- **Type:** Story
- **Epic:** Sources
- **Priority:** High
- **Story points:** 3
- **Labels:** backend, source, integration
- **Depends on:** BE-17, BE-28
- **Traceability:** FR-08, FR-09, FR-12, FR-33, NFR-03, NFR-08, NFR-18, CON-03, CON-09, RSK-04, Q-03, ARSK-07, architecture Sections 7.1, 13.3

**Description**
In `source-newsapi`, implement the `EventSource` for NewsAPI.org top headlines with one request per run. Filters (country, category, language, page size) and the maximum events per run (default 10) come from `alerting.sources.newsapi.*`; the API key comes from an environment variable. Map articles to drafts with the article URL as stable identity, and classify errors as transient or permanent.

**Acceptance criteria**
- [ ] One HTTP request per run with the configured filters and the API key in a header, not in the logged URL.
- [ ] Articles map to time, title, content (description, or content when empty), source name and link; articles without a title are dropped.
- [ ] At most the configured maximum number of events is returned.
- [ ] 5xx, timeouts and 429 are transient; 401 (bad key) and other 4xx are permanent and not retried.
- [ ] WireMock tests cover a normal response, an empty result, an error body, a timeout and a rate-limit response.
- [ ] Missing API key with the source enabled fails start-up with a clear message.

**Technical notes**
- Verify the current NewsAPI.org response format and error codes with current docs when implementing (CLAUDE.md working agreement), including how removed articles are represented.
- Free plan: 100 requests per day; retries count against it, so keep retries low by default.

---

### Epic: Delivery and channels

#### BE-30: Create notification records for new events and active subscribers

- **Type:** Story
- **Epic:** Delivery and channels
- **Priority:** High
- **Story points:** 3
- **Labels:** backend, delivery
- **Depends on:** BE-28, BE-11
- **Traceability:** FR-14, FR-20, ASM-02, ASM-07, ADR-06, architecture Sections 6.1, 10.3

**Description**
Implement the first delivery step: for every event that is new in the run and every ACTIVE subscriber, create one PENDING NOTIFICATION row with the channel key of the subscriber's type, using batch inserts. The unique (event, subscriber) pair makes the step safe to repeat.

**Acceptance criteria**
- [ ] With N new events and M active subscribers, exactly N × M notifications are created.
- [ ] Repeating the step for the same run creates no additional rows.
- [ ] Inactive subscribers and subscribers of a type whose channel is disabled get no notifications (the latter is logged).
- [ ] With 5,000 subscribers and 10 events, creation finishes in seconds on PostgreSQL (integration test with timing log).

**Technical notes**
- Insert in batches per event; on PostgreSQL an "insert ... on conflict do nothing" style is allowed only if kept portable or in a vendor-specific repository method with an H2 equivalent.

#### BE-31: Dispatch notifications through channels with leases, retries and result handling

- **Type:** Story
- **Epic:** Delivery and channels
- **Priority:** High
- **Story points:** 5
- **Labels:** backend, delivery, reliability
- **Depends on:** BE-30, BE-17
- **Traceability:** FR-14, FR-19, FR-20, FR-21, NFR-07, NFR-08, NFR-10, ADR-06, ARSK-03, architecture Sections 6.1, 10.3, 12.2

**Description**
Implement the delivery service loop: claim PENDING notifications (this run and earlier runs) and expired IN_PROGRESS leases, ordered by event, mark them IN_PROGRESS with a lease, send through the matching channel on a bounded worker pool per channel, and record the result: SENT; PENDING with attempts plus one and a backoff `next_attempt_at`, or FAILED at the maximum attempts; or FAILED on a permanent error, which marks the subscriber INACTIVE and cancels its other PENDING notifications. Run counts (sent, failed) are updated.

**Acceptance criteria**
- [ ] One failing notification does not stop others; with one invalid webhook among several subscribers all others receive all notifications (FR-19).
- [ ] Transient failures are retried up to `alerting.channels.<key>.max-attempts`, then FAILED and logged.
- [ ] A permanent Slack error marks the subscriber INACTIVE; it gets no further attempts (FR-21).
- [ ] Restarting a partly completed run sends only the missing notifications (FR-20); SENT rows are never sent again.
- [ ] An IN_PROGRESS row whose lease expired is picked up again by the next run.
- [ ] SENT is recorded immediately after the channel call returns.
- [ ] Tests use a fake channel with scripted results; one integration test with the stub source and the log channel.

**Technical notes**
- Java 17: bounded `ThreadPoolTaskExecutor` per channel, size from `alerting.delivery`.
- The remaining duplicate window (crash between send and SENT) is the documented at-least-once limitation (ARSK-03).
- Recipient data (decrypted address) is loaded only for the send call and never logged.

#### BE-32: Implement the log stub channel and prove the extension points end to end

- **Type:** Story
- **Epic:** Delivery and channels
- **Priority:** High
- **Story points:** 2
- **Labels:** backend, channel, extensibility
- **Depends on:** BE-31, BE-27
- **Traceability:** FR-13, FR-18, NFR-14, architecture Sections 6.2, 7.3, 13.6

**Description**
In `channel-log`, implement a `NotificationChannel` that writes each notification to the log (masked recipient, event title, run ID), enabled by `alerting.channels.log.enabled`. Add the acceptance test from Section 13.6: with only configuration changes, the stub source and the log channel produce events and delivered notifications.

**Acceptance criteria**
- [ ] Enabling the stub source and the log channel by configuration results in stored events and SENT notifications on the log channel.
- [ ] No existing component changes are needed to add the module (reviewable in the diff: only the new module, its auto-configuration and configuration).
- [ ] ArchUnit rules pass for the new module.

**Technical notes**
- How log-channel recipients exist is OP-09 (proposal: the module brings a `log` subscriber type without a public endpoint; tests create such subscribers through the subscription service).

#### BE-33: Implement the email channel with the SMTP gateway and message templates

- **Type:** Story
- **Epic:** Delivery and channels
- **Priority:** High
- **Story points:** 3
- **Labels:** backend, channel, email
- **Depends on:** BE-31, BE-15
- **Traceability:** FR-15, FR-17, NFR-08, CON-10, ADR-08, architecture Sections 7.3, 12.1

**Description**
In `channel-email`, implement the email `NotificationChannel` with a gateway port and its SMTP adapter (Spring Mail, Mailpit locally). Render one email per event in plain text and simple HTML with title, date and time in the configured zone, source, summary and link, from the configured sender identity. Classify SMTP errors as transient or permanent.

**Acceptance criteria**
- [ ] Each notification produces exactly one email with all FR-17 fields; the time shows the Central European offset.
- [ ] `alerting.channels.email.gateway=smtp` selects the SMTP adapter; host, port and credentials come from configuration.
- [ ] Temporary SMTP errors (4xx replies, connection problems) are transient; permanent rejections (5xx replies for the recipient) are permanent.
- [ ] HTML content is escaped (event text is untrusted).
- [ ] Integration test with GreenMail or Mailpit (Testcontainers) checks the sent message.

**Technical notes**
- Templates are rendered inside the channel (ADR-08); a small template engine or plain Java text blocks are enough.
- Permanent email errors fail the notification but do not inactivate the subscriber (FR-21 is Slack-specific).

#### BE-34: Implement the Slack notification channel

- **Type:** Story
- **Epic:** Delivery and channels
- **Priority:** High
- **Story points:** 3
- **Labels:** backend, channel, slack
- **Depends on:** BE-31, BE-18
- **Traceability:** FR-16, FR-17, FR-21, NFR-08, CON-04, RSK-05, architecture Sections 7.3, 13.3

**Description**
In `channel-slack`, implement the Slack `NotificationChannel`: one POST per event to the subscriber's webhook with a message containing title, time in the configured zone, source, summary and link. Classify results: success; transient (5xx, timeout, 429 with retry-after); permanent (for example 404 `no_service`, 410, 403 `invalid_token`, `channel_is_archived`), which leads to FAILED and an INACTIVE subscriber through BE-31.

**Acceptance criteria**
- [ ] A notification produces one Slack message with all FR-17 fields; message text escapes Slack control characters.
- [ ] Each error response from the list above is classified correctly (WireMock tests).
- [ ] The `Retry-After` value of a 429 is passed back in the result.
- [ ] Declared pacing: at most one message per second per webhook.

**Technical notes**
- Reuse the webhook client of BE-18. Verify the current list of Slack webhook error codes against Slack documentation when implementing.

#### BE-36: Enforce the delivery deadline and finish runs cleanly

- **Type:** Story
- **Epic:** Delivery and channels
- **Priority:** High
- **Story points:** 2
- **Labels:** backend, delivery, reliability
- **Depends on:** BE-31, BE-35
- **Traceability:** FR-35, NFR-10, NFR-20, architecture Sections 10.3, 12.2

**Description**
Stop dispatching new notifications a configurable time before the next scheduled run, leave the rest PENDING for the next run and log the overrun. On graceful shutdown the delivery loop stops taking new work and in-flight sends finish or their leases expire. The run is finalised with status and counts.

**Acceptance criteria**
- [ ] With a deadline reached, no new notifications are claimed; the rest stay PENDING and the run ends PARTIAL with a logged overrun.
- [ ] The next run picks up those PENDING notifications first (ordered by event).
- [ ] On shutdown, no notification is left IN_PROGRESS without a lease that will expire.
- [ ] Deadline and shutdown behaviour are tested with a slow fake channel.

**Technical notes**
- The deadline is computed from the next cron fire time in the configured zone.

#### BE-37: Implement the Amazon SES API gateway adapter

- **Type:** Story
- **Epic:** Delivery and channels
- **Priority:** Medium
- **Story points:** 3
- **Labels:** backend, channel, email, aws
- **Depends on:** BE-33
- **Traceability:** FR-15, NFR-19, NFR-03, ADR-08, AQ-06, ARSK-02, architecture Sections 7.3, 12.2

**Description**
Add the second email gateway adapter using the AWS SDK for Java v2 SESv2 API, selected with `alerting.channels.email.gateway=ses` in the `aws` profile. Credentials come from the default AWS credential chain (IAM role in the cloud), not from configuration files. Map SES errors: throttling and service errors are transient, message rejections and invalid parameters are permanent.

**Acceptance criteria**
- [ ] With `gateway=ses`, emails are sent through the SESv2 client with the configured sender identity and region.
- [ ] Error mapping is covered by tests against a mocked SES endpoint (WireMock or a stubbed client).
- [ ] No AWS credentials are needed or read in `local` and `demo`.
- [ ] Operator notes list the SES prerequisites: verified sender domain with SPF, DKIM and DMARC, and production access (NFR-19, ARSK-02).

**Technical notes**
- Not used by the local demo (Mailpit); priority Medium because the demo is local only (AQ-07).

---

### Epic: Operations and observability

#### BE-05: Expose Actuator on a separate management port with operator credentials

- **Type:** Task
- **Epic:** Operations and observability
- **Priority:** Highest
- **Story points:** 2
- **Labels:** backend, ops, security
- **Depends on:** BE-04
- **Traceability:** NFR-17, FR-34, ADR-16, architecture Sections 9.1, 12.2, 13.4

**Description**
Move Actuator to its own management port and expose health (liveness and readiness groups), info and Prometheus metrics. Protect all management endpoints except the health probes with an operator credential from the secret store, in a separate security filter chain. This gives load balancers and the later runs endpoint (BE-39) a safe home, and is part of the runnable skeleton.

**Acceptance criteria**
- [ ] Actuator endpoints answer only on the management port; the application port returns 404 for `/actuator/**`.
- [ ] `/actuator/health/liveness` and `/actuator/health/readiness` work without credentials; readiness includes the database once BE-06 is done.
- [ ] `/actuator/info` and `/actuator/prometheus` require the operator credential; a wrong credential gets 401.
- [ ] The operator credential comes from configuration (environment variable), not from the repository.

**Technical notes**
- The management port is never routed by Nginx (BE-44) or the load balancer.
- Graceful shutdown is enabled here (`server.shutdown=graceful` equivalent) so the probes behave correctly during stop.

#### BE-39: Provide the runs operations endpoint for status, manual trigger and resend

- **Type:** Story
- **Epic:** Operations and observability
- **Priority:** Medium
- **Story points:** 3
- **Labels:** backend, ops
- **Depends on:** BE-05, BE-26, BE-31
- **Traceability:** FR-34, NFR-17, ADR-16, architecture Sections 9.1, 10.3

**Description**
Add a custom Actuator endpoint `runs` on the management port: read the last runs with status, times, counts and per-source results; trigger a run (trigger MANUAL) that behaves like a scheduled one; resend the FAILED notifications of a given run by resetting them to PENDING and running delivery only. All operations use the same lock and the operator credential.

**Acceptance criteria**
- [ ] Reading returns the last N runs (N configurable) newest first, with per-source results.
- [ ] Triggering starts a run with trigger MANUAL; if a run is active, the call returns a clear "skipped, run in progress" answer.
- [ ] Resend resets only FAILED notifications of the given run (subscribers that still exist and are ACTIVE) and delivers them; unknown run ID gives 404.
- [ ] All operations require the operator credential and are unreachable on the application port.

**Technical notes**
- The runs endpoint is for operators only, not the admin UI (Q-07); it is not part of the UI API contract.

#### BE-40: Add correlation IDs and personal-data-free logging

- **Type:** Task
- **Epic:** Operations and observability
- **Priority:** High
- **Story points:** 2
- **Labels:** backend, ops, security
- **Depends on:** BE-31
- **Traceability:** NFR-16, NFR-04, architecture Section 13.4

**Description**
Put the run ID and notification ID into the logging context (MDC) during runs and a request ID on API requests, so structured logs can be correlated. Add a central masking helper used by all log statements that mention recipients, and a test that fails if a known email address or webhook URL appears in the captured logs of a full run.

**Acceptance criteria**
- [ ] Log lines within a run carry the run ID; lines about a notification carry its ID.
- [ ] A full-run test with email and Slack subscribers finds no plaintext email address, webhook URL, API key or token in the log output.
- [ ] Error logs contain enough detail (component, error class, HTTP status, attempt) to diagnose failures.

**Technical notes**
- Worker threads must receive the MDC context from the dispatching thread (task decorator).

#### BE-41: Add the last-run health indicator and delivery metrics

- **Type:** Story
- **Epic:** Operations and observability
- **Priority:** Medium
- **Story points:** 2
- **Labels:** backend, ops
- **Depends on:** BE-05, BE-26, BE-31
- **Traceability:** NFR-17, NFR-09, NFR-16, RSK-08, architecture Section 13.4

**Description**
Add a custom health indicator that reports a warning state when no successful run happened within two intervals, and Micrometer metrics for runs by status, new events, notifications sent and failed per channel, and delivery duration, exported through the Prometheus endpoint.

**Acceptance criteria**
- [ ] The last-run indicator is UP after a recent successful run and switches to the warning state after two missed intervals (test with a controlled clock).
- [ ] The indicator is not part of the liveness group, so a missed run does not restart the instance.
- [ ] Metrics appear in `/actuator/prometheus` with channel tags and without personal data in tags.

**Technical notes**
- Use a custom status (for example `WARNING`) mapped to HTTP 200, and document it for the operator.

#### BE-42: Implement the nightly retention job

- **Type:** Story
- **Epic:** Operations and observability
- **Priority:** High
- **Story points:** 3
- **Labels:** backend, ops, persistence
- **Depends on:** BE-08, BE-07, BE-26
- **Traceability:** NFR-18, FR-10, Q-08, architecture Sections 6.1, 8.4, 13.2, 13.5

**Description**
Add the retention service: a nightly cron in the configured zone, protected by its own lock, that deletes in batches events older than 30 days (only when no PENDING notification refers to them), runs and notifications older than 90 days, and audit entries older than 1 year. All periods and the cron are configurable in `alerting.retention`.

**Acceptance criteria**
- [ ] Records older than their period are deleted; newer ones stay (test with fixed timestamps).
- [ ] Events referenced by PENDING notifications are not deleted.
- [ ] Deletion happens in batches of a configurable size; the job logs counts per table.
- [ ] Two instances never run the job at the same time.
- [ ] A validation rule ensures the event period is longer than a minimum (for example 7 days) so old events are not collected again.

**Technical notes**
- Delete order respects foreign keys (notifications, then run source results, then events, then runs).

---

### Epic: Deployment

#### BE-43: Build the core container image

- **Type:** Task
- **Epic:** Deployment
- **Priority:** High
- **Story points:** 2
- **Labels:** backend, deployment
- **Depends on:** BE-05, BE-06
- **Traceability:** NFR-03, NFR-15, ADR-14, architecture Sections 12.1, 12.2

**Description**
Create the core image in `deploy/` (or via the Spring Boot Gradle image task): Java 17 runtime, layered jar, non-root user, application and management ports exposed, configuration only through environment variables and profiles. No environment-specific values or secrets in the image.

**Acceptance criteria**
- [ ] One command builds the image; the container starts with the `demo` profile and passes liveness and readiness.
- [ ] The container runs as a non-root user.
- [ ] Stopping the container triggers graceful shutdown within the configured timeout.
- [ ] Image inspection shows no secrets.

**Technical notes**
- Health check in the image uses the management port.

#### BE-44: Provide the Docker Compose demo stack

- **Type:** Task
- **Epic:** Deployment
- **Priority:** High
- **Story points:** 3
- **Labels:** backend, deployment
- **Depends on:** BE-43, BE-33, BE-20, FE-24 (UI container image)
- **Traceability:** NFR-01, NFR-03, NFR-15, ADR-02, ADR-14, AQ-04, AQ-07, architecture Sections 5, 12.1, 13.1

**Description**
Write the Docker Compose file for the local demo with `ui` (Nginx serving the Angular build and acting as reverse proxy with TLS), `core` (H2, `demo` profile) and `mailpit`. Nginx routes `/api`, `/oauth2`, `/login` and `/logout` to the core on the same origin, terminates TLS with a locally generated certificate, and sets HSTS, CSP and other security headers. The management port stays unpublished except for local operator use.

**Acceptance criteria**
- [ ] `docker compose up` with a filled `.env` starts the stack; `https://localhost` shows the UI and the API answers on the same origin.
- [ ] Email sign-up followed by a manual run (stub source) delivers an email visible in the Mailpit inbox.
- [ ] The Google login redirect URI works with the documented local settings.
- [ ] Without a NewsAPI.org key the stack runs with the stub source.
- [ ] HTTP redirects to HTTPS; responses carry HSTS and CSP headers.
- [ ] No secret is committed; the certificate is generated by a script, not stored in the repository.

**Technical notes**
- Ownership of the Nginx image and configuration between backend and UI is OP-05 (proposal: the UI plan builds the image with the static files; this task owns the proxy configuration fragment and the Compose file).

#### BE-45: Provide the Docker Compose PostgreSQL variant with two core instances

- **Type:** Task
- **Epic:** Deployment
- **Priority:** Medium
- **Story points:** 2
- **Labels:** backend, deployment
- **Depends on:** BE-44, BE-21, BE-26
- **Traceability:** Section 5 constraints (separate database, scalable and fault-tolerant), NFR-20, ADR-04, ADR-05, AQ-04, ARSK-01, architecture Sections 8.3, 12.1

**Description**
Add a Compose variant (profile or override file `postgres`) with a PostgreSQL container and two core instances behind Nginx. It shows the physically separate database, shared sessions and the single-run lock.

**Acceptance criteria**
- [ ] The variant starts with one command; both core instances become ready.
- [ ] Each hourly or manual run executes on exactly one instance; the other logs the skip.
- [ ] An admin session survives requests served by either instance.
- [ ] Data survives a restart of the core containers.
- [ ] Database credentials come from `.env`.

**Technical notes**
- Nginx load-balances round robin across both instances without sticky sessions.

---

### Epic: Quality and testing

#### BE-46: Build the full-run integration test suite on PostgreSQL

- **Type:** Task
- **Epic:** Quality and testing
- **Priority:** High
- **Story points:** 3
- **Labels:** backend, testing
- **Depends on:** BE-29, BE-33, BE-34, BE-35, BE-36
- **Traceability:** NFR-15, FR-08, FR-10, FR-12, FR-14, FR-15, FR-16, FR-17, FR-19, FR-20, FR-21, NFR-07, architecture Section 13.6

**Description**
Add integration tests that run complete hourly runs on PostgreSQL (Testcontainers) with WireMock for NewsAPI.org and Slack and GreenMail or Mailpit for SMTP. They are the executable acceptance criteria of the main flows and run in the normal build.

**Acceptance criteria**
- [ ] N new events and M subscribers (email and Slack) lead to exactly N notifications per subscriber (FR-14, FR-15, FR-16).
- [ ] A second run with the same NewsAPI.org data stores no new events and sends nothing (FR-10).
- [ ] A NewsAPI.org outage is retried, recorded, and the next run works (FR-12).
- [ ] One revoked webhook among several subscribers: all others receive everything, the revoked one becomes INACTIVE (FR-19, FR-21).
- [ ] A run interrupted in the middle and restarted sends only the missing notifications (FR-20).
- [ ] Sample messages on both channels contain all FR-17 fields.

**Technical notes**
- Use a controllable clock and trigger runs directly through the run coordinator instead of waiting for cron.

#### BE-47: Publish and guard the OpenAPI contract

- **Type:** Task
- **Epic:** Quality and testing
- **Priority:** High
- **Story points:** 2
- **Labels:** backend, api, api-contract, testing
- **Depends on:** BE-16, BE-19, BE-22, BE-23, BE-24, BE-13
- **Traceability:** ADR-13, NFR-15, architecture Sections 9.2, 13.6

**Description**
Export the generated OpenAPI document of the public and admin API to a committed file and add a test that fails when the running application's contract differs from the committed file. The UI plan uses this file as its contract source and can generate TypeScript types from it.

**Acceptance criteria**
- [ ] One Gradle task writes the current contract to the committed file.
- [ ] The build fails with a readable diff when the contract changes without updating the file.
- [ ] The file contains all `api-contract` operations, their problem responses and the CSRF header requirement.
- [ ] Operations endpoints (Actuator) are not in the file.

**Technical notes**
- File location to agree with the UI plan (proposal: `core/alerting-app/src/main/resources/openapi/alerting-api.json` or `docs/api/`). "Checked in CI" needs a CI platform (OP-07); until then the check runs in `./gradlew build`.

#### BE-48: Verify delivery throughput at demo scale

- **Type:** Task
- **Epic:** Quality and testing
- **Priority:** Medium
- **Story points:** 2
- **Labels:** backend, testing, performance
- **Depends on:** BE-36, BE-32
- **Traceability:** NFR-10, FR-35, ARSK-02, AQ-03, architecture Sections 12.2, 13.3

**Description**
Measure one run with 5,000 subscribers and 10 events (50,000 notifications) on PostgreSQL using the log channel and a WireMock Slack endpoint with realistic latency, and record the time per phase. The result shows whether delivery finishes within the interval and which rate limit dominates; email volume with a real SES quota is a known external limit (ARSK-02).

**Acceptance criteria**
- [ ] A repeatable, opt-in test (not part of the default build) creates the data set and runs one full run.
- [ ] Results (notification creation time, dispatch time, throughput per channel) are written to the task result.
- [ ] With the log channel and default pool sizes, delivery completes well within one hour.

**Technical notes**
- Run with a Gradle property or JUnit tag so the normal build stays fast.

#### BE-49: Document the backend build, test and run procedure

- **Type:** Task
- **Epic:** Quality and testing
- **Priority:** High
- **Story points:** 1
- **Labels:** backend, documentation
- **Depends on:** BE-44, BE-45
- **Traceability:** NFR-15, NFR-03, architecture Sections 12.1, 16.2 (AQ-07)

**Description**
Write the build, test and run procedure for the core and the Compose stacks: prerequisites, environment variables, profiles, commands, the Google OAuth client setup for localhost, the operator endpoints and known limitations. Fill in the "Build / run / test commands" part of `CLAUDE.md`.

**Acceptance criteria**
- [ ] A developer with Docker and JDK 17 can build, test and start the demo by following the text only.
- [ ] All environment variables and profiles are listed with their purpose, without real values.
- [ ] Known limitations are listed: in-memory data loss and single instance (ARSK-01), at-least-once edge case (ARSK-03), NewsAPI.org free plan limits (ARSK-07), missed-event recovery (AQ-05).

**Technical notes**
- Keep the text in the repository documentation structure agreed with the user (`docs/` numbering or a `core/README`).


## 7. UI tasks

The UI lives in the `ui/` Angular 21 workspace. Tasks that call the backend can start against a mock (`HttpTestingController` in unit tests, Playwright request interception in end-to-end tests) and are finished against the real API once it exists.

### E1 Workspace and tooling

#### FE-01: Scaffold the Angular 21 workspace in `ui/`

- **Type:** Task
- **Epic:** E1 Workspace and tooling
- **Priority:** Highest
- **Story points:** 2
- **Labels:** frontend, setup
- **Depends on:** none
- **Traceability:** ADR-12, ADR-02, NFR-15, NFR-11, architecture Section 3, Section 6.2 (`ui` module), Section 11

**Description**
Create the Angular 21 workspace with one application in the `ui` directory of the repository, using the Angular CLI and NPM. The application uses standalone components only, zoneless change detection, strict TypeScript and strict templates, SCSS styles and Vitest as the unit test runner. This is the base every other UI task builds on.

**Acceptance criteria**
- [ ] `ui/` contains an Angular 21 workspace with one application; no NgModules exist.
- [ ] Zoneless change detection is active and `zone.js` is not a dependency.
- [ ] `strict` TypeScript options and `strictTemplates` are on.
- [ ] The Node version is pinned to an LTS line supported by Angular 21 (22 or 24) through `engines` in `package.json` and an `.nvmrc` (or equivalent); `package-lock.json` is committed.
- [ ] `npm start`, `npm run build` and `npm test` work; the default Vitest spec passes.
- [ ] Production build budgets are set (initial bundle and component style limits) so size regressions fail the build.

**Technical notes**
- Use `ng new` with the Angular 21 CLI; accept the zoneless and Vitest defaults, verify them in the generated config rather than assuming (check angular.dev through context7 first).
- Keep the generated root component minimal; routing is added in FE-05.
- Angular 21 is in LTS (AQ-01); pin the exact 21.x versions.
- Budgets support NFR-11 (pages respond within 2 seconds).

#### FE-02: Add ESLint, Prettier and standard NPM scripts

- **Type:** Task
- **Epic:** E1 Workspace and tooling
- **Priority:** Highest
- **Story points:** 1
- **Labels:** frontend, setup, quality
- **Depends on:** FE-01
- **Traceability:** NFR-15, architecture Section 11 (build and tests), Section 5 of the project documentation ("best practices for UI")

**Description**
Add linting and formatting so all later tasks are reviewed against the same rules. Set up angular-eslint with template and accessibility lint rules, Prettier, and NPM scripts that CI and developers can run.

**Acceptance criteria**
- [ ] `npm run lint` runs angular-eslint over TypeScript and templates, including the template accessibility rules.
- [ ] `npm run format` and `npm run format:check` run Prettier; lint and Prettier rules do not conflict.
- [ ] `npm run test:ci` runs the unit tests once, headless, with coverage output.
- [ ] The scaffolded code passes all scripts.

**Technical notes**
- Use `ng add @angular-eslint/schematics` (check the Angular 21 compatible version).
- Enable the angular-eslint template accessibility rules (for example alt text, label association, valid ARIA); they support NFR-13 early.
- No CI pipeline is defined in the architecture; see OP-07.

#### FE-03: Configure the dev-server proxy for same-origin API calls

- **Type:** Task
- **Epic:** E1 Workspace and tooling
- **Priority:** Highest
- **Story points:** 1
- **Labels:** frontend, setup, security
- **Depends on:** FE-01
- **Traceability:** ADR-02, ADR-03, architecture Section 9.1, Section 11 (local development), Section 12.1

**Description**
Configure `ng serve` with a proxy so the browser sees one origin locally, as in production. The proxy forwards `/api`, `/oauth2`, `/login` and `/logout` to the Core; everything else is served by the dev server.

**Acceptance criteria**
- [ ] A proxy configuration file is referenced by the `serve` target; the Core URL can be overridden (for example by environment variable) without editing the file.
- [ ] Requests to `/api/**`, `/oauth2/**`, `/login/**` and `/logout` go to the Core; cookies and redirects work through the proxy.
- [ ] The UI makes only relative (same-origin) API calls; no absolute backend URL exists in the UI code or environment files.
- [ ] The README section of FE-28 documents how to start the UI with the proxy.

**Technical notes**
- The Google OAuth callback `/login/oauth2/code/google` must reach the Core through the proxy with the original host, so the Core builds the right redirect URI; forward `Host`/`X-Forwarded-*` headers rather than rewriting the origin (coordinate with the backend security task; see OP-02).
- No UI route may start with `/login`, `/logout`, `/oauth2` or `/api`, because these paths belong to the Core.

### E2 App shell and layout

#### FE-04: Add Angular Material and CDK with an accessible theme

- **Type:** Task
- **Epic:** E2 App shell and layout
- **Priority:** Highest
- **Story points:** 2
- **Labels:** frontend, ui, accessibility
- **Depends on:** FE-01
- **Traceability:** ADR-12, NFR-12, NFR-13, architecture Section 11 (components and styling)

**Description**
Install Angular Material and CDK (21.x) and define the application theme, typography and global styles. Colour choices must meet WCAG 2.1 AA contrast for text and interactive elements.

**Acceptance criteria**
- [ ] Angular Material and CDK of the same major version as Angular are installed.
- [ ] A custom theme (colours, typography, density) is defined in one global SCSS entry point.
- [ ] Text and focus indicator colours in the theme meet AA contrast (4.5:1 for normal text, 3:1 for large text and UI components); the check is recorded in the pull request.
- [ ] The theme respects the user's reduced-motion setting.
- [ ] A visible focus indicator exists on all interactive elements.

**Technical notes**
- Use `ng add @angular/material`; check current theming APIs for Material 21 through context7 before writing SCSS.
- No brand is defined; see OP-17.

#### FE-05: Build the application shell and route structure

- **Type:** Story
- **Epic:** E2 App shell and layout
- **Priority:** Highest
- **Story points:** 3
- **Labels:** frontend, ui, routing, accessibility
- **Depends on:** FE-04
- **Traceability:** NFR-12, NFR-13, NFR-11, NFR-14, ADR-12, architecture Section 11 (areas and routing)

**Description**
Create the root layout (header with the product name, main content landmark, footer, skip link) and the route table. Public routes: email sign-up as the default route and Slack sign-up; the admin area is a lazy-loaded route tree (placeholder for now); an unknown path shows a "page not found" page. Each route sets a page title.

**Acceptance criteria**
- [ ] Routes exist for the email sign-up (default, empty path), the Slack sign-up, a lazy-loaded admin area placeholder and a "page not found" page.
- [ ] The admin area is loaded with `loadChildren` / `loadComponent`; the production build shows it as a separate chunk that the public pages do not load.
- [ ] Each route sets a unique document title through the router title strategy.
- [ ] The layout has `header`, `nav`, `main` and `footer` landmarks and a "skip to main content" link as the first focusable element.
- [ ] On navigation, focus moves to the main heading (or main content) of the new page.
- [ ] Public navigation links switch between "Email" and "Slack" sign-up and show the current page (`aria-current`).
- [ ] The layout works from 320 px wide to desktop without horizontal scrolling.
- [ ] A component test covers the route table (default route, lazy admin, not found).

**Technical notes**
- Standalone components, `provideRouter` with component input binding; signals for any shell state.
- Adding a future subscriber type means adding a route and a form component only (Section 7.2, NFR-14); keep the public nav driven by a small route list so that stays a one-line change.
- The admin placeholder is replaced in FE-14.

#### FE-06: Add the UI text catalogue and CET time formatting

- **Type:** Task
- **Epic:** E2 App shell and layout
- **Priority:** High
- **Story points:** 2
- **Labels:** frontend, i18n, time
- **Depends on:** FE-01
- **Traceability:** ASM-09, CON-10, ASM-03, AQ-02, FR-25, architecture Section 11 (i18n, time display), Section 9.2 (time values)

**Description**
Provide one place for UI messages that are set from TypeScript (validation messages, API error messages, notifications) and a formatting utility for times. Times from the API carry a CET/CEST offset and must be shown in the configured Central European zone (`Europe/Budapest`), not the browser zone.

**Acceptance criteria**
- [ ] A text catalogue holds all messages that are produced in TypeScript; components use it instead of inline string literals.
- [ ] A pure pipe (and matching function) formats an ISO-8601 timestamp with offset as date and time in `Europe/Budapest`, using the `Intl` API, with a visible zone label (CET or CEST).
- [ ] The zone ID is a single application configuration value.
- [ ] Unit tests cover a winter date (CET), a summer date (CEST), a daylight-saving transition day, and a browser running in another zone (for example UTC or America/New_York).

**Technical notes**
- Static template text stays in templates per architecture Section 11; see OP-14 for the "one place" question.
- The zone must match the backend configuration; see OP-15.

### E3 API client and security plumbing

#### FE-07: Set up `HttpClient`, CSRF handling and typed API models

- **Type:** Task
- **Epic:** E3 API client and security plumbing
- **Priority:** Highest
- **Story points:** 3
- **Labels:** frontend, api, security
- **Depends on:** FE-01, FE-03; BE-12 (API conventions), BE-13 (CSRF cookie, see OP-01)
- **Traceability:** NFR-05, ADR-02, ADR-03, ADR-13, architecture Section 9.2, Section 11 (API client, security in the browser), Section 13.1

**Description**
Configure `HttpClient` for the whole application with Angular's built-in XSRF support (cookie-to-header), and define the TypeScript types the UI exchanges with the Core: subscription requests, current admin, subscriber page and item, and RFC 9457 problem details with field errors. This is the shared base for all API services.

**Acceptance criteria**
- [ ] `provideHttpClient` is configured with XSRF protection using the cookie and header names agreed with the backend (Angular defaults `XSRF-TOKEN` / `X-XSRF-TOKEN` unless OP-01 decides otherwise).
- [ ] A unit test with `HttpTestingController` proves that POST and DELETE requests to relative URLs carry the XSRF header when the cookie is present, and GET requests do not.
- [ ] Typed models exist for every request and response in the API list (see "Backend APIs the UI needs"), in one `api` folder, matching the OpenAPI contract.
- [ ] A typed `ProblemDetails` model includes the field-level validation error list.
- [ ] No token, session or user data is written to `localStorage`, `sessionStorage` or IndexedDB.

**Technical notes**
- Can start with hand-written types from the architecture and replace them once the backend publishes the OpenAPI document; type generation is the optional spike FE-27.
- The session cookie is HttpOnly and handled by the browser; the UI never reads it.
- The exact JSON field names are not fixed yet; see OP-12.

#### FE-08: Add the error-mapping interceptor and API error model

- **Type:** Task
- **Epic:** E3 API client and security plumbing
- **Priority:** High
- **Story points:** 3
- **Labels:** frontend, api, error-handling
- **Depends on:** FE-07
- **Traceability:** NFR-05, FR-02, FR-04, architecture Section 9.2 (errors), Section 11 (API client)

**Description**
Add a functional HTTP interceptor that turns failed responses into one typed `ApiError` shape: validation problems with field errors, other problem details, network failures and unexpected errors. Components then show consistent messages without parsing HTTP details themselves.

**Acceptance criteria**
- [ ] Responses with `application/problem+json` become an `ApiError` carrying status, title, detail and field errors.
- [ ] Network errors (status 0) and 5xx responses without a problem body become a generic, user-friendly error from the text catalogue.
- [ ] 401 responses are passed through as a distinct error kind, so the admin area can react (FE-16); the interceptor itself does not navigate.
- [ ] Error messages never include request bodies (for example a webhook URL).
- [ ] Unit tests cover each error kind.

**Technical notes**
- Functional interceptor registered with `withInterceptors`.
- Keep the mapping pure and testable; components receive `ApiError` through the service layer.

### E4 Public sign-up

#### FE-09: Implement the public subscription API service

- **Type:** Task
- **Epic:** E4 Public sign-up
- **Priority:** High
- **Story points:** 2
- **Labels:** frontend, api, public
- **Depends on:** FE-08; BE-16, BE-19
- **Traceability:** FR-01, FR-03, FR-05, architecture Section 9.1, Section 9.2, Section 10.1, Section 10.2

**Description**
Create a typed `SubscriptionApiService` with one method per public subscription resource. It posts the sign-up data and returns either success (HTTP 202) or an `ApiError`. It can be built and tested against `HttpTestingController` before the backend exists.

**Acceptance criteria**
- [ ] The service has "subscribe by email" and "subscribe Slack channel" methods that POST to the two public resources with relative URLs.
- [ ] HTTP 202 maps to a success result with no data about whether the address already existed (FR-05).
- [ ] HTTP 400 and 422 problem responses map to `ApiError` with field errors.
- [ ] Unit tests cover success, validation error, Slack verification failure (422) and network error.

**Technical notes**
- Mock-first: complete with `HttpTestingController`; verify against the real Core in FE-26.
- The optional label is omitted from the request when empty (agree with backend; see OP-12).

#### FE-10: Build shared form feedback components

- **Type:** Task
- **Epic:** E4 Public sign-up
- **Priority:** High
- **Story points:** 2
- **Labels:** frontend, ui, accessibility, public
- **Depends on:** FE-04, FE-06, FE-08
- **Traceability:** NFR-13, NFR-05, FR-02, architecture Section 11 (accessibility)

**Description**
Build small reusable pieces that both sign-up forms use: a status message area announced to screen readers, an error summary that links to invalid fields, a submit button with a busy state, and a helper that copies server field errors onto the matching form controls. This keeps the two forms consistent and accessible.

**Acceptance criteria**
- [ ] A status message component announces success politely and errors assertively through an ARIA live region.
- [ ] An error summary lists all invalid fields after a failed submit, each entry moves focus to its field, and focus moves to the summary after the failed submit.
- [ ] The submit button shows a busy state, is protected against double submit and exposes the busy state to assistive technology.
- [ ] A helper maps `ApiError` field errors to typed Reactive Form controls (`setErrors`) and back to messages.
- [ ] Component tests cover announcement, focus movement and field error mapping.

**Technical notes**
- Use Material form field error slots so errors are linked to inputs with `aria-describedby`.
- Keep the components presentational (inputs and outputs as signals).

#### FE-11: Build the email sign-up page

- **Type:** Story
- **Epic:** E4 Public sign-up
- **Priority:** High
- **Story points:** 3
- **Labels:** frontend, public, forms, accessibility
- **Depends on:** FE-05, FE-09, FE-10; BE-16
- **Traceability:** FR-01, FR-02, FR-05, NFR-05, NFR-12, NFR-13, architecture Section 10.1, Section 11 (forms)

**Description**
Build the default public page where a visitor subscribes with name and email. It uses a typed Reactive Form with validators that mirror the server rules, posts through `SubscriptionApiService`, and shows a confirmation message on success. Server validation errors are shown on the fields.

**Acceptance criteria**
- [ ] The form has labelled "Name" (required) and "Email" (required, valid email format) fields with autocomplete hints (`name`, `email`).
- [ ] Invalid or empty input is rejected on the client with a clear message per field, and no request is sent.
- [ ] A valid submit shows the same generic confirmation message for new and already registered addresses (FR-05), announced to screen readers, and resets the form.
- [ ] Server field errors appear on the matching fields; other errors show a general message and keep the input.
- [ ] The page works with keyboard only and on a 320 px wide screen.
- [ ] Component tests cover validation, success, server validation error and network error.

**Technical notes**
- Validators mirror server limits (maximum lengths, email format); take exact limits from the OpenAPI contract (OP-12).
- The page states in one sentence what the visitor will receive (one email per new event).
- Mock-first with the service mocked; end-to-end in FE-21 and FE-26.

#### FE-12: Build the Slack sign-up page

- **Type:** Story
- **Epic:** E4 Public sign-up
- **Priority:** High
- **Story points:** 3
- **Labels:** frontend, public, forms, accessibility, security
- **Depends on:** FE-05, FE-09, FE-10; BE-19
- **Traceability:** FR-03, FR-04, FR-05, NFR-04, NFR-05, NFR-12, NFR-13, CON-04, architecture Section 10.2, Section 11 (forms)

**Description**
Build the public page where a visitor subscribes a Slack channel by pasting an incoming webhook URL and an optional label. The client checks the Slack incoming webhook URL format, the server verifies it by sending a welcome message, and the page shows a confirmation or a clear error. The page includes short help text on how to create an incoming webhook in Slack.

**Acceptance criteria**
- [ ] The form has a required "Slack webhook URL" field with a format check against the Slack incoming webhook pattern, and an optional "Label" field.
- [ ] A non-Slack URL is rejected on the client and no request is sent.
- [ ] While the server verifies the webhook, the page shows a busy state that is announced to screen readers.
- [ ] Success shows the same generic confirmation for new and existing webhooks (FR-05) and clears the form, including the webhook URL.
- [ ] A failed welcome message (problem response) shows "the webhook could not be verified" style guidance and nothing about the URL itself; the entered URL stays in the field so the user can correct it.
- [ ] The webhook URL is never logged to the console, shown in error texts or kept after success.
- [ ] Component tests cover format validation, success, verification failure and network error.

**Technical notes**
- The webhook pattern must match the backend's pattern exactly; take it from the backend task or OpenAPI contract (OP-12).
- Use an input type that does not trigger browser password saving; disable autocomplete on the URL field.
- Help text links to Slack's documentation on incoming webhooks (external link, opens in the same tab, marked as external).

### E5 Admin authentication

#### FE-13: Implement the admin session service and `canMatch` guard

- **Type:** Story
- **Epic:** E5 Admin authentication
- **Priority:** High
- **Story points:** 3
- **Labels:** frontend, admin, security, routing
- **Depends on:** FE-05, FE-08; BE-20, BE-22
- **Traceability:** FR-23, NFR-02, CON-02, ADR-03, architecture Section 10.4, Section 11 (guards, state)

**Description**
Create an `AdminSessionService` that holds the current admin as a signal and loads it from the "get current admin" resource. Add a functional `canMatch` guard on the admin route tree: if the admin is known it allows the match; on 401 it starts the server-side Google login by full-page navigation to the login start URL. The guard is for usability only; the Core enforces security.

**Acceptance criteria**
- [ ] Opening any admin route without a session triggers one "get current admin" call and then a full-page navigation to `/oauth2/authorization/google`.
- [ ] With a valid session, the admin routes load and the current admin's name and email are available as a signal.
- [ ] The guard does not call the backend again on every navigation inside the admin area once the admin is known.
- [ ] Errors other than 401 (for example 5xx) show an error page with a retry action instead of a login loop.
- [ ] Unit tests cover: known admin, 401 redirect (navigation abstracted behind an injectable so it can be tested), server error.

**Technical notes**
- Full-page navigation via an injectable wrapper around `window.location` (testable, SSR-safe).
- Mock-first with `HttpTestingController`; real login is verified in FE-26 with the test OIDC provider.
- Behaviour of `/api/v1/admin/me` for an unauthenticated call must be a plain 401, not a redirect (OP-03).

#### FE-14: Build the admin layout and the access-denied page

- **Type:** Story
- **Epic:** E5 Admin authentication
- **Priority:** High
- **Story points:** 3
- **Labels:** frontend, admin, security, ui
- **Depends on:** FE-13; BE-20 (redirect targets, see OP-02)
- **Traceability:** FR-23, FR-24, NFR-12, ADR-03, architecture Section 10.4, Section 11 (areas and routing)

**Description**
Replace the admin placeholder with the admin layout: a header showing the signed-in admin's name or email and a "Sign out" action, and an outlet for the subscriber list. Add the "access denied" page that the Core redirects to when a Google account is not on the allow-list; it lives outside the guarded tree so it does not trigger a new login.

**Acceptance criteria**
- [ ] The admin layout shows the current admin (from `AdminSessionService`) and a sign-out action, and is responsive.
- [ ] The access-denied page is reachable at the URL agreed with the backend, is lazy-loaded, is not protected by the admin guard and does not call admin APIs.
- [ ] The access-denied page says clearly that the account is not allowed to use the admin area, shows no admin data (FR-24), and offers "try another account" (starts login again) and "back to sign-up" links.
- [ ] A login that fails or is cancelled at Google ends on a readable UI page, not a backend default page (depends on OP-02).
- [ ] Component tests cover the layout and the access-denied page.

**Technical notes**
- Proposed URL: `/admin/access-denied` as an unguarded sibling of the guarded admin tree (OP-02).
- The admin area has no public navigation to it; admins open `/admin` directly.

#### FE-15: Implement admin sign-out

- **Type:** Story
- **Epic:** E5 Admin authentication
- **Priority:** High
- **Story points:** 2
- **Labels:** frontend, admin, security
- **Depends on:** FE-14, FE-07; BE-21
- **Traceability:** FR-29, NFR-05, ADR-03, architecture Section 10.4, Section 13.1

**Description**
Implement sign-out from the admin layout. The UI sends the sign-out request with the CSRF token, clears the admin state and list state, and shows the public area with a "you have been signed out" message. After sign-out, opening an admin page requires login again.

**Acceptance criteria**
- [ ] "Sign out" sends POST `/logout` with the XSRF header.
- [ ] On success, `AdminSessionService` and the subscriber list state are cleared and the user lands on the public page with a signed-out message.
- [ ] After sign-out, navigating to `/admin` starts the login flow again (verified in FE-26).
- [ ] If the sign-out request fails, the user is told and the admin state is still cleared locally.
- [ ] Unit tests cover success and failure.

**Technical notes**
- The response of `/logout` for an XHR call (204 versus redirect) must be agreed (OP-02). If the backend keeps a redirect, use a form POST with the CSRF token as a hidden field instead of `HttpClient`.

#### FE-16: Handle admin session expiry

- **Type:** Story
- **Epic:** E5 Admin authentication
- **Priority:** High
- **Story points:** 2
- **Labels:** frontend, admin, security
- **Depends on:** FE-13, FE-08; BE-13 (401 for admin API), BE-21 (session timeout)
- **Traceability:** NFR-02, FR-23, ADR-03, architecture Section 11 (guards), Section 13.1 (session inactivity timeout)

**Description**
When the session expires after inactivity (default 30 minutes), admin API calls return 401. The UI then clears the admin state, tells the admin that the session has expired, and offers to sign in again (or starts login directly). A 403 (for example a CSRF failure after expiry) is handled the same way where it indicates a lost session.

**Acceptance criteria**
- [ ] A 401 from any admin API call clears the admin state and shows a "session expired, sign in again" message with a sign-in action.
- [ ] After signing in again, the admin returns to the subscriber list.
- [ ] Unsaved work is limited to search text; the current query is restored after re-login if the return target is kept (OP-13).
- [ ] Unit tests cover 401 on list and on delete.

**Technical notes**
- Implement as an admin-scoped interceptor or in the admin API service layer, using the 401 error kind from FE-08; public pages are not affected.
- The 403-on-CSRF case needs the backend's error semantics (OP-03).

### E6 Admin subscriber management

#### FE-17: Implement the admin subscriber API and list state service

- **Type:** Task
- **Epic:** E6 Admin subscriber management
- **Priority:** High
- **Story points:** 3
- **Labels:** frontend, admin, api, state
- **Depends on:** FE-08, FE-13; BE-23, BE-24
- **Traceability:** FR-25, FR-26, FR-27, ADR-12, architecture Section 9.1, Section 9.2 (paging), Section 11 (state)

**Description**
Create a typed `AdminSubscribersApiService` (list with paging and search, delete) and a signal-based `SubscriberListStore` that holds page index, page size, query, results, total count, loading and error state. Components read signals from the store and call its actions; they do not call `HttpClient` directly.

**Acceptance criteria**
- [ ] List calls send zero-based `page`, `size` and optional `q` to the list resource; delete calls use the subscriber ID.
- [ ] The store exposes signals for items, total, page, size, query, loading and error, and actions to load, change page, change size, search and delete.
- [ ] Out-of-order responses do not overwrite newer results (latest request wins).
- [ ] After a delete, the current page is reloaded; if it becomes empty and is not the first page, the previous page is loaded.
- [ ] Unit tests cover paging, search, stale-response handling, delete and error states.

**Technical notes**
- Mock-first with `HttpTestingController`.
- Page size capped as the backend caps it (for example 100); default and options in OP-16.
- The DELETE request relies on the XSRF header from FE-07.

#### FE-18: Build the subscriber list page

- **Type:** Story
- **Epic:** E6 Admin subscriber management
- **Priority:** High
- **Story points:** 5
- **Labels:** frontend, admin, ui, accessibility
- **Depends on:** FE-06, FE-14, FE-17; BE-23
- **Traceability:** FR-25, FR-26, FR-21, NFR-04, NFR-12, CON-10, architecture Section 10.4, Section 11 (components and styling, time display)

**Description**
Build the main admin page: a Material table with paginator that lists subscribers with type (email or Slack), name or Slack label, email or masked webhook identifier, subscription date and time in CET, and status (active or inactive). It handles loading, empty, error and "no search results" states and is usable on mobile.

**Acceptance criteria**
- [ ] The table shows type, name or label, identifier (email or masked webhook, as delivered by the API), subscription date and time in CET/CEST, and status for each subscriber.
- [ ] Status is shown as text (not colour only); inactive Slack subscribers (FR-21) are clearly marked.
- [ ] The paginator shows total count and allows changing page and page size; it is keyboard accessible and labelled.
- [ ] Loading, empty ("no subscribers yet"), error (with retry) and "no match for the search" states are shown.
- [ ] Full webhook URLs never appear; the UI displays only what the API returns as masked value (NFR-04).
- [ ] On narrow screens (below a tablet breakpoint) rows switch to a stacked card layout or the table scrolls inside its container without page-level horizontal scrolling.
- [ ] Component tests cover all states and the CET formatting of the date column.

**Technical notes**
- `mat-table` with a data source fed from store signals; `mat-paginator` wired to store actions (server-side paging, not client-side).
- Table has a caption or `aria-label`; column headers are proper `th` elements.
- Display of full versus masked email is OP-04.

#### FE-19: Add subscriber search with URL state

- **Type:** Story
- **Epic:** E6 Admin subscriber management
- **Priority:** Medium
- **Story points:** 2
- **Labels:** frontend, admin, ui
- **Depends on:** FE-18; BE-23
- **Traceability:** FR-26, NFR-11, architecture Section 9.1, Section 11 (state)

**Description**
Add a labelled search field above the subscriber table that filters by part of a name, label or email through the server-side `q` parameter. Search and paging are kept in the URL query parameters so reload and browser back work.

**Acceptance criteria**
- [ ] Typing in the search field triggers a search after a short debounce; Enter searches immediately; a clear button resets the search.
- [ ] A new search resets to the first page.
- [ ] `q`, `page` and `size` are reflected in the URL; opening such a URL restores the same view.
- [ ] The number of results is announced to screen readers after a search.
- [ ] Component tests cover debounce, reset to first page and URL restore.

**Technical notes**
- Search is server-side only; the UI does not filter locally.
- Trim the query; do not send an empty `q`.

#### FE-20: Implement subscriber deletion with confirmation

- **Type:** Story
- **Epic:** E6 Admin subscriber management
- **Priority:** High
- **Story points:** 3
- **Labels:** frontend, admin, security, accessibility
- **Depends on:** FE-18; BE-24
- **Traceability:** FR-27, FR-28, NFR-05, architecture Section 10.4, Section 11 (dialog), Section 8.2

**Description**
Add a delete action per row that opens a Material confirmation dialog naming the subscriber (name or label and masked identifier). On confirm, the UI sends the delete request with the CSRF token, shows a result message and refreshes the list. Cancel leaves everything unchanged.

**Acceptance criteria**
- [ ] Each row has a "Delete" button with an accessible name that includes the subscriber's name or label.
- [ ] The confirmation dialog states that the subscriber and their personal data will be removed and cannot be restored; the default focus is on "Cancel"; Escape cancels.
- [ ] Confirm sends DELETE with the XSRF header; while it runs, the dialog shows a busy state and cannot be confirmed twice.
- [ ] After HTTP 204 the dialog closes, a snackbar (announced to screen readers) confirms the deletion, the list reloads and the subscriber is gone.
- [ ] HTTP 404 (already deleted by another admin) is treated as done, with a matching message; other errors keep the row and show an error.
- [ ] After closing the dialog, focus returns to a sensible element (the table or the next row's action).
- [ ] Component tests cover confirm, cancel, 204, 404 and error.

**Technical notes**
- `MatDialog` with a standalone dialog component; returns the user's choice.
- FR-28 (removal of personal data) and FR-30 (audit entry) are backend work; the UI only triggers the delete.

### E7 Accessibility and responsiveness

#### FE-22: Run and fix the accessibility audit of the public pages

- **Type:** Task
- **Epic:** E7 Accessibility and responsiveness
- **Priority:** Medium
- **Story points:** 3
- **Labels:** frontend, accessibility, public, testing
- **Depends on:** FE-11, FE-12, FE-21
- **Traceability:** NFR-13, NFR-12, architecture Section 11 (accessibility), Section 13.6 (end-to-end with axe)

**Description**
Check both public sign-up pages against WCAG 2.1 AA with automated axe checks and a manual review (keyboard only, screen reader, 200 % zoom and 320 px reflow), and fix what is found. Automated axe checks become part of the end-to-end suite so regressions are caught.

**Acceptance criteria**
- [ ] Playwright tests run axe (WCAG 2.1 A and AA rules) on the email page, the Slack page, and each page in its error and success states, with no violations.
- [ ] Manual checklist done and recorded in the pull request: keyboard-only use, visible focus, logical focus order, screen reader announcement of errors and confirmations (at least one screen reader), 200 % zoom, 320 px reflow, contrast.
- [ ] All findings are fixed or recorded as open points with a reason.

**Technical notes**
- Use `@axe-core/playwright`.
- The admin area is not bound to AA by NFR-13, but run the same axe checks there in FE-26 and fix serious issues cheaply.

#### FE-23: Verify and fix responsive layout for public and admin pages

- **Type:** Task
- **Epic:** E7 Accessibility and responsiveness
- **Priority:** High
- **Story points:** 2
- **Labels:** frontend, responsive, ui
- **Depends on:** FE-11, FE-12, FE-18, FE-20
- **Traceability:** NFR-12, architecture Section 11 (components and styling)

**Description**
Check all pages on current desktop and mobile browsers and fix layout issues. Add Playwright projects for a mobile and a desktop viewport so the main flows run on both.

**Acceptance criteria**
- [ ] The public pages, the admin list, the delete dialog and the access-denied page work at 320 px, 768 px and 1280 px widths without page-level horizontal scrolling.
- [ ] Touch targets are at least 44 by 44 CSS pixels on mobile.
- [ ] Playwright runs the mocked public and admin flows (FE-21) in a desktop Chromium, a desktop Firefox or WebKit, and a mobile viewport project.
- [ ] Findings are fixed or recorded.

**Technical notes**
- Use Material breakpoints from the CDK layout module where the layout changes (for example the table to card switch in FE-18).

### E8 Build and deployment

#### FE-24: Create the UI container image with Nginx

- **Type:** Task
- **Epic:** E8 Build and deployment
- **Priority:** High
- **Story points:** 3
- **Labels:** frontend, deployment, docker
- **Depends on:** FE-05
- **Traceability:** ADR-02, ADR-14, NFR-11, NFR-15, architecture Section 5 (UI container), Section 12.1

**Description**
Add a multi-stage Dockerfile for the UI: build the Angular production bundle with Node and NPM, then serve the static files with Nginx. Nginx serves the single-page application with a fallback to `index.html` for client routes and sets cache headers so hashed assets are cached long and `index.html` is not cached.

**Acceptance criteria**
- [ ] `docker build` of the UI produces an image that serves the production build; no Node runtime is in the final image; the container runs as a non-root user.
- [ ] Deep links such as `/slack` and `/admin` return the application (SPA fallback), while missing static assets return 404.
- [ ] Hashed assets get long-lived cache headers; `index.html` gets `no-cache`.
- [ ] Gzip (or Brotli if available) compression is on for text assets.
- [ ] The image contains no environment-specific values; the same image runs in every environment.

**Technical notes**
- Use `npm ci` in the build stage with the pinned Node LTS version.
- The Compose file that starts `ui`, `core` and `mailpit` is backend-owned (Section 12.1); coordinate the service name and ports (OP-05).

#### FE-25: Configure the Nginx reverse proxy, TLS and security headers

- **Type:** Task
- **Epic:** E8 Build and deployment
- **Priority:** High
- **Story points:** 3
- **Labels:** frontend, deployment, security
- **Depends on:** FE-24; BE-44 (Compose stack, see OP-05)
- **Traceability:** NFR-01, NFR-05, ADR-02, ADR-03, architecture Section 11 (security in the browser), Section 12.1, Section 13.1

**Description**
Extend the UI Nginx configuration so it is also the local edge: it terminates TLS for `https://localhost`, routes `/api`, `/oauth2`, `/login` and `/logout` to the Core, and sets the security headers. This gives the browser one HTTPS origin, as in the target deployment.

**Acceptance criteria**
- [ ] HTTP redirects to HTTPS; HTTPS uses a local development certificate that is not committed to the repository.
- [ ] `/api/**`, `/oauth2/**`, `/login/**` and `/logout` are proxied to the Core with `Host`, `X-Forwarded-For`, `X-Forwarded-Proto` and `X-Forwarded-Host` headers; the Google callback works through the proxy.
- [ ] Responses carry a strict Content Security Policy that allows only same-origin scripts, styles, fonts and API calls, `Strict-Transport-Security`, `X-Content-Type-Options: nosniff`, frame protection (`frame-ancestors 'none'`) and a `Referrer-Policy`.
- [ ] The application runs without CSP violations in the browser console on all pages.
- [ ] The management port of the Core is not routed through Nginx.

**Technical notes**
- Angular's inline style handling may need CSP nonces or `'unsafe-inline'` for styles; check current Angular 21 CSP guidance (`ngCspNonce` / autoCsp) through context7 and record the choice.
- Self-host fonts and icons (no Google Fonts CDN) so the CSP stays same-origin.
- Certificate creation (for example mkcert) is OP-05.

### E9 Quality and testing

#### FE-21: Set up Playwright with end-to-end tests against a mocked backend

- **Type:** Task
- **Epic:** E9 Quality and testing
- **Priority:** High
- **Story points:** 3
- **Labels:** frontend, testing, e2e
- **Depends on:** FE-11, FE-12
- **Traceability:** NFR-15, FR-01, FR-02, FR-03, FR-04, FR-05, architecture Section 11 (build and tests), Section 13.6

**Description**
Add Playwright to the workspace and write end-to-end tests for the public flows with the backend mocked by request interception. These tests run without a Core and give fast feedback on the main user paths. They also host the axe checks of FE-22.

**Acceptance criteria**
- [ ] `npm run e2e` starts the dev server and runs Playwright; `npm run e2e:ci` runs headless.
- [ ] Tests cover: email sign-up success, email validation errors, Slack sign-up success, Slack non-Slack URL rejected, Slack verification failure, network error.
- [ ] The mocks assert that POST requests carry the XSRF header.
- [ ] Tests use accessible locators (role and label), not CSS selectors.

**Technical notes**
- Mock responses follow the OpenAPI contract, including problem details.
- Admin flows with mocked `/api/v1/admin/**` can be added here as well once FE-18 to FE-20 exist; the full login flow needs the running stack (FE-26).

#### FE-26: Add end-to-end tests against the running stack

- **Type:** Task
- **Epic:** E9 Quality and testing
- **Priority:** High
- **Story points:** 5
- **Labels:** frontend, testing, e2e, admin
- **Depends on:** FE-15, FE-16, FE-19, FE-20, FE-25, FE-21; BE-16, BE-19, BE-20, BE-21, BE-22, BE-23, BE-24, BE-25, BE-44
- **Traceability:** NFR-15, FR-01, FR-03, FR-05, FR-23, FR-24, FR-25, FR-26, FR-27, FR-29, NFR-02, NFR-05, NFR-13, architecture Section 10.1, Section 10.2, Section 10.4, Section 13.6

**Description**
Write Playwright tests that run against the real Core in its test profile (Google login replaced by a test OIDC provider, Slack stubbed). They cover the main flows end to end, including real CSRF handling and the session cookie, which mocks cannot prove.

**Acceptance criteria**
- [ ] Email sign-up and duplicate sign-up both show the same confirmation, and only one subscriber appears in the admin list.
- [ ] Slack sign-up with the stubbed webhook succeeds; the admin list shows only the masked webhook.
- [ ] Opening `/admin` without a session goes through the test OIDC login and shows the list; a non-allow-listed test user ends on the access-denied page with no admin data.
- [ ] Search filters the list; paging works with more than one page of seeded subscribers.
- [ ] Delete with confirmation removes the subscriber from the list.
- [ ] Sign-out ends the session: opening `/admin` afterwards starts login again.
- [ ] Expired session (simulated by deleting the session cookie or a short timeout in the test profile) shows the session-expired message.
- [ ] axe checks run on the admin pages; serious violations are fixed or recorded.
- [ ] The procedure to start the stack for these tests is documented (FE-28).

**Technical notes**
- Choice of test OIDC provider and Slack stub is backend or test-infrastructure work (OP-06).
- Run through the Nginx edge (FE-25) or the dev-server proxy; both must work.

#### FE-27: Spike on generating API types from the OpenAPI contract

- **Type:** Spike
- **Epic:** E9 Quality and testing
- **Priority:** Low
- **Story points:** 1
- **Labels:** frontend, api, tooling
- **Depends on:** FE-07; BE-47
- **Traceability:** ADR-13, NFR-15, architecture Section 11 (API client)

**Description**
Evaluate generating the TypeScript request and response types (types only, no generated services) from the backend's OpenAPI document, and decide whether the hand-written models of FE-07 should be replaced. Timeboxed to one day.

**Acceptance criteria**
- [ ] At least one generator (for example `openapi-typescript`) is tried against the real OpenAPI document.
- [ ] A short recommendation (adopt or not, with how the contract is fetched in the build) is written in the pull request or task comment.
- [ ] If adopted, a follow-up task is created; no production code changes in the spike.

**Technical notes**
- Keep the typed service layer hand-written in any case; generated types only replace `api` model files.

#### FE-28: Document the UI build, run and test procedure

- **Type:** Task
- **Epic:** E9 Quality and testing
- **Priority:** High
- **Story points:** 2
- **Labels:** frontend, documentation
- **Depends on:** FE-03, FE-24, FE-25, FE-21
- **Traceability:** NFR-15, architecture Section 15.2 (NFR-15: build and run procedure documented in the implementation phase)

**Description**
Write the UI part of the build and run documentation: prerequisites (Node LTS, NPM), install, dev server with proxy, lint, unit tests, end-to-end tests (mocked and against the stack), production build and container image. Update the "build / run / test commands" placeholder in `CLAUDE.md` if the user agrees.

**Acceptance criteria**
- [ ] `ui/README.md` describes all commands and prerequisites, and a new developer can run the UI against a local Core by following it.
- [ ] The document states which backend profile and Compose variant are needed for FE-26.
- [ ] Commands in the document are the NPM scripts that exist; they are checked by running them once.

**Technical notes**
- Coordinate with the backend's documentation task so there is one top-level "how to run" entry.

## 8. Requirements coverage

Must (M) and Should (S) requirements, plus the constraints and assumptions the UI must respect. Every Must and Should requirement is covered by at least one task.

| Requirement | Priority | Backend tasks | UI tasks |
|---|---|---|---|
| FR-01 | M | BE-07, BE-14, BE-15, BE-16 (UI page: UI plan) | FE-09, FE-11, FE-21, FE-26 |
| FR-02 | M | BE-12, BE-15, BE-16 | FE-08, FE-10, FE-11, FE-21 |
| FR-03 | M | BE-07, BE-14, BE-18, BE-19 | FE-09, FE-12, FE-21, FE-26 |
| FR-04 | M/S | BE-18, BE-19 | FE-08, FE-12, FE-21 |
| FR-05 | M | BE-07, BE-09, BE-14, BE-16, BE-19 | FE-09, FE-11, FE-12, FE-26 |
| FR-08 | M | BE-28, BE-29, BE-46 | - |
| FR-09 | M/S | BE-08, BE-10, BE-28, BE-29 | - |
| FR-10 | M | BE-08, BE-28, BE-46 | - |
| FR-11 | M | BE-26 | - |
| FR-12 | M | BE-17, BE-28, BE-29, BE-46 | - |
| FR-13 | M | BE-03, BE-10, BE-11, BE-27, BE-32 | - |
| FR-14 | M | BE-30, BE-31, BE-46 | - |
| FR-15 | M | BE-33, BE-37, BE-46 | - |
| FR-16 | M | BE-34, BE-46 | - |
| FR-17 | M | BE-33, BE-34, BE-46 | - |
| FR-18 | M | BE-03, BE-10, BE-11, BE-32 | - |
| FR-19 | M | BE-31, BE-46 | - |
| FR-20 | M | BE-08, BE-30, BE-31, BE-46 | - |
| FR-21 | S | BE-31, BE-34, BE-23, BE-46 | FE-18 (inactive status shown in the list; marking is backend) |
| FR-23 | M | BE-13, BE-20, BE-22, BE-25 (redirect in the UI guard: UI plan) | FE-13, FE-14, FE-16, FE-26 |
| FR-24 | M | BE-20, BE-25 | FE-14, FE-26 |
| FR-25 | M | BE-23 | FE-06, FE-17, FE-18, FE-26 |
| FR-26 | S | BE-07, BE-23 | FE-17, FE-18, FE-19, FE-26 |
| FR-27 | M | BE-24 (confirmation dialog: UI plan) | FE-17, FE-20, FE-26 |
| FR-28 | M | BE-08, BE-24 | FE-20 (trigger only; data removal is backend) |
| FR-29 | M | BE-21 | FE-15, FE-26 |
| FR-30 | S | BE-07, BE-24 | - |
| FR-31 | M | BE-04, BE-26 | - |
| FR-32 | M | BE-26 | - |
| FR-33 | M | BE-04, BE-11, BE-27, BE-29 | - |
| FR-34 | S | BE-26, BE-39 | - |
| FR-35 | M | BE-35, BE-36, BE-48 | - |
| NFR-01 | M | BE-13 (forwarded headers, secure cookies), BE-44 (TLS, HSTS at the edge) | FE-25 |
| NFR-02 | M | BE-20, BE-21 | FE-13, FE-16, FE-26 |
| NFR-03 | M | BE-04, BE-09, BE-29, BE-37, BE-43, BE-44 | - |
| NFR-04 | M | BE-09, BE-18, BE-23, BE-40 | FE-12, FE-18 (masked display only; encryption and masking are backend) |
| NFR-05 | M | BE-12, BE-13, BE-15, BE-16, BE-18, BE-19 (output encoding in the browser: UI plan) | FE-07, FE-08, FE-10, FE-11, FE-12, FE-15, FE-20, FE-25, FE-26 |
| NFR-07 | M | BE-28, BE-31, BE-46 | - |
| NFR-08 | M | BE-17, BE-29, BE-31, BE-35 | - |
| NFR-09 | S | BE-28, BE-38, BE-41 (partial by design: AQ-05) | - |
| NFR-10 | S | BE-30, BE-31, BE-36, BE-48 (partial for email volume: ARSK-02) | - |
| NFR-11 | S | BE-07, BE-23 (page response times also depend on the UI plan) | FE-01, FE-05, FE-19, FE-24 |
| NFR-12 | M | - | FE-04, FE-05, FE-11, FE-12, FE-14, FE-18, FE-23 |
| NFR-13 | S | - | FE-02, FE-04, FE-05, FE-10, FE-11, FE-12, FE-22, FE-26 |
| NFR-14 | M | BE-01, BE-03, BE-10, BE-11, BE-32 | FE-05 (a new subscriber type is a new route and form only) |
| NFR-15 | M | BE-01, BE-25, BE-46, BE-47, BE-49 | FE-01, FE-02, FE-21, FE-26, FE-27, FE-28 |
| NFR-16 | M | BE-04, BE-26, BE-40, BE-41 | - |
| NFR-17 | S | BE-05, BE-39, BE-41 | - |
| NFR-18 | M | BE-04, BE-26, BE-29, BE-17, BE-42 | - |
| NFR-19 | S | BE-37 (partial: SPF, DKIM, DMARC and SES production access are operator setup, not provisioned for the local demo, AQ-07) | - |
| NFR-20 | M | BE-26, BE-36, BE-45 | - |
| CON-02 | - | - | FE-13 |
| CON-04 | - | - | FE-12 |
| CON-10 / ASM-03 | - | - | FE-06, FE-18 |
| ASM-09 | - | - | FE-06 |

Partial coverage, accepted in the architecture: NFR-09 (NewsAPI.org returns only current headlines, AQ-05), NFR-10 for email volume (SES quota, ARSK-02), NFR-19 (sender authentication is operator setup; the local demo does not use SES, AQ-07). FR-30, FR-34 and NFR-17 have no UI part by design (Q-07, ADR-16).

## 9. Open points for the user

| ID | Open point | Affects | Proposed default                                                                                                                                                                                                                                                                    |
|---|---|---|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| OP-01 | **CSRF cookie before the first POST.** Nginx serves the UI, so the browser has no `XSRF-TOKEN` cookie before its first state-changing request, and Spring Security creates the token lazily. Without a fix, the first sign-up fails with 403. The architecture lists no endpoint for this. | BE-13, FE-07 | `GET /api/v1/csrf` returns 204 and sets the cookie (planned in BE-13); the UI calls it once at start-up (FE-07).                                                                                                                                                                    |
| OP-02 | **Login redirects and logout response.** The architecture does not name where the Core sends the browser after login success, allow-list refusal or a failed login, or what `POST /logout` returns. | BE-20, BE-21, FE-14, FE-15 | Success to `/admin`; refusal and failure to `/admin/access-denied` (unguarded UI route) with a flag that tells them apart; `POST /logout` returns 204 and the UI navigates.                                                                                                         |
| OP-03 | **Error semantics for the admin API.** | BE-13, FE-16 | Unauthenticated `/api/**` calls get a plain 401 (no redirect); a CSRF failure gets a 403 problem detail with its own type; the UI treats both as "session expired" in the admin area.                                                                                               |
| OP-04 | **Email addresses in the admin list.** FR-25 mentions a masked identifier; the architecture masks only webhook URLs. | BE-23, FE-18 | Admins see the full email (they search by it); webhook URLs stay masked.                                                                                                                                                                                                            |
| OP-05 | **Ownership of the Nginx edge.** Both plans claim the proxy routes, TLS and security headers (BE-44 and FE-25). | BE-44, FE-24, FE-25 | The UI plan owns the Nginx image and its configuration (FE-24, FE-25); BE-44 owns only the Compose files. Local TLS certificate from a mkcert or self-signed script outside the repository, documented in FE-28. BE-44's description should be narrowed accordingly once confirmed. |
| OP-06 | **Test login provider and Slack stub for end-to-end tests.** | BE-25, FE-26 | A mock OAuth2/OIDC server container in a Compose override for the `e2e` profile; WireMock for Slack.                                                                                                                                                                                |
| OP-07 | **CI platform.** The architecture says the contract is "checked in CI", but no CI is decided, so no CI task exists. | BE-47, FE-28 | All checks run in `./gradlew build` and the NPM scripts.                                                                                                                                                                                                                            |
| OP-08 | **Gradle DSL, group ID and base Java package.** | BE-01 | Kotlin DSL; one base package for all modules (use com.sonrisa.alerting).                                                                                                                                                                                                            |
| OP-09 | **Recipients of the log stub channel.** A channel only receives notifications if some subscriber type uses it. | BE-32 | `channel-log` brings a `log` subscriber type with no public endpoint; tests create log subscribers through the subscription service.                                                                                                                                                |
| OP-10 | **Missed-run handling.** The run record has no field for a gap, and it is unclear whether a catch-up run starts at start-up. | BE-38 | Start a catch-up run after start-up when the last successful run is older than one interval; record the gap in the run's error summary.                                                                                                                                             |
| OP-11 | **Pending notifications when a subscriber is deleted.** Architecture Section 6.1 says "cancel", Section 8.2 says "delete". | BE-24 | Delete them, as in Section 8.2.                                                                                                                                                                                                                                                     |
| OP-12 | **API contract details the UI mirrors** (field names, paging fields, status values, maximum lengths, the Slack webhook pattern). | FE-07, FE-27 | Taken from BE-12 and the OpenAPI contract (BE-47); the UI starts with hand-written types.                                                                                                                                                                                           |
| OP-13 | **Return to the original admin URL after (re-)login.** | FE-13, FE-14 | Always land on `/admin`; not planned.                                                                                                                                                                                                                                               |
| OP-14 | **Where UI texts live.** The architecture says templates; the frontend skill asks for one place. | FE-06 | Static texts in templates; messages produced in TypeScript in one catalogue file.                                                                                                                                                                                                   |
| OP-15 | **Display time zone in the UI.** | FE-06, FE-18 | UI configuration constant `Europe/Budapest` (AQ-02).                                                                                                                                                                                                                                |
| OP-16 | **Admin list page size.** | FE-18, BE-23 | Default 25; options 10, 25, 50, 100 (backend cap 100).                                                                                                                                                                                                                              |
| OP-17 | **Visual identity.** | FE-04 | Angular Material default palette adjusted for AA contrast; product name "Alerting System".                                                                                                                                                                                          |

### 9.1 Decisions made during implementation

All open points above are closed: their "Proposed default" is the decision. The points below came up while implementing milestone M2 and were decided by the user on 2026-09-30. They are binding for the later milestones.

| ID | Decision | Affects |
|---|---|---|
| OP-18 | **HTTP clients in plugin modules.** Plugin modules get Spring Boot on their classpath and build their own `RestClient` with connect and read timeouts from their own configuration group (`alerting.sources.<key>.*`, `alerting.channels.<key>.*`). The global timeouts (`spring.http.clients`) remain the safety net, and the ArchUnit rule against clients without timeouts applies to plugin code too. | BE-18, BE-29, BE-33, BE-34, BE-37 |
| OP-19 | **Event retention and notification history.** The foreign key from NOTIFICATION to EVENT becomes `ON DELETE SET NULL`, so `event_id` is nullable: events are deleted after 30 days while their notifications are kept for 90 days. The change is made with the retention job. | BE-42 |
| OP-20 | **Schema deviations from architecture Section 8.1 are accepted:** `EVENT.source_name` (human-readable source, FR-09, FR-17); `NOTIFICATION.created_at` (for retention); `RUN.run_trigger` and `AUDIT_ENTRY.performed_at` instead of the SQL keywords `trigger` and `at`; `EVENT.content` limited to 4,000 characters; RUN_SOURCE_RESULT status values `COMPLETED` and `FAILED`. The address fingerprint does not normalise: the subscriber type passes an already normalised address. | BE-08 (done), BE-14, BE-15, BE-18, BE-28 |
| OP-21 | **Subscriber type without an active channel.** Start-up fails with a report naming the property to fix; the application does not start with a type whose channel is missing or disabled. | BE-11 (done), all plugin tasks |
| OP-22 | **Plugins are off unless enabled.** `alerting.sources.<key>.enabled` and `alerting.channels.<key>.enabled` default to `false`. Each plugin task sets its flag in the profiles where it runs: `local` and `demo` use the stub source, email (Mailpit), Slack and the log channel; `test` enables only what a test needs; NewsAPI.org only where an API key is configured. | BE-27, BE-29, BE-32, BE-33, BE-34, BE-44 |
| OP-23 | **Source failures are a checked exception.** `EventSource.fetchNewItems` throws `EventSourceException` with the kind `TRANSIENT` or `PERMANENT`; channels return a `DeliveryResult`. An unexpected runtime exception from a source or a channel is treated as transient by the collection and delivery services. | BE-27, BE-28, BE-29, BE-31 |
| OP-24 | **API conventions of BE-12 and BE-13 are accepted:** problem types are URNs (`urn:alerting:problem:<slug>`); framework error texts are replaced by fixed texts; HSTS is set by the edge, not the Core; forwarded headers are trusted only from private-network and local proxies (the edge must run in such a network); a `Retry-After` above `alerting.resilience.max-retry-after` (default 60 s) ends the retries of that call. | BE-16, BE-19, BE-44, all integrations |
| OP-25 | **UI behaviour of FE-07, FE-08 and FE-10 is accepted as built:** the start-up CSRF call does not block rendering; a CSRF 403 is reported without an automatic retry; an identical message shown twice in a row is not announced again by screen readers. | FE-07, FE-08, FE-10, FE-11, FE-12 |
| OP-26 | **Accepted limitations:** the `aws` profile has no datasource yet (to be defined with the cloud target); Safari rejects `Secure` cookies on `http://localhost`, so local development uses Chrome or Firefox, or the HTTPS edge. | BE-44, FE-28 |
| OP-27 | **Sign-up for a disabled plugin.** When the email or Slack plugin is disabled by configuration, its sign-up endpoint answers 404 `urn:alerting:problem:not-found`, whatever the request body holds. The endpoints stay in the OpenAPI document in every configuration. | BE-16 (done), BE-19 (done), FE-11, FE-12 |
| OP-28 | **Re-subscribing an inactive subscriber.** A subscriber marked INACTIVE after a permanent delivery error (FR-21) who signs up again is verified again (for Slack, a new welcome message). When the verification passes, the subscriber becomes ACTIVE; when it fails, nothing changes and the answer is 422 `webhook-not-verified`. An ACTIVE subscriber is never verified again. Implemented after M3 in the subscription service. | BE-14 (done), BE-31, BE-34 |
| OP-29 | **Slack 400 answers stay permanent.** Slack's 400 errors (`invalid_payload`, `no_text`, `user_not_found`) are permanent failures, like 403, 404 and 410. In the Slack channel this marks the subscriber INACTIVE (FR-21), even when the payload fault is on our side; the channel's message rendering must therefore be covered well by tests. | BE-18 (done), BE-31, BE-34 |
| OP-30 | **Uniqueness across subscriber types (note for later).** The address fingerprint covers the normalised address only, so uniqueness is shared by all subscriber types (ADR-09). A future type whose addresses can overlap another type's, for example a `log` type (OP-09) using email addresses, would get "accepted" with nothing stored. Kept for now; revisit when such a type is added (for example by including the type key in the fingerprint). | BE-14 (done), BE-32, future types |
| OP-31 | **Accepted details of M3:** email local parts are lower-cased; internationalised domains are stored and shown in Punycode (`xn--`); request length limits count the raw input before trimming; the Slack webhook pattern is described in OpenAPI text, not as a formal pattern; two simultaneous sign-ups of the same new webhook may each send a welcome message; the 202 confirmation text is "Thank you. Your subscription has been received."; axe accessibility checks come with FE-22. | BE-15, BE-16, BE-18, BE-19, FE-12, FE-22 |
