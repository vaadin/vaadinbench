# ADR-003: Flyway owns the schema; entities map it, Hibernate never creates it

**Status:** Accepted   
**Date:** 2026-09-17   
**Affects:** Logical View, Development View, Physical View

## Context

[ADR-001](ADR-001-jpa-with-panache.md) maps hand-written entities onto a database
schema, so something has to define that schema. Three candidates: Hibernate
generating it from the entities, a hand-maintained SQL file, or versioned
migrations. The entity model in [`../../entity_model.md`](../../entity_model.md)
is the specification; NFR-009 requires the schema to reproduce it exactly, which
means the schema needs to be reviewable as text next to that document — not
derived from annotations spread across classes.

## Decision

**Flyway migrations are the only way the schema is created or changed**
(C-006), in `src/main/resources/db/migration`. `quarkus-flyway` runs them when
the application starts (`quarkus.flyway.migrate-at-start=true`), and Hibernate's
schema management is off (`quarkus.hibernate-orm.schema-management.strategy=none`):
entities map the tables, they never create or alter one.

Test-only seed data is a separate migration on the test classpath
(`src/test/resources/db/migration/V2__seed_reference_data.sql`) so it can never
reach a production database. The dev profile adds that location explicitly.

## Consequences

- **The migrations are the schema DSL, the entities a second description of it.**
  Changing one and not the other is the most common surprise in this project, and
  nothing catches it at build time: the first test that queries the table does.
- **A local PostgreSQL server is a build dependency** (C-007): every Maven build
  runs `vb-postgres ensure petclinic_test`, and the test and dev profiles clean
  and re-migrate their database at start-up.
- **No generated code.** There is nothing to regenerate, commit, or exclude from
  analysis; every class under `src/main/java` was written by someone.
- **A schema change is reviewable as a diff** against the entity model, which is
  what makes NFR-009 checkable by a human in seconds.
