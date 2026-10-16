# ADR-006: Two test layers — use-case `*Test` and journey `*IT` — plus Vitest

**Status:** Accepted   
**Date:** 2026-09-17   
**Affects:** Development View

## Context

Two things need verifying, and they are not the same thing. A **use case** is a
screen's behaviour: validation, notifications, navigation, ordering. A **test
case** is a journey across several use cases, and it is only convincing if a
real browser walks it.

Walking every use case as a full journey is slow and brittle. Testing only the
REST API never proves the page renders and responds in a browser; testing only
Angular components with a mocked backend never proves the two halves agree.
Measuring coverage on one layer only would push an agent into writing duplicate
tests for code another layer already covers.

## Decision

Two Java layers, distinguished by the class name suffix, which also decides the
Maven phase, and a component layer in the frontend:

| Layer | Name | Runner | Scope |
|-------|------|--------|-------|
| `@QuarkusTest` + `BrowserTest` / REST Assured | `UC<NNN><Name>Test` | Surefire, `test` | one use case: its page in Playwright, its resource over `/api` — **the default** |
| `@QuarkusTest` + `BrowserTest`, `@TestCase` | `TC<NNN><Name>IT` | Failsafe, `verify` | a test case journey in a real browser |
| Vitest (`ng test`) | `*.spec.ts` beside the component | `npm test` | a component's own logic, `describe('UC-NNN: …')` |

Both Java layers run against a real PostgreSQL, the local server (NFR-007, C-007),
and the built frontend. `quarkus-jacoco` instruments the `@QuarkusTest` runs and
writes its report to `target/jacoco-report`. Vitest specs are not seen by the
Java sensors; they never replace a `UC<NNN><Name>Test`.

## Consequences

- **Coverage counts what the application did, not which layer drove it.**
  `quarkus-jacoco` instruments the application inside every `@QuarkusTest`,
  journeys included, so nobody writes a use-case test just to satisfy the gate.
- **Only `mvn verify` runs both layers.** `mvn test` stops before the journeys —
  a coverage number read after `test` leaves them out by construction. And the
  frontend must be built first, or every browser test fails.
- **Naming is load-bearing.** A use-case test called `*IT` never runs in `test`;
  a journey called `*Test` runs in the wrong phase. `TestLayerConventionsTest`
  fails the build on either.
- **The journey layer stays small** — one class per specified test case, and no
  more — because it is the expensive one.
