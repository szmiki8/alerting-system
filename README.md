# Alerting System

An alerting web application built as a demonstration of AI-assisted software development: from a vague brief, through requirements and architecture, to a tested implementation. The documentation of the process matters as much as the product.

**Start here: [`docs/00-project-documentation.md`](docs/00-project-documentation.md).** It holds the original brief, the plan of the project and the record of every step, including the prompts given to Claude Code and their results.

## Repository layout

| Path | Content |
|---|---|
| [`docs/`](docs/) | The numbered process documents, read in order: `00` project documentation (entry point), `01` requirements specification, `02` system architecture, `03` implementation details (task breakdown, milestones and decisions). |
| [`core/`](core/) | The backend: a Spring Boot 4 application on Java 17, built with Gradle. Modules: `alerting-spi` (extension interfaces), `alerting-app` (the application) and the plugin modules `source-*` and `channel-*`. |
| [`ui/`](ui/) | The frontend: an Angular 21 single-page application with Angular Material, built with NPM. See [`ui/README.md`](ui/README.md) for local development. |
| [`.claude/`](.claude/) | Claude Code configuration. `skills/` holds the role skills used in the project: `business-analyst`, `system-architect`, `backend-engineer` and `frontend-engineer`. |
| [`CLAUDE.md`](CLAUDE.md) | Instructions for Claude Code: current phase, working agreements, tech stack, build/run/test commands and key directories. |

## Quick start

- Backend, from `core/`: `./gradlew build` (tests need Docker), then `./gradlew :alerting-app:bootRun`.
- Frontend, from `ui/`: `npm ci`, `npm run check`, then `npm start`.

Details, profiles and environment variables are in [`CLAUDE.md`](CLAUDE.md) and [`core/.env.example`](core/.env.example).
