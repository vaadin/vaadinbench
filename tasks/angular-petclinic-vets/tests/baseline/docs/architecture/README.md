# Architecture

How the AIUP PetClinic is built. The use cases under [`../use_cases/`](../use_cases)
say *what* the application does; this document says *how*: the stack, where code
goes, the conventions it follows and how it is tested. Read it before implementing
a use case.

## Stack

| Layer          | Technology                                         | Notes                                                      |
|----------------|----------------------------------------------------|------------------------------------------------------------|
| Runtime        | Java 25                                            | C-001                                                      |
| Backend        | Quarkus 3.40, Quarkus REST with Jackson            | C-002; the API lives under `/api`                          |
| Persistence    | JPA: Hibernate ORM with Panache                    | C-004; entities map the schema, never create it            |
| Schema         | Flyway, `src/main/resources/db/migration`          | C-006; migrations run at start-up                          |
| Database       | PostgreSQL on `localhost:5432` (`vb-postgres`)     | C-005, C-007; no Docker, so no Dev Services                |
| Frontend       | Angular 22, standalone components, signals         | C-003; `frontend/`, Angular Material available             |
| Frontend tests | Vitest through `ng test`                           | component tests next to the component, `*.spec.ts`         |
| Browser tests  | Playwright for Java in `@QuarkusTest`s             | `src/test/java`, extending `BrowserTest`                   |

## Layout

Both halves are organised **by feature** — one package or folder per area of the
domain (`vet`, `owner`, `pet`, `visit`), plus `core` for what every feature shares
(the shell, error handling). A feature's backend and frontend use the same name.

```text
src/main/java/ai/unifiedprocess/petclinic/
  core/                  SPA routing, error mapping, the /oups demonstration
  <feature>/             entities, repositories and the REST resource of a feature
src/main/resources/
  application.properties
  db/migration/          Flyway migrations: the schema (C-006)
src/test/java/ai/unifiedprocess/petclinic/
  BrowserTest.java       base class of the browser tests
  <feature>/             UC<NNN><Name>Test classes
src/test/resources/db/migration/
  V2__seed_reference_data.sql   test data: the Spring PetClinic reference set
frontend/src/app/
  app.ts, app.html       the shell: header, main menu, router outlet (UC-001)
  app.routes.ts          one route per page
  core/error/            the error panel and pages (UC-010)
  <feature>/             pages, components and services of a feature
```

## Backend conventions

- **REST.** One resource class per feature, `@Path("/api/<feature>")`, returning
  records shaped for the page that shows them — not entities. Lists that grow with
  the clinic's data are fetched a page at a time (GR-002): the resource takes an
  offset and a limit, caps the limit, and the repository applies both in SQL.
- **JPA.** Entities map the tables Flyway creates, with explicit `@Table` and
  `@Column` names (`first_name`, not `firstName`); Hibernate's schema management is
  off. Fetch associations lazily, and load the collections a page shows without
  a query per row: `@BatchSize` on the association, or a fetch join in the page
  query.
- **Ordering.** Every list has a defined order (GR-003), applied in the query, with
  the primary key as the final tie-breaker so paging is stable.
- **Transactions.** A resource method that reads lazily loaded associations runs in
  a transaction (`@Transactional`); repositories are `@ApplicationScoped` Panache
  repositories.
- **Errors.** Every failure reaches the browser as `{"message": "…"}` with no stack
  trace (UC-010 BR-003); `core/ErrorMapper` does this for every resource.

## Frontend conventions

- Standalone components, `inject()` for dependencies, signals for state, and the
  built-in control flow (`@if`, `@for`). One component per file, named after what
  it is (`vets-page.ts`), template and styles beside it.
- A page is a routed component in its feature folder; HTTP calls go through a
  service in the same folder, which returns typed observables.
- Use semantic HTML: a list of records is a `<table>` with `<th scope="col">`
  headers (or an Angular Material table), so it is accessible and testable by role.
- The shell (`app.html`) owns the header and the main menu. The menu already links
  every top-level page: Home `/`, Find Owners `/owners/find`, Veterinarians `/vets`,
  Error `/oups`.
- Lazy loading (GR-002) means infinite scrolling: rows are fetched as the user
  scrolls towards the end of the list, with no page controls.

## Testing

Three layers, each named after the use case it verifies (`UC<NNN><Name>…`):

| Layer          | Where                                    | Runs with                     |
|----------------|------------------------------------------|-------------------------------|
| API            | `@QuarkusTest` + REST Assured, `src/test/java` | `mvn -o test`           |
| Browser        | `@QuarkusTest` extending `BrowserTest`   | `mvn -o test` (frontend built first) |
| Component      | `*.spec.ts` beside the component         | `npm test` in `frontend/`     |

The browser tests drive the application exactly as Quarkus serves it — the API and
the built frontend — so build the frontend before running them. Test data comes from
the Flyway seed in `src/test/resources/db/migration`; a test that needs other data
writes it itself.

## Build and run

```bash
# Frontend: build it (Quarkus serves frontend/dist), test it
cd frontend && npm run build && npm test && cd ..

# Backend: every test, API and browser
mvn -o test

# Run it: the backend on :8080 with the seed data …
mvn -o quarkus:dev
# … and, for frontend work, the Angular dev server on :4200, proxying /api to :8080
cd frontend && npm start
```

PostgreSQL runs in this container; `vb-postgres start` starts it, and every Maven
build does so for you. The test profile uses the `petclinic_test` database, emptied
and migrated whenever the test application starts.
