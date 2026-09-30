# Alerting System — SDLC demonstration

A project that demonstrates AI assisted software development, starting from a vague
brief (see `docs/00-project-documentation.md`). The documentation of the process
matters as much as the final product.

## Current phase
Implementation (step 7 of the plan in `docs/00-project-documentation.md`), following the
task breakdown in `docs/03-implementation-details.md`. Milestone M1 (runnable skeletons,
BE-01 to BE-05 and FE-01 to FE-05) is done; M2 is next. The requirements, the
architecture (`docs/02-system-architecture.md`, all ADRs accepted) and the open points in
Section 9 of `docs/03-implementation-details.md` are closed decisions.

- Implement tasks with the `backend-engineer` and `frontend-engineer` skills, one task at
  a time, in ID order, within the milestone the user asks for.
- Keep the build green after every task. Don't commit unless the user asks.

## Working agreements
- The user makes the design decisions. When there are options, lay out the
  trade-offs and give a recommendation, but don't pick silently.
- The user checks every implementation detail, so keep changes small and
  easy to review, and explain any non-obvious choices.
- For questions about external APIs or libraries (NewsAPI, Slack API, market
  data providers, etc.), look up current docs through context7 or the web
  instead of answering from memory, and say where the information came from.
- Keep answers to discovery questions concise. They may be quoted directly
  in the docs.

## Documentation
- `docs/` holds the numbered process docs (`00-…`, `01-…`). New phases get
  a new numbered file.
- Q&A with Claude is recorded in the docs as `**Claude > <question>**` followed by
  the quoted answer.
- Write in English, in plain prose, and mark decisions and open questions explicitly.

## Tech stack
Versions are pinned; see `core/docs/library-versions.md` for the backend checks and sources.

- **Core** (`core/`): Java 17 (Gradle toolchain, auto-provisioned), Spring Boot 4.1.1,
  Gradle 9.8 (wrapper, Kotlin DSL, version catalog `core/gradle/libs.versions.toml`,
  convention plugins in `core/buildSrc`). Base package `com.sonrisa.alerting`.
  Tests: JUnit 6, ArchUnit, Testcontainers (PostgreSQL), `wiremock-standalone`, GreenMail.
- **UI** (`ui/`): Angular 21.2 (standalone, zoneless, strict), Angular Material and CDK 21.2,
  TypeScript 5.9, Vitest, ESLint + Prettier, self-hosted Roboto (`@fontsource/roboto`).
  Node 22 or 24 (`ui/.nvmrc`), exact versions (`ui/.npmrc`).

## Build / run / test commands
There is no CI (OP-07); these commands are the checks.

- **Core**, from `core/`:
  - `./gradlew build`: compile, all tests (unit, ArchUnit, library smoke tests).
  - `./gradlew :alerting-app:bootRun`: start on port 8080 with the `local` profile;
    Actuator on the management port 8081.
  - `./gradlew --stop`: stop the Gradle daemons afterwards.
  - Configuration: environment variables listed in `core/.env.example`. The `demo`,
    `postgres` and `aws` profiles refuse to start without
    `ALERTING_MANAGEMENT_OPERATOR_PASSWORD`.
- **UI**, from `ui/`:
  - `npm ci`, then `npm run check`: lint, format check, build, contrast check,
    tests with coverage.
  - `npm start`: dev server on port 4200, proxying `/api`, `/oauth2`, `/login` and
    `/logout` to the Core (`CORE_URL`, default `http://localhost:8080`).
  - `npm test` runs once; `npm run test:watch` watches.

## Architecture overview and key directories
Details in `docs/02-system-architecture.md`.

- `core/alerting-spi`: extension interfaces for sources, subscriber types and channels.
- `core/alerting-app`: the Spring Boot application, packaged by feature
  (`com.sonrisa.alerting.app.<feature>`).
- `core/source-*`, `core/channel-*`: plugin modules. They depend only on `alerting-spi`
  and are runtime-only dependencies of the app; ArchUnit tests enforce this.
- `ui/src/app`: `public/` (sign-up pages), `admin/` (lazy-loaded admin area), `core/`
  (app-wide services such as the title strategy), `shared/` (reusable UI parts).
- `deploy/` (later milestones): Dockerfiles and Docker Compose.
