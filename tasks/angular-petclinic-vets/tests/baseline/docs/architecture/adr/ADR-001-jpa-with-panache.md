# ADR-001: JPA (Hibernate ORM with Panache) instead of jOOQ

**Status:** Accepted   
**Date:** 2026-09-17   
**Affects:** Logical View, Development View

## Context

The original Spring PetClinic uses JPA, and so does Quarkus's own persistence
stack. The specifications state exact queries, orderings, and "load this in one
query" rules (UC-002 BR-002, UC-004 BR-002, UC-005 BR-001/002, NFR-001/002), and an
AI coding agent must not be free to satisfy them with a lazy-loading side effect
nobody can see in the code.

With an ORM, what reaches the database is the product of mappings, fetch types,
and a session lifecycle — used without rules, exactly the implicit behaviour an
agent gets wrong and a reviewer misses. jOOQ would avoid it, at the price of a code
generator in the build and an idiom no Quarkus guide uses.

## Decision

Use **JPA through Hibernate ORM with Panache**, and constrain it so the query a
page runs is written in one known place (C-004).

- Hand-written `@Entity` classes in the feature's `domain` package map the tables
  Flyway creates ([ADR-003](ADR-003-flyway-owns-the-schema.md)), names explicit.
- **Repository pattern, no active record.** Queries live in `<Entity>Repository`,
  an `@ApplicationScoped` class implementing `PanacheRepositoryBase<E, ID>`. An
  entity never extends `PanacheEntityBase`, so not every class can run a query.
- Associations are lazy, and **the repository loads what a page needs** in its own
  query: `left join fetch` for the children, which Hibernate 7 pages in SQL.
  Nothing lazy leaves the repository; resources return records, never entities.

`ArchitectureTest.noSpring`, `entitiesLiveInDomain`, `entitiesAreNotActiveRecords`,
`repositoriesAreNamedAndScoped`, `panacheRepositoriesAreRepositories` and
`resourcesNeverReturnEntities` enforce this.

## Consequences

- **The query is in the code, the SQL is not.** The JPQL in a repository is what
  review compares with the specification; the SQL Hibernate makes of it has to be
  checked by logging it (`quarkus.hibernate-orm.log.sql=true`) when the N+1
  discussions in [`../process.md`](../process.md) matter.
- **No code generation, and no compile-time check.** A wrong column name surfaces
  as an SQL error in the first test that reads it, so a migration and its entity
  change in the same commit.
- **Dirty checking and cascades exist, and are kept out of reach.** An entity is
  only managed inside a repository method; what a resource holds is detached, so
  every write is an explicit repository call.
- **The schema is PostgreSQL-specific** even though JPQL is not. Accepted: C-005
  names one database.
