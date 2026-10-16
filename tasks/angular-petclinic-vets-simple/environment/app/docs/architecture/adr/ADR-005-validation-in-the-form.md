# ADR-005: Validation lives in the Angular form, not in the entity

**Status:** Accepted   
**Date:** 2026-09-17   
**Affects:** Logical View, Development View

## Context

Every use case that writes data states its validation as user-visible behaviour:
the offending field is marked, the message names the problem, and the user's
other entries survive (FR-023, and the alternative flows of UC-003, UC-006,
UC-007, UC-008, UC-009). Validation is therefore not only a guard against bad
data — it is a specified interaction.

Bean Validation annotations on the entities would put the rule in a second
place: an exception thrown after the fact, on the server, which the page would
then have to translate back into a field-level error message. Two implementations
of the same rule, one of which cannot produce the specified behaviour on its own.

## Decision

**The form is the validation boundary.** A feature's page builds an Angular
reactive form whose validators carry the field-level rules; the page submits only
a valid form, and shows each error on its field. An error discovered only by a
database lookup — the duplicate pet name — comes back from the resource as
`{"message": …}` and the page sets it on the named control (`setErrors`).

**The domain stays validation-free:** no `jakarta.validation` annotations on
entities, no constructor checks. `ArchitectureTest.domainIsValidationFree`
enforces it.

## Consequences

- **The specified behaviour is the implementation,** not a translation of an
  exception into it.
- **An entity can be constructed in an invalid state** — in tests, for instance.
  Accepted: the database's `NOT NULL`, length limits and foreign keys are the
  backstop against a genuinely broken write.
- **The API is a second entry point without the form's validation**
  ([ADR-002](ADR-002-angular-spa-and-rest-api.md)). On the trusted network
  (C-010) the database constraints are its only guard; exposing the API to any
  other client means revisiting this decision first.
- **Rules that need the database stay in the resource** (duplicate pet name),
  which is why [`../process.md`](../process.md) documents that check-then-act
  explicitly.
