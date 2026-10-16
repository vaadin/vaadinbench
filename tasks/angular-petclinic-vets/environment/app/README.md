# AIUP PetClinic — Angular and Quarkus

The [AIUP PetClinic](https://github.com/AI-Unified-Process/petclinic) — the classic
[Spring PetClinic](https://github.com/spring-projects/spring-petclinic) rebuilt
specification-first with the [AI Unified Process](https://unifiedprocess.ai/) — on
a different stack: an Angular frontend and a Quarkus backend using JPA.

The specifications in [`docs/`](docs/) are the upstream project's, at the start of
its demo: every use case is specified, and only UC-001 (the welcome page and the
main menu) is built. What changes with the stack is
[`docs/architecture/`](docs/architecture/README.md) and the technical constraints
in [`docs/requirements.md`](docs/requirements.md).

## Stack

- **Java 25**, **Quarkus 3.40** — REST API under `/api`
- **Hibernate ORM with Panache** — JPA
- **Flyway** — database migrations
- **PostgreSQL**
- **Angular 22** with Angular Material — `frontend/`

## Build

```bash
cd frontend && npm run build && cd ..   # the frontend Quarkus serves
mvn test                                # API and browser tests
```

Licensed under the Apache License, Version 2.0, like the project it is derived from.
