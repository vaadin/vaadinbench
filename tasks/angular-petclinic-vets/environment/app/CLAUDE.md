# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project purpose

Demo for a talk on **Spec-Driven Development with the AI Unified Process
(AIUP)**. Re-implements the classic Spring PetClinic by writing the specs
first (`docs/`) and generating code against them — here on Angular and
Quarkus with JPA.

**`docs/` is the source of truth, not the code.** When asked to implement
something, read the relevant spec first:

- `docs/entity_model.md` — ER diagram + attribute tables with validation
  rules. The schema in Flyway migrations must match this.
- `docs/use_cases/UC-NNN-*.md` — one file per use case with preconditions,
  main success scenario, alternative flows, postconditions, business rules.
  UI flows, field labels, and navigation come from these.
- `docs/business_rules.md` — the rules several use cases share, as `GR-NNN`.
  A use case's `BR-NNN` heading references one instead of restating it, so
  follow the link before implementing that rule.
- `docs/test_cases/TC-NNN-*.md` — end-to-end journeys spanning several use
  cases.
- `docs/architecture/README.md` — the stack, the layout, the conventions and
  the tests. The use cases say *what*, it says *how it is built*.

If a use case and the code disagree, the use case wins unless the user says
otherwise.

## Stack

**Java 25**, **Quarkus 3.40** (REST + Jackson), **Hibernate ORM with Panache**
on **PostgreSQL**, schema owned by **Flyway**; **Angular 22** in `frontend/`,
served by Quarkus from the frontend's build output. Backend code lives under
`ai.unifiedprocess.petclinic`, package by feature; the frontend under
`frontend/src/app`, folder by feature.

## Commands

```bash
# Build the frontend — Quarkus serves frontend/dist, so the browser tests need it
cd frontend && npm run build && cd ..

# Frontend component tests (Vitest)
cd frontend && npm test && cd ..

# Backend tests: API tests and Playwright browser tests (UC<NNN><Name>Test)
mvn test
mvn test -Dtest=UC001ViewWelcomePageTest

# Run locally: backend on :8080 with the seed data, frontend dev server on :4200
mvn quarkus:dev
cd frontend && npm start
```

PostgreSQL runs locally on port 5432 (`vb-postgres start`; Maven starts it on
every build). There is no Docker, so Quarkus Dev Services are off.

## When to read the detailed guides

- **Before implementing a use case** → read the corresponding
  `docs/use_cases/UC-NNN-*.md` spec first, and follow every `GR-NNN` link in
  its business rules into `docs/business_rules.md`.
- **Before writing code or tests** → read `docs/architecture/README.md`.
