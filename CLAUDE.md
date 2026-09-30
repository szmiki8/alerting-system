# Alerting System — SDLC demonstration

A project that demonstrates AI assisted software development, starting from a vague
brief (see `docs/00-project-overview.md`). The documentation of the process
matters as much as the final product.

## Current phase
Requirements discovery (steps 2–3 of the plan in `docs/00-project-overview.md`).
No tech stack is chosen and no code exists yet. Don't scaffold code or assume a
language or framework until the user has decided on one.

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

## To be filled in later (steps 5–6)
- Tech stack and versions
- Build / run / test commands
- Architecture overview and key directories
