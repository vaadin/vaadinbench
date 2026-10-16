# ADR-009: The repository is the transaction boundary

**Status:** Accepted   
**Date:** 2026-09-17   
**Affects:** Process View, Development View   
**Supersedes:** relying on auto-commit with no transaction declared anywhere

## Context

Every use case in this application writes exactly one row with one statement, so
a single statement's atomicity in PostgreSQL would do: nothing can be half-written.
Declaring nothing at all is therefore tempting — the transaction boundary would
exist, it would simply be nowhere in the code.

That is a bad shape to hand an AI coding agent. If nothing in the source says
where a transaction begins, the first use case that needs two statements gets
them as two calls from a resource, each committing on its own — and it looks
exactly like every use case before it. An implicit boundary cannot be reviewed,
and it cannot be enforced. With JPA there is a second reason: an entity's lazy
association can only be read inside a transaction.

The question is not *whether* transactions exist but *where they are declared*.
Two candidates: the resource, which knows the use case, or the repository, which
knows the data. A transactional resource invites returning entities and letting
Jackson walk their lazy associations — queries nobody wrote, made while the
response is serialized.

## Decision

**Declare transactions on the repository.**

- `jakarta.transaction.Transactional` at class level on every `*Repository`, so
  every read and every write (`persist`, an update) runs in one.
- **Never `@Transactional` in an `api` package.** A resource calls repository
  methods in sequence and returns records built from fully loaded results; it
  does not wrap them.
- A use case that needs two statements to succeed together gets **one**
  repository method in the feature's `domain` package that does both.

`ArchitectureTest.repositoriesAreTransactional` and `resourcesAreNotTransactional`
enforce it; [`../development.md`](../development.md#transaction-boundary) states
it.

## Consequences

- **The boundary is visible and checkable.** An agent adding a repository method
  inherits the class-level annotation, and a repository without it fails the build.
- **Nothing lazy leaves the repository.** A child collection not loaded by the
  repository's query fails with `LazyInitializationException` in the resource —
  loudly, in the first test — instead of quietly costing a query per row.
- **Behaviour is the same as auto-commit today.** One statement per transaction
  either way — this buys a declared place to put the second statement when it
  arrives, not a fix for a bug.
- **A page's requests are still separate transactions**, and deliberately so:
  wrapping them would mean holding a transaction across what the user does between
  them. [`../process.md`](../process.md#transaction-boundaries) says what that costs.
- **The check-then-act on duplicate pet names stays two transactions** with a
  click between them. The database's unique index is what closes that race — an
  interaction cannot be made atomic by annotating it.
