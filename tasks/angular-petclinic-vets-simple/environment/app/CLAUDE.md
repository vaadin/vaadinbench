# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project purpose

Demo for a talk on **Spec-Driven Development with the AI Unified Process
(AIUP)**. Re-implements the classic Spring PetClinic by writing the specs
first (`docs/`) and generating code against them — here as an Angular
frontend over a Quarkus REST API with JPA.

**`docs/` is the source of truth, not the code.** When asked to implement
something, read the relevant spec first:

- `docs/entity_model.md` — ER diagram + attribute tables with validation
  rules. The schema in Flyway migrations must match this.
- `docs/use_cases.puml` — PlantUML actor/use-case diagram.
- `docs/use_cases/UC-NNN-*.md` — one file per use case with preconditions,
  main success scenario, alternative flows, postconditions, business rules.
  UI flows, field labels, and navigation come from these.
- `docs/business_rules.md` — the rules several use cases share, as `GR-NNN`.
  A use case's `BR-NNN` heading references one instead of restating it, so
  follow the link before implementing that rule. A rule only one use case
  needs stays in that use case; **no business rule belongs in
  `docs/architecture/`**.
- `docs/test_cases/TC-NNN-*.md` — end-to-end journeys spanning several use
  cases; each is verified by a Playwright `TC<NNN><Name>IT`.
- `docs/architecture/` — the 4+1 views (logical, process, development,
  physical), the code and test conventions, and the ADRs behind them. The use
  cases say *what*, these say *how it is built*;
  [`docs/architecture/README.md`](docs/architecture/README.md) says which
  document answers which question.

Four sensors (`mvn test`) enforce the links between the documents and the
code: `UseCaseTraceabilityTest`, `TestCaseTraceabilityTest`,
`BusinessRuleTraceabilityTest` — between `docs/business_rules.md` and the use
cases that reference a `GR-NNN` — and `RequirementTraceabilityTest`, between
`docs/requirements.md` and the `**Requirements:**` line of every use case.
Because of the first two a `Status:` line (`Done`/`Tested` on a use case,
`Automated` on a test case) is an assertion that switches the sensor on, not a
label — run
`aiup-angular-jpa:coverage-check` first; the hooks refuse a status set without
it. What each sensor checks is in
[`docs/architecture/testing.md`](docs/architecture/testing.md#the-traceability-sensors).

If a use case and the code disagree, the use case wins unless the user says
otherwise.

## Stack

**Java 25**, **Quarkus 3.40** (Quarkus REST + Jackson), **Hibernate ORM with
Panache** (repository pattern) on **PostgreSQL**, schema owned by **Flyway**,
**Angular 22** (standalone components, signals, Angular Material) in
`frontend/`, served by Quarkus from the frontend's build output. Backend code
lives under `ai.unifiedprocess.petclinic`, package by feature (`api` +
`domain`); the frontend under `frontend/src/app`, one folder per feature.
There is no code generation: the entities are written by hand against the
tables Flyway creates.

The full table, with the decisions behind each choice, is in
[`docs/architecture/development.md`](docs/architecture/development.md#technology-stack).

## Commands

```bash
# Build the frontend. Quarkus serves frontend/dist/petclinic/browser, so every
# browser test (UC<NNN><Name>Test with a page, TC<NNN><Name>IT) needs it built first.
cd frontend && npm run build

# Frontend component tests: Vitest runs every *.spec.ts beside its component
cd frontend && npm test

# Run the app locally: backend on :8080 (postgres database, cleaned, migrated
# and seeded on start), frontend dev server on :4200 proxying /api to it
mvn quarkus:dev
cd frontend && npm start

# Backend tests: Surefire runs every *Test class
# (UC<NNN><Name>Test — REST Assured against /api and/or Playwright against the
# built frontend — the ArchUnit rules in ArchitectureTest and
# TestLayerConventionsTest, and the four traceability sensors)
mvn test

# Only the sensors (ArchitectureTest, TestLayerConventionsTest and the four
# traceability sensors): seconds, no Quarkus application, no database, no
# frontend build. The run the Stop hook asks for.
mvn -q test -Dgroups=sensor

# Run a single *Test class / method
mvn test -Dtest=UC004FindOwnersByLastNameTest
mvn test -Dtest=UC004FindOwnersByLastNameTest#singleMatchNavigatesDirectlyToDetails

# Integration tests: Failsafe runs every *IT class in the verify phase
# (the Playwright journeys TC<NNN><Name>IT, @QuarkusTest + @TestCase).
# verify runs all *Test first; it does not build the frontend.
mvn verify

# Run a single *IT class
mvn verify -Dit.test=TC001NewOwnerFirstVisitIT
```

Test class suffix decides the phase: `*Test` → `test` (Surefire),
`*IT` → `verify` (Failsafe). Never name a `UC*` use-case test `*IT` or a
`@TestCase` journey `*Test` — `TestLayerConventionsTest` fails the build if you
do, because the wrong suffix makes a test silently never run.

PostgreSQL runs locally on localhost:5432 (`vb-postgres start`); every Maven
build makes sure it is running and that the `petclinic_test` database exists.
There is no Docker, so Quarkus Dev Services are off. Tests use
`petclinic_test`, cleaned and migrated at start and seeded from
`src/test/resources/db/migration/V2__seed_reference_data.sql`. **If you
change the frontend, rebuild it before running the browser tests — they test
whatever `frontend/dist` holds.**

## Coverage

Coverage comes from `quarkus-jacoco` and spans **both** Java test layers: the
`TC<NNN><Name>IT` journeys are `@QuarkusTest`s too, so a line reached only by a
journey counts as covered — **don't add a use-case test just to cover
something a `TC<NNN><Name>IT` already exercises.** `mvn verify` runs both
layers; the report is in `target/jacoco-report`. Vitest specs cover the
frontend and do not count toward it. Details:
[`docs/architecture/development.md`](docs/architecture/development.md#coverage).

## When to read the detailed guides

- **Before implementing a use case** → read the corresponding
  `docs/use_cases/UC-NNN-*.md` spec first, and follow every `GR-NNN` link in
  its business rules into
  [`docs/business_rules.md`](docs/business_rules.md). Together they define
  preconditions, the main success scenario, alternative flows, postconditions,
  business rules, field labels, and navigation. The spec is the source of
  truth.

- **Before implementing a use case, writing a page or a resource, adding a
  repository, or touching anything in `src/main/java/` or `frontend/src/`** →
  read [`docs/architecture/development.md`](docs/architecture/development.md)
  first. It covers the stack, package and folder layout, JPA mapping patterns,
  the transaction boundary, REST resource and Angular page conventions, the
  shell, form validation, error handling, the `*Repository` naming and scope
  rule, and the build. Much of it is enforced by ArchUnit in
  `ArchitectureTest` (`mvn test`) — if you change a convention there, update
  the matching rule in the same commit.

- **Before changing a transaction, a save path, or anything about how the
  application behaves at runtime** → read
  [`docs/architecture/process.md`](docs/architecture/process.md). It holds the
  transaction rule (the repository is the boundary, never a resource), the
  check-then-act on duplicate pet names that the database backs up, and the
  fact that nothing here is asynchronous — don't add a job, an event, or a
  server push that no use case asks for.

- **Before changing `.claude/` or wondering why a turn refuses to end** → the
  hooks in `.claude/hooks/` enforce what the sensors cannot: that they actually
  ran before work is reported as done, and that a `Status:` line is not set
  without a coverage audit. They read the repository, not the tool call, and
  accept only positive evidence (a fresh Surefire report, a finished audit). See
  *Agent guardrails* in
  [`docs/architecture/development.md`](docs/architecture/development.md),
  [ADR-010](docs/architecture/adr/ADR-010-hooks-guard-the-session.md) and
  [ADR-011](docs/architecture/adr/ADR-011-guards-read-state-and-demand-evidence.md). The rule
  for adding one: if it can be checked by reading the repository, it is a test,
  not a hook.

- **Before writing or modifying any test under `src/test/java/` or a
  `*.spec.ts`** → read [`docs/architecture/testing.md`](docs/architecture/testing.md)
  first. It covers `BrowserTest`, REST Assured against `/api`, the
  `UC<NNN><Name>Test` / `*IT` naming rule, the `@UseCase` annotation, seed-data
  conventions, accessibility-role locators, the Playwright `*IT` layer, the
  Vitest specs, and what you must **not** do (no CSS or XPath locators where a
  role and an accessible name work, no assertions on a component's internal
  state, no sleeps).

The logical, physical and decision views —
[`logical.md`](docs/architecture/logical.md),
[`physical.md`](docs/architecture/physical.md) and
[`adr/`](docs/architecture/adr) (why JPA with Panache, why an Angular SPA over
a REST API, why Flyway owns the schema) — answer *why it is shaped this way*.

Do not skip these. The files are short and kept current — drift between
them and the code is a bug to fix, not a style preference to ignore.

## Skills available for this project

Prefer these over ad-hoc generation:

- `aiup-core:entity-model`, `aiup-core:use-case-spec`,
  `aiup-core:use-case-diagram`, `aiup-core:requirements` — authoring/updating
  specs in `docs/`.
- `aiup-angular-jpa:flyway-migration` — generate `V*.sql` from the entity
  model.
- `aiup-angular-jpa:implement` — implement a use case end-to-end (JPA entity
  and repository, REST resource, Angular page). Already honours
  `docs/architecture/development.md`.
- `aiup-angular-jpa:spring-boot-test` — the backend test skill: `@QuarkusTest`
  use-case tests (REST Assured, `BrowserTest`), named `UC<NNN><Name>Test`, run
  by `mvn test`. Default for UC tests; see `docs/architecture/testing.md`.
- `aiup-angular-jpa:vitest-test` — Angular component tests, `*.spec.ts` beside
  the component, run by `npm test`.
- `aiup-angular-jpa:playwright-test` — browser journeys extending
  `BrowserTest`, named `TC<NNN><Name>IT`, run by `mvn verify`. Use for test
  cases (TC-*) and journeys that span several use cases.
- `aiup-core:test-case` — author `docs/test_cases/TC-NNN-*.md`.
- `aiup-angular-jpa:coverage-check` — audit a UC/TC against code and tests
  before changing its `Status:` line (sub-agent `aiup-angular-jpa:uc-coverage`).
