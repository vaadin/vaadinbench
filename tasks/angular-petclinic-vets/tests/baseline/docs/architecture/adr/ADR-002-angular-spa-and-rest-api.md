# ADR-002: An Angular SPA over a REST API instead of server-side views

**Status:** Accepted   
**Date:** 2026-09-17   
**Affects:** Process View, Development View, Physical View

## Context

The application is a small internal CRUD front end for a clinic's front desk:
forms, lists, and navigation on the clinic's own network. Server-rendered Qute
templates would keep it to one language and one build. A single-page application
adds a second language, build and test stack, plus an API that exists so the two
halves can talk — for an AI coding agent, a second place for every use case to be
half-implemented. Against that, Angular over JSON is what most teams build such
applications with, and PetClinic parity (C-009) lies in screens and URLs, not in
the rendering model.

## Decision

Build the UI as an **Angular 22 single-page application** in `frontend/`, talking
to a **REST API under `/api`** (C-003). One page per use case, routed in
`frontend/src/app/app.routes.ts`; one resource per feature on the backend,
returning records shaped for the page. Quarkus serves the built frontend as static
content, and `core/SpaRouting` answers every non-`/api` path with `index.html` so
a deep link or a reload reaches the Angular router.

Server-side rendering with Qute is not used here. If a screen ever needs it, it is
a new decision and a new ADR, not a quiet addition.

## Consequences

- **Two halves, one test class.** A `UC<NNN><Name>Test` drives the page in a real
  browser through the real backend, so it verifies both
  ([ADR-006](ADR-006-two-test-layers-merged-coverage.md)) — once the frontend is
  built (`npm run build`).
- **UI state lives in the browser.** The server is stateless: scaling out needs no
  session affinity, and a reload loses an open form. See
  [`../physical.md`](../physical.md).
- **There is an API.** Anything that reaches the port can call it without the UI,
  which C-010's trusted-network assumption tolerates — and which is also why
  breaking that assumption is a bigger change than it looks.
- **Angular conventions become code conventions:** a Material sidenav for the menu,
  component styles, `NotFoundPage` on the `**` route for a 404, `core/ErrorMapper`
  for every failure the API reports.
