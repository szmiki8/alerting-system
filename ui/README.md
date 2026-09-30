# Alerting System UI

Angular 21 single-page application for the Alerting System (public sign-up and admin area).
Generated with [Angular CLI](https://github.com/angular/angular-cli) 21.2.24.

## Prerequisites

- Node.js 22 or 24 LTS (see `.nvmrc`; run `nvm use`) and npm.
- `npm ci` to install the pinned dependencies from `package-lock.json`.

## NPM scripts

| Script                   | What it does                                                                                             |
| ------------------------ | -------------------------------------------------------------------------------------------------------- |
| `npm start`              | Dev server on `http://localhost:4200/` with the proxy to the Core.                                       |
| `npm run build`          | Production build into `dist/alerting-ui/` (fails when a budget is exceeded).                             |
| `npm test`               | Unit tests (Vitest) once.                                                                                |
| `npm run test:watch`     | Unit tests in watch mode.                                                                                |
| `npm run test:ci`        | Unit tests once, headless, with a coverage report in `coverage/`.                                        |
| `npm run lint`           | angular-eslint over TypeScript and templates, including accessibility rules.                             |
| `npm run format`         | Format all files with Prettier.                                                                          |
| `npm run format:check`   | Check formatting without changing files.                                                                 |
| `npm run check:contrast` | WCAG AA contrast check of the built theme colours (after `npm run build`).                               |
| `npm run check`          | All checks: lint, format check, build, contrast check, e2e type check, tests with coverage.              |
| `npm run e2e`            | Playwright end-to-end tests against a mocked Core; starts `ng serve` on port 4201 (or reuses one).       |
| `npm run e2e:ci`         | The same, headless with line output; with `CI` set it always starts a fresh dev server and retries once. |
| `npm run e2e:typecheck`  | Type-checks the Playwright tests (Playwright itself does not).                                           |

The end-to-end tests need the Chromium browser once: `npx playwright install chromium`. They are
not part of `npm run check`, because that would require a browser binary and a free port; run
them separately. Reports go to `playwright-report/` and `test-results/`.

## Local development with the Core

The browser must see one origin, as in production behind Nginx (ADR-02). `npm start` runs
`ng serve` with `proxy.conf.mjs`, which forwards these paths to the Core:

- `/api/**` (REST API)
- `/oauth2/**` and `/login/**` (Google login start and callback)
- `/logout`

Everything else is served by the dev server. The UI only makes relative calls, so no backend URL
appears in the UI code.

1. Start the Core on `http://localhost:8080` (see `core/`).
2. Run `npm start` and open `http://localhost:4200/`.

To use another Core address, set `CORE_URL`, for example:

```bash
CORE_URL=http://localhost:9090 npm start
```

The proxy keeps the browser's `Host` header and adds `X-Forwarded-*` headers, so the Core builds
redirect URIs (for example the Google OAuth callback) for `http://localhost:4200`. UI routes must
not start with `/api`, `/oauth2`, `/login` or `/logout`, because these paths belong to the Core.
