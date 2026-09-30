# Alerting System - SDLC demonstration

## Project Overview

The aim of the project is to demonstrate a software development methodology based on real requirements and fictional details.

The prompts given to Claude Code during the process are also recorded in this documentation. Each prompt starts with the string "Claude > ", and the result produced by Claude Code follows it.

This document in sequence guides the reader through the process of implementing the project.

## Initial requirements

_"We want users to be able to set up alerts so they get notified when
something important happens in the world — like breaking news, market
movements, natural disasters, that kind of thing. Should work for both email
and Slack. Make it flexible enough that we can add more channels later.
We need an admin view too."_

## Planned steps for project implementation
_(These are subject to change during the process.)_

1. Define initial toolset
2. Understand vague brief
3. Fleshing out vague details to form a complete project picture
4. Define deliverables, set up scope
5. Design system architecture and set constraints
6. Implement system
7. Evaluate and test product

## 1. Define initial toolset

I will use Claude Code to assist with development at every stage of the project. Implementation will be based on my own ideas, though I will also seek the agent's input on them. I will entrust the implementation details to the agent but will verify them in every instance.

_(Claude Code is already set up on my workstation with the following MCPs: context7, grep-mcp)._

## 2. Understand vague brief

I start with the description outlined in the initial requirements. In a real-world project, I would clarify the details iteratively with the product manager, business analyst or directly with the client, leveraging my own knowledge and experience to achieve the goal. In this case, however, I will define the details based on my own vision.

Questions arising from the incomplete requirements:

- What defines "something important"? Is it important in generic or is it important based on user preferences?
- Where do events come from?
- What are users, what kind of system is imagined?
- What is the primary function of the admin view? Who can be an admin?
- What are the exact ways of delivering messages via Slack?
- ...

**Claude > Summarize briefly how a message can be delivered to a user via Slack.** 

"There are four main ways to deliver a message to a user through Slack:

1. Incoming webhooks. The user or workspace admin installs a Slack app and picks a channel. Your system gets a unique URL and POSTs a JSON payload to it. This is the simplest option: no OAuth token handling, one-way only, and each webhook is locked to a single channel. It works well for "post alerts to
   #my-alerts".

2. Bot token with chat.postMessage

_(output cut...)_"

**Claude > What sources can be used to gather information besides RSS feeds and web crawlers?**

"The main sources are these, grouped by the three examples in the brief plus general options:

1. News APIs (breaking news)
- Aggregators: NewsAPI.org, GNews, Mediastack, NewsData.io. These return articles by keyword, category, country or language.
- Wire and publisher APIs: AP, Reuters (via Refinitiv/LSEG), The Guardian Open Platform, NYT APIs.
- Event databases: GDELT monitors global news and publishes structured, geo-tagged events every 15 minutes, free of charge. Event Registry is a paid alternative.

2. Financial market data (market movements)
- REST and WebSocket APIs: Alpha Vantage, Finnhub, Polygon.io, Twelve Data, IEX Cloud, Yahoo Finance (unofficial). WebSocket streams give real-time price ticks, which suits threshold alerts like "BTC drops 5%".
- Crypto exchanges: Binance, Coinbase and Kraken offer public WebSocket feeds.
- Official data: SEC EDGAR for company filings, and central bank or statistics APIs such as FRED and ECB for rate decisions.
  
_(output cut...)_"

**Claude > Starting with NewAPI.org, NewsData.io, is there a way to retrieve news from general importance perspective or user preferences?**

"Yes, both APIs support both. Neither returns a true "importance score", though. General importance is approximated by editorial curation and source prestige, and user preferences are expressed as query filters.

NewsAPI.org

General importance
- /v2/top-headlines: breaking headlines filtered by country, category (business, entertainment, general, health, science, sports, technology) or sources. This is the closest thing to "what's important right now", because it's based on what publishers put on their front pages.
- /v2/everything with sortBy=popularity: puts articles from prominent sources first. The other options are relevancy and publishedAt.

User preferences
- q: keyword search with boolean syntax: "exact phrase", +must, -exclude, AND/OR/NOT, parentheses, up to 500 characters. One user's alert rule can map directly to one query, for example (earthquake OR tsunami) AND Japan.
- Other filters:
   - searchIn limits matching to the title"
_(output cut...)_"

## 3. Fleshing out vague details to form a complete project picture

Based on Section 2. "Understanding the vague brief" and my previous experience, I am making the following decisions to clarify the requirements.

- The system meeting the requirements will be implemented as a web application based on a classic client-server architecture.
- A user can be anyone who:
  - has access to the domain, is capable of using a web browser, and possesses an email address
  - has access to a Slack channel (as a user or administrator) and is able to create a Webhook URL
- Designated administrators access the system's administration interface using Google social login.
- Users can sign up via the web interface using their name and email address to receive information about important news and events.
- Users can subscribe a channel by providing a Slack Webhook URL to receive notifications about important events and information on that channel.
- Important events must be stored in a standard format (date and time, title, content, source); they must be collected at regular, configurable intervals and subsequently forwarded to subscribed users.
- The initial source of information is NewsAPI.org headlines. The list of sources may be expanded in the future to include services such as NewsAPI.org, Alpha Vantage, USGS Earthquake API, X/Twitter API, etc.
- After authenticating, administrators can list and delete subscribers.
- New events are collected every hour. Right after each collection, every new event is sent to subscribers as a separate message, one by one (there is no digest). The collection interval can be modified by the system operator. The system uses the CET time zone.

Out of scope for now but maybe implemented later in the future:

- There is no need to manage user preferences, such as news categories. There is no need for an unsubscribe function.

**Claude > Create a Business Analyst skill to refine, clarify and create requirement specification for a web app which has initial specification in docs/00-project-overview.md. Wait for my confirmation after and spawn and agent to use that skill to refine the requirements to docs/01-requirements-specification.md. Don't go to low level details but use high level words like REST etc.** 

"I've created the Business Analyst skill at .claude/skills/business-analyst/SKILL.md. It isn't used yet; I'm waiting for your go-ahead.

How it reads your brief:
- Your decisions in Section 3 are binding and override the original quote.
- The Claude > … Q&A blocks count only as background research, never as decisions.
- Items you marked out of scope (preferences, unsubscribe) are listed as out of scope and not specified.
- Where the brief is unclear, it states assumptions (ASM-xx) or open questions (Q-xx), each with a proposed default, and doesn't invent features.
- It flags conflicts as risks rather than changing your decisions. For example, keeping users' email addresses with no way to unsubscribe raises a GDPR concern.

Level of detail: it stays high level. It may name REST/JSON, OAuth / Google login, scheduled jobs, Slack webhooks and HTTPS. It must not include endpoints, database schemas, specific libraries or cron syntax.

Output structure (in docs/01-requirements-specification.md):
- Introduction and glossary
- User roles and scope
- Assumptions and constraints
- A system context diagram (Mermaid)
- Functional requirements (FR-xx) with priority, acceptance criterion and source
- Main user flows and data entities
- Non-functional requirements (NFR-xx), external interfaces and risks
- Open questions and a table tracing each brief statement to its requirements

When it finishes, it reports how many requirements, assumptions, open questions and risks it produced, plus the most important conflicts it found."

Claude > Yes, spawn the agent."

"Agent "Write requirements specification" finished · 2m 2s

"The agent has written docs/01-requirements-specification.md (324 lines). It follows all 14 sections of the skill's structure, and a scan found no endpoint paths, schemas or cron syntax. It hasn't been committed.

Contents: 34 functional requirements, 19 non-functional requirements, 10 assumptions, 8 constraints, 11 open questions and 9 risks. It also has a Mermaid context diagram and a table tracing brief statements to requirements. Your Section 3 decisions are kept, and user preferences, unsubscribe, extra
sources and extra channels are listed as out of scope.

Most important issues it found in the brief:
1. No unsubscribe conflicts with GDPR and anti-spam rules. The spec keeps your decision. To reduce the risk, it adds a privacy notice at sign-up, a removal contact in every email, and full deletion of personal data when an admin deletes a subscriber.
2. No email confirmation at sign-up, so anyone can subscribe someone else's address. It proposes no confirmation step for the demo, plus rate limiting or a bot check.
3. "Alerts" and "breaking news" vs. a once-a-day digest. Users may expect timely alerts, and the brief doesn't say when or in which time zone the digest is sent. The proposed default is 3:00 AM UTC, configurable.
4. What "important" means. It currently means NewsAPI.org's top headlines, but the country, category and language filters still need choosing. Market movements and disasters from the original quote aren't covered in the first release.
5. NewsAPI.org plan limits. The agent left this as an open question to check, but we already confirmed it earlier from their pricing page. The free Developer plan is for development only: it can't be used in staging or production, articles are delayed 24 hours, and it allows 100 requests per day. That's
   fine for the demo; a real deployment needs a paid plan."

Claude > I've updated the Proposed defaults in Section 13. of 01-requirements-specification.md. Based on those, re-run the agent to make the necessary changes. I've reviewed the open questions, they are fine as is.

"I've updated both documents to match your answers. Neither change is committed.

docs/00-project-overview.md
- In Section 3, the daily midnight–3 AM rule now says: events are collected every hour, each new event is sent as a separate message right after collection, the interval is configurable, and the system uses CET.
- I didn't touch the rest of the file.

docs/01-requirements-specification.md (now version 0.3)
- Digest removed: the question text of Q-04, Q-05, Q-09 and Q-10 no longer mentions a digest. Your defaults are unchanged, except that Q-05 now ends with "CET time zone."
- CET:
    - New constraint CON-10 makes CET the time zone for the schedule, for messages and for the admin UI.
    - Notifications show times in CET (FR-17), and the time zone is no longer a configuration setting (NFR-18).
    - ASM-03 now says CET includes the summer-time switch to CEST. Tell me if you want fixed CET all year instead.
- Hourly schedule, events one by one: CON-06 and the traceability row for Decision 9 now point to the updated brief, not to "the operator's override".
- Privacy and spam risk accepted (demo):
    - Added CON-11, which records that privacy and anti-spam compliance are not goals of this release.
    - Removed:
        - FR-06: the privacy notice at sign-up.
        - FR-07: the bot and rate-limit protection on the sign-up forms.
        - FR-22: the removal contact in each email.
        - NFR-06: the GDPR requirement.
        - RSK-01, RSK-02 and RSK-10: the privacy, spam and message-volume risks.
    - NFR-19 (email sender authentication) is back to Should.
    - Admin deletion still removes the subscriber's data (FR-28), because that is part of your delete decision.
- Outdated events accepted:
    - Added CON-12, which says events may be outdated in the demo.
    - Removed RSK-03, the breaking-news gap.
    - The NewsAPI.org row in Section 11 notes the 24-hour delay is accepted.
- IDs: removed IDs are not reused, and the change history lists them.
- New counts: 32 FR, 19 NFR, 12 CON and 6 RSK."

## 4. Define deliverables, set up scope

Deliverables for this project are:
- docs/00-project-documentation.md: detailed information about the project implementation, evolution
- docs/01-requirements-specification.md: refined requirements specification of the project
- docs/02-system-architecture.md: architecture document of the project
- docs/03-implementation-details.md: detailed implementation steps of the project with acceptance criteria
- implementation files (Core application, UI application, Docker Compose file to run the system locally)
- .claude: skill definitions used in the project

## 5. Define system architecture and constraints

Since I am most up-to-date with Java, Spring Boot, and Angular technologies, I will choose these for the implementation.

Basic constraints for the system architecture design:
- It must be a web application.
- Separate implementation on a high level: Database, Core and UI
- It must be scalable and fault-tolerant - cloud ready.
- Separate components in core, use abstraction to be able to extend the system later
  - for Sources (later other Sources will be added)
  - for Subscribers
  - for Channels (e.g. using AWS SES for email delivery)
- Use best practices for UI
- Use in-memory database for demonstration but keep the possibility to use a persistent database in the future
- Assume the following technologies: Java 17, Spring Boot 4, Angular 21, NPM, Gradle, Git

**Claude > Define a Senior System Architect skill to design the system architecture based on the docs/01-requirements-specification.md and Section 5. Define system architecture and constraints of 00-project-overview.md. Use that skill and spawn and agent to design the system architecture to docs/02-system-architecture.md.**

"The architecture document is ready in docs/02-system-architecture.md (842 lines, version 0.1, Draft). It isn't committed.

Coverage:
- All 16 sections from the skill are there, with 10 Mermaid diagrams.
- Every Must and Should requirement is covered. Two are only partly met, because of limits outside the design:
    - NFR-09 (recovering missed events): NewsAPI.org top headlines only return current items, so events missed during a long outage can't be fetched later.
    - NFR-10 (delivery volume): email volume at full demo scale is far above typical Amazon SES limits.

Proposed decisions (ADRs), each marked "Proposed" for you to accept:
- Structure:
    - One Spring Boot application split into Gradle modules: a plugin-interface module, the app, and one module per source or channel. Not microservices.
    - The Angular app is a separate static build, served from the same address as the API.
- Admin login: handled on the server with Spring Security, Google login and the allow-list, using session cookies.
- Data:
    - Spring Data JPA with Flyway migrations.
    - H2 in memory for the demo, PostgreSQL as the switchable persistent option.
    - One generic subscriber table for all subscriber types.
- Hourly run: a Spring scheduled job in the CET zone. ShedLock makes sure only one instance runs it.
- Delivery:
    - One stored notification per event and subscriber, unique on that pair, processed by bounded worker pools per channel.
    - Resilience4j handles retries, rate limits and circuit breakers.
    - Email goes through the Amazon SES API in the cloud and through SMTP to Mailpit (a local test mail server) on your machine.
- Security: Slack webhook URLs are encrypted in the database and masked in the UI and logs.
- Extensibility: sources, channels and subscriber types are Spring beans switched on by a flag in configuration.
- UI: Angular Material, standalone components, signals, lazy-loaded admin area.
- Other:
    - The API documentation is generated from the code (springdoc).
    - Docker Compose runs the system locally; an AWS target is documented but not built.
    - Spring Boot 4.1.x.
    - The manual run trigger, resend and run status sit on an internal management port, not in the admin UI.

Version checks:
- Java 17 works with Spring Boot 4.
- The agent recommends 4.1 over 4.0, because 4.0's free support ends on 2026-12-31.
- Angular 21 works, but it has been in long-term support (critical fixes only) since June 2026, and 22 is the current version.
- Java 17 has no virtual threads, which is why delivery uses bounded thread pools.

Decisions I'd put to you first:
1. Email volume: 5,000 subscribers × about 30 events is roughly 150,000 emails per hour, while the SES trial allows 200 per day. Proposed: cap the events per run and use Mailpit for the demo.
2. In-memory H2 vs. "scalable, fault-tolerant": H2 loses all data on restart and allows only one instance. Proposed: H2 for the demo, plus a PostgreSQL setup to show several instances running.
3. Angular 21 or 22: proposed to stay on 21 as you chose.
4. Duplicate sends: a crash between sending and recording the send can deliver one message twice. Neither Slack webhooks nor SES can prevent that. Proposed: accept it and document it.
5. Which time zone ID means "CET": proposed a regional ID such as Europe/Budapest, so summer time applies.

The document lists these and two more under AQ-01 to AQ-07, each with a proposed default.

The agent's version of the document ended with a note that CLAUDE.md still had no tech stack. I had already fixed CLAUDE.md, so I removed that note. Also, the agent couldn't load the new skill through the skill system, because the skill was created during this session. It read the skill file and followed
it directly instead."

**Claude > I've reviewed and slightly modified 02-system-architecture.md Section 16.2. Finalize this.**

"The architecture document docs/02-system-architecture.md is now final (version 1.0), with a change-history entry listing what changed. It isn't committed.

Status: all 16 ADRs are now Accepted. I kept Section 16.2 as you wrote it, but renamed it "Resolved questions" and renamed its answer column "Decision". I also removed the leftover "proposed" and "recommended" wording from the rest of the document.

Your answers, applied to the rest of the document:
- AQ-01, Angular 21: the tech stack table and compatibility summary now record staying on 21 as a decision.
- AQ-02, time zone: the scheduling section now uses Europe/Budapest.
- AQ-03, event cap:
  - Each source now has a "maximum events per run" setting, default 10, in its extension interface and in its configuration.
  - The email-volume risk mitigation and the NFR-10 row in the traceability table mention the cap.
- AQ-04, database:
  - ADR-04 now includes a Docker Compose variant with PostgreSQL.
  - The local deployment section describes that variant: a separate PostgreSQL container with two Core instances, which shows the single-run lock working.
- AQ-05, missed events: already recorded in the NFR-09 row of the traceability table as a limitation.
- AQ-06, email: ADR-08 now says option (b) was not chosen.
- AQ-07, hosting: the deployment section says the demo runs locally with Docker Compose and the AWS target is documented only.

I edited only text outside the diagrams, so the 10 diagrams are unchanged and still render.

The "Current phase" section of CLAUDE.md still says to wait for architecture approval before writing code. Should I update it to say the architecture is approved and implementation is next?"

**Claude > Architecture is ready. Define two new Senior Software Engineer skills (one for Angular UI, one for Spring Boot Backend development). Fan out two agents with each skill to create implementation details tasks based on the requirements and system architecture. Gather those tasks and orginaze them in docs/03-implementation-details.md. Tasks should be just like JIRA tasks but now recorded only to plain text/md file. I'll approve skills files before agent fan out.** 

"I've written the two skills. Please review them before I start the agents.

Files:
- .claude/skills/backend-engineer/SKILL.md (Spring Boot backend)
- .claude/skills/frontend-engineer/SKILL.md (Angular UI)

What both skills share:
- Sources: the architecture document is binding, and the requirements define the scope. The skills also use Section 5 of the project documentation and CLAUDE.md. If the documents leave a gap that blocks a task, the agent records an open point for you instead of inventing a design.
- Two modes:
  - Planning, which is what we're doing now: JIRA-style tasks, no code.
  - Implementation, for later: one task at a time, with tests, no commits unless you ask.
- Task format: the same template in both skills, so I can merge the results easily:
  - ID (BE-nn / FE-nn), Type (Story, Task or Spike), Epic, Priority, Story points (1, 2, 3 or 5), Labels, Depends on, Traceability (FR/NFR/ADR/section).
  - A Description, a checklist of acceptance criteria, and technical notes.
- Sizing: tasks take 1–3 days and are numbered in the order they should be done. The first tasks produce a runnable skeleton, and the build must stay green after each one.
- Coverage: each agent ends with a table mapping requirements to tasks, lists any Must or Should requirement no task covers, and lists open points.

Backend skill:
- Covers everything under core/: Gradle modules, persistence, REST API, security, scheduling, sources, delivery, channels, resilience, operations, tests, and the Docker Compose files including the PostgreSQL variant.
- Standards include:
  - Java 17 only.
  - Plugins may depend only on the extension-interface module, checked by an ArchUnit test.
  - Validated configuration classes.
  - Flyway-only schema.
  - A standard error format for the API.
- Every API the UI needs gets its own task labelled api-contract.

Frontend skill:
- Covers the public sign-up pages, the admin area (login, list, search, delete, sign-out), routing, guards, the API client, CSRF handling, accessibility and UI tests.
- Standards follow ADR-12: standalone components, signals, zoneless change detection, typed Reactive Forms, Angular Material, a lazy-loaded admin area, strict TypeScript, WCAG 2.1 AA and responsive layout.
- The UI agent can't know the backend task IDs, so it names the backend APIs it depends on in words.

Merge plan, once you approve:
1. Two agents run in parallel. Each writes its tasks to a temporary file in my scratchpad folder, not to the project.
2. I combine both into docs/03-implementation-details.md, with:
  - an overview
  - a suggested order that interleaves backend and UI tasks
  - the backend and UI tasks grouped by epic
  - one requirements coverage table
  - open points
3. While merging, I'll replace the UI's in-words API dependencies with the matching BE-xx IDs.

Let me know when they're approved, or what to change."

**Claue > Approved, fan out the agents**

"2 background agents launched (↓ to manage)

Plan backend implementation tasks 

Plan Angular UI implementation tasks 

2 skills available

Both agents are now running in parallel:
- Backend agent: uses the backend-engineer skill and writes its BE-nn tasks, plus a list of the APIs it delivers for the UI, to a temporary file in my scratchpad folder.
- UI agent: uses the frontend-engineer skill and writes its FE-nn tasks to a separate temporary file there. It names each backend API it depends on in words.

Neither agent will edit any project file. When both have finished, I'll combine the results into docs/03-implementation-details.md and replace the UI's in-words API dependencies with the matching BE-xx IDs."

## 6. Implementation

### Milestone 1

**Claude > I've reviewed "Section 9. Open points for the user", consider the open points in the requirements specification closed. Start the implementation with M1 milestone with multiple agents with necessary skills.**

"The UI half of M1 is done: FE-01 to FE-05, in ui/. I re-ran npm run check myself. Lint, format check, build, the colour-contrast check and all 12 tests pass. The backend agent is still working on BE-01 to BE-05.

What was built:
- FE-01, workspace:
  - Angular 21.2.25 with Material and CDK 21.2.14 and TypeScript 5.9.3, all pinned exactly.
  - It runs without zone.js, and strict TypeScript and strict templates are on.
- FE-02, tooling:
  - ESLint with Angular's accessibility rules, and Prettier.
  - An npm run check script that runs every check, since there's no CI.
- FE-03, proxy: the dev server forwards /api, /oauth2, /login and /logout to the Core on localhost:8080. It was tested against a mock Core, including cookies and redirects.
- FE-04, theme:
  - Material's default palette. A new check:contrast script measures its colour pairs, and all of them pass WCAG AA, so no colour change was needed.
  - Strong focus outlines, and the system font stack instead of Google Fonts.
- FE-05, shell:
  - Header, navigation, main area and footer, a skip link, and focus moving to the page heading after navigation.
  - The admin area loads as a separate chunk.
  - The public sign-up pages are placeholders for now.

Points for you:
1. Fonts: keep the system fonts, or self-host Roboto. Either works with the strict security policy planned for Nginx.
2. npm test: it now runs once and exits, and watch mode is npm run test:watch.
3. Extra checks: the agent added three small things beyond the tasks: the check and check:contrast scripts and a lint rule that requires the faster OnPush change detection. Keep them or drop them?
4. ui/.vscode/mcp.json: the Angular CLI generated this editor config, and it runs an unpinned npx @angular/cli mcp, which would download Angular 22. I'd pin it to @21 or delete it.

I'll report the backend half when its agent finishes."

"M1 is complete: BE-01 to BE-05 are in core/ and FE-01 to FE-05 are in ui/. I re-ran both builds myself. ./gradlew build passes with 51 tests, none skipped or failed. npm run check passes with 12 tests. Nothing is committed, and docs/, CLAUDE.md and .claude/ are unchanged.

  Backend, what was built:
  - BE-01, skeleton:
    - Gradle 9.8 with the Kotlin DSL, a version catalog, and shared build rules.
    - The 7 modules under com.sonrisa.alerting; the plugin modules are wired into the app as runtime-only dependencies.
    - The app starts on Java 17 and reports healthy.
  - BE-02, versions: pinned in core/docs/library-versions.md, each with a source and a smoke test:
    - Spring Boot 4.1.1, springdoc 3.1.1, Resilience4j 2.4.0, ShedLock 7.10.1.
    - AWS SDK 2.55.8, ArchUnit 1.5.1, Testcontainers 2.0.5, GreenMail 2.1.14.
    - The WireMock Jetty clash came up again, so it uses wiremock-standalone 3.13.2, as agreed.
  - BE-03, module rules: 6 ArchUnit rules, each proven with a deliberately broken example class.
  - BE-04, configuration:
    - Validated configuration classes with Europe/Budapest as the default zone.
    - Profiles local, demo, postgres, aws and test, with JSON logs in the deployed profiles.
    - A scan that fails if a literal secret appears in the configuration files, and core/.env.example.
  - BE-05, operations port:
    - Actuator runs only on port 8081, and port 8080 returns 404 for all Actuator paths.
    - Health checks are open; info and metrics need the operator's login.
    - Graceful shutdown with a 30-second timeout.

  Decisions for you:
  1. Operator password: if none is set, the app starts with a warning and only the health checks answer. The first run instead refused to start without it in demo, postgres and aws. I'd go back to that, so a deployed profile can't run without a password.
  2. Encryption and fingerprint keys: they stay optional until BE-09 needs them. That's fine for now.
  3. Default profile: local is the default, so containers must set SPRING_PROFILES_ACTIVE. BE-43 will do that.
  4. Mail in tests: tests use GreenMail, which runs inside the JVM without Docker. Mailpit stays the mail inbox in Docker Compose.
  
  UI points still open from the UI report: fonts, npm test running once, the extra check, check:contrast and lint rule, and the unpinned ui/.vscode/mcp.json.

  Things I noticed:
  - The build adds -Xmx1g Gradle memory settings in core/gradle.properties because of metaspace warnings.
  - The PostgreSQL Testcontainers check ran instead of being skipped, so Docker works here for BE-06.
  - CLAUDE.md still says no code exists. Should I update its current phase, stack and build commands now?"

**Claude > Decisions on UI related points. 1: self-host Roboto font. 2: ok. 3: Keep the extra checks. 4: Pin to v21. Decisions on backend points. 1: deployed profile cannot run without a password. 2. Optional is fine for now. 3: local profile is fine. 4: Ok. Finally, update CLAUDE.md.**

"I've applied all your decisions and updated CLAUDE.md. Both builds pass after the changes. Nothing is committed.

UI changes:
- Roboto: it's now self-hosted from @fontsource/roboto 5.3.0, pinned, in weights 400, 500 and 700. The theme uses Roboto, with system fonts as the fallback.
    - The build copies the font files into the output folder, and there are no references to Google's servers.
    - The build includes 54 font files, but a browser only downloads the ones for the characters it shows, usually the Latin woff2.
    - The styles file grew by about 3 kB compressed.
- npm test running once: kept as agreed.
- Extra checks: kept, as you decided.
- ui/.vscode/mcp.json: now runs @angular/cli@21.2.24, the same version as the project.
- Check: npm run check passes, with 12 tests.

Backend change:
- Operator password:
    - The demo, postgres and aws profiles now refuse to start without ALERTING_MANAGEMENT_OPERATOR_PASSWORD. The error names the setting but never shows a value.
    - local and test still allow no password.
    - core/.env.example explains this.
- New tests: a start-up test for each deployed profile, plus three binding tests.
- One side effect I had to fix: a failed start doesn't reset the JSON log format, and that broke the local-profile logging test that runs later in the same test run. The new test now switches JSON logging off for its own start.
- Check: ./gradlew build passes, with 57 tests, none skipped or failed.

CLAUDE.md:
- Current phase: implementation, with M1 done and M2 next. The requirements, the architecture and the Section 9 open points count as closed decisions. Tasks are done with the two engineer skills, one at a time, and there are no commits unless you ask.
- New sections: these replace the "to be filled in later" list:
    - Tech stack: the pinned versions.
    - Build / run / test commands: the Gradle and NPM commands, ports, the proxy, and the password rule.
    - Key directories: the main folders in core/ and ui/.

core/.gitignore and ui/.gitignore are already staged in git, so you or your editor must have added them. Everything else in core/ and ui/ is still untracked."

**Claude > Propose a short commit message for M1 milestonre related implementation.**

"Here's a suggested commit message:

Implement milestone M1: runnable core and UI skeletons

- core: Gradle 9.8 multi-module build (Kotlin DSL, version catalog,
  Java 17 toolchain), Spring Boot 4.1.1 app, pinned and verified
  libraries, ArchUnit module rules, validated configuration and
  profiles, Actuator on management port 8081 (BE-01 to BE-05)
- ui: Angular 21 workspace (standalone, zoneless, strict), ESLint and
  Prettier, dev-server proxy to the core, Material theme with
  self-hosted Roboto, app shell and routes (FE-01 to FE-05)
- CLAUDE.md: implementation phase, tech stack, build/run/test
  commands and key directories"

### Milestone 2

**Claude > Start M2 milestone with multiple agents**

"M2 is complete: BE-06 to BE-13, BE-17, FE-06 to FE-08 and FE-10. Both builds pass on the merged code. ./gradlew build runs 249 tests, none skipped or failed. npm run check runs 56 tests. The merged app also works when started. Nothing is committed.

How the parallel work came together:
- Merging: I brought the three backend worktrees into the main tree.
    - SPI: applied cleanly.
    - API/security: one conflict, in the ArchUnit rules.
    - Persistence: four more conflicts, all blocks added at the same spot, and I kept both sides.
    - The three worktrees are now removed.
- Glue code: I added SpiFailureClassifier, with tests, so the BE-17 retries use the transient/permanent types from BE-10.
- Contract fix: the UI had guessed the CSRF problem type as urn:alerting:problem:csrf. The backend uses urn:alerting:problem:csrf-token-invalid, so I changed the UI to match. The Slack webhook-not-verified type already matched.
- Mid-run fix: the SPI agent found that the EVENT table in the architecture lacked a human-readable source name, which FR-09 and FR-17 need. I told the persistence agent while it was still working, and it added EVENT.source_name.
- Housekeeping: .claude/worktrees/ is now in .gitignore. CLAUDE.md now shows M2 as done, lists the new required keys, the database and migrations, and notes that ./gradlew build needs Docker.

What M2 delivers:
- Persistence: JPA, 5 Flyway migrations that run on both H2 and PostgreSQL, all tables, AES-GCM encryption of webhook URLs, and HMAC fingerprints of addresses. The deployed profiles refuse to start without the keys.
- Extension points: the three interfaces with their contracts documented. The registries stop start-up on a duplicate key or on a subscriber type whose channel is missing.
- API foundation: 11 stable Problem Details types, times in the Budapest offset, OpenAPI, and the CSRF endpoint. Unauthenticated admin API calls get a 401.
- HTTP clients and retries: clients must have timeouts, and retries honour Retry-After.
- UI: the message catalogue, CET time formatting, the CSRF call at start-up, the error-mapping interceptor, and accessible form feedback components.

Decisions for you. The first one matters before M3:

1. How plugins build HTTP clients (needed for BE-18 in M3): plugins depend only on the extension-interface module, so they can't use the app's timeout-checked client builder. The options:
    - (a) Plugins get Spring Boot and build clients with timeouts from their own settings.
    - (b) A small shared support module for plugins.
    - (c) The app builds the clients and passes them in.
      I recommend (a). It's the least new structure, and the global timeouts plus the ArchUnit rule still apply.
2. Retention vs. foreign key: events are kept 30 days, but their notifications 90. The plain foreign key would block the retention job from deleting old events. I recommend ON DELETE SET NULL, decided in BE-42.
3. Schema deviations to confirm:
    - source_name and notification.created_at are new columns.
    - trigger is renamed to run_trigger and at to performed_at, because both are SQL keywords.
    - Event content is limited to 4,000 characters.
4. SPI choices:
    - A subscriber type whose channel is missing stops start-up instead of warning.
    - Plugins are off unless enabled.
    - The source exception is checked, so every source must say whether a failure is transient.
5. API choices:
    - Problem types are URNs (urn:alerting:problem:*).
    - Framework error texts are replaced by fixed ones.
    - HSTS is left to Nginx.
    - Forwarded headers are trusted only from private networks.
    - A Retry-After longer than 60 seconds ends the retries.
6. UI choices:
    - The start-up CSRF call doesn't block rendering; I'd keep it that way.
    - A repeated identical message isn't announced again by screen readers.
    - There's no automatic CSRF retry.
7. Other notes:
    - The aws profile has no datasource yet.
    - Safari rejects Secure cookies on http://localhost, but Chrome and Firefox are fine.

One lesson for future parallel runs: the worktrees share Gradle daemons, so one agent's ./gradlew --stop interrupted another agent's build. Next time I'll tell agents not to stop daemons while others are running."

**Claude > Decisions: 1: a, 2: go with ON DELETE SET NULL, 3: ok, 5: ok, 6: ok, 7: ok, provide more details on 4.**

"Point 4 covers three separate choices from BE-10 and BE-11. Here is each one with its options.

4a. A subscriber type whose channel is missing or disabled: stop start-up (current) or only warn

Every subscriber type names the channel that delivers to it: email goes to the email channel, slack to the Slack channel. The rule catches the case where the type is active but its channel isn't. Examples: a module that brings a subscriber type for another module's channel, or a configuration mistake in
a future module that has separate flags for its type and channel. It rarely happens with our own modules, because the email and Slack modules switch their type and channel on with one shared flag.

_(output cut...)_"

### Milestone 3

Claude > Start M3 milestone with multiple agents

