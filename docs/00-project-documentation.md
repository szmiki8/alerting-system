# Alerting System - SDLC demonstration

## Project Overview

The aim of the project is to demonstrate a software development methodology based on real requirements and fictional details.

The prompts given to Claude Code during the process are also recorded in this documentation. Each prompt starts with the string "Claude > ", and the result produced by Claude Code follows it.

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
5. Design system architecture and constraints
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
- .claude: skill definitions used in the project

## 5. Define system architecture and constraints

Since I am most up-to-date with Java, Spring Boot, and Angular technologies, I will choose these for the implementation.

Basic constraints for the system architecture design:
- It must be a web application.
- Separate implementation on a high level: Database, Core and UI
- It must be scalable and fault-tolerant - cloud ready.
- Separate components in core, use abstraction to be able to extend the system later
  - for Sources (later other Sources will be added)
  - for Subscribers and Channels
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