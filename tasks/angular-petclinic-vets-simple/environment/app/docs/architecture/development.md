# Development View

**Question:** How is the code organized, built, and kept honest?  
**Artifacts:** this document, [`testing.md`](testing.md) (the testing half of this
view), `CLAUDE.md` (the short form an agent always has loaded)  
**Notation:** Markdown  
**For the agent:** the most important view, and the one to read **before writing
anything** in `src/main/java/` or `frontend/src/app/`. Every rule here saves a
correction in every use case.

Most of what follows is **enforced executably** by
`src/test/java/ai/unifiedprocess/petclinic/ArchitectureTest.java` (ArchUnit, runs
with `mvn test`). Each rule names the section it comes from, so a failure points
straight back here. If you change a convention below, change the matching rule in
the same commit — a rule that contradicts this document is the rule that's wrong.
ArchUnit reads Java only: the Angular conventions below are held by review and
by the tests, not by a sensor.

These rules are the harness for use cases that have **not been written yet**, so
a rule may legitimately match no classes — on a branch where only UC-001 exists
there is no `domain` package to check. `src/test/resources/archunit.properties`
sets `archRule.failOnEmptyShould=false` for exactly that reason: a rule about
code that does not exist yet is vacuously satisfied, the same stance the
traceability sensors take towards a use case that is not `Done` yet.

## Technology stack

| Concern               | Choice                                                         | Version         | Decided in                                                |
|-----------------------|----------------------------------------------------------------|-----------------|-----------------------------------------------------------|
| Language / runtime    | Java                                                           | 25              | C-001                                                     |
| Application framework | Quarkus, Quarkus REST with Jackson                             | 3.40            | C-002                                                     |
| UI                    | Angular SPA (standalone components, signals), Angular Material | 22.2            | [ADR-002](adr/ADR-002-angular-spa-and-rest-api.md), C-003 |
| Database access       | JPA: Hibernate ORM with Panache, repository pattern            | Quarkus-managed | [ADR-001](adr/ADR-001-jpa-with-panache.md), C-004         |
| Schema migration      | Flyway (`quarkus-flyway`), run at start-up                     | Quarkus-managed | [ADR-003](adr/ADR-003-flyway-owns-the-schema.md)          |
| Database              | PostgreSQL, local server on `localhost:5432` (`vb-postgres`)   | —               | C-005, C-007                                              |
| Build                 | Maven, single module; npm + Angular CLI in `frontend/`         | —               | —                                                         |
| Use-case tests        | `@QuarkusTest` + REST Assured, Playwright for Java             | Playwright 1.59 | [ADR-006](adr/ADR-006-two-test-layers-merged-coverage.md) |
| Component tests       | Vitest through `ng test`                                       | 5.0             | [ADR-006](adr/ADR-006-two-test-layers-merged-coverage.md) |
| Architecture tests    | ArchUnit                                                       | 1.5             | —                                                         |
| Coverage              | `quarkus-jacoco`, across both Java test layers                 | Quarkus-managed | —                                                         |
| Quality gate          | SonarQube Cloud                                                | —               | —                                                         |

Exact versions are in `pom.xml` and `frontend/package.json`, which win over this
table.

## Module layout

Single Maven module, **package by feature** under
`ai.unifiedprocess.petclinic`, and **folder by feature** under
`frontend/src/app`, with the same feature names on both sides. The features
follow the use cases — `owner`, `pet`, `visit`, `vet`, `welcome` — and each Java
feature package has exactly two sub-packages
(`ArchitectureTest.featuresHaveOnlyApiAndDomain`):

- **`api`** — the feature's REST resource and the records it returns. One
  resource per feature, one method per request a page makes.
- **`domain`** — JPA entities and their `<Entity>Repository` (Panache),
  mapping the tables Flyway creates. **No Spring, no active record.**

A feature's frontend folder holds its pages (one per use case / screen), its
components, and one HTTP service, `<feature>-service.ts`. `core/` holds what
every feature shares: SPA routing, error mapping, the error panel and pages.

```
src/main/java/ai/unifiedprocess/petclinic/
  <feature>/api/      <Feature>Resource + the records it returns
  <feature>/domain/   JPA entities + <Entity>Repository (Panache)
  core/               SpaRouting, ErrorMapper, CrashResource
src/main/resources/db/migration/     production schema
src/test/resources/db/migration/     test seed data (test classpath only)
frontend/src/app/
  app.ts, app.html, app.routes.ts    the shell and the route table
  <feature>/          pages, components, <feature>-service.ts
  core/error/         ErrorPanel, crash page, not-found page
frontend/dist/petclinic/browser/     the built frontend Quarkus serves (generated, never edited)
```

The project is intentionally thin on layers — **no service, DTO, or mapper
layer** beyond `api` + `domain` unless a use case demands one: no Java class is
named `*Service`, `*Dto` or `*DTO` (`ArchitectureTest.noServiceOrDtoLayer`). The
Angular `<feature>-service.ts` is not such a layer; it is the frontend's one
place for the feature's HTTP calls. Put query logic in the repository of the
entity it reads until duplication justifies extraction
([ADR-004](adr/ADR-004-package-by-feature-api-domain.md)).

There is no generated Java code: entities are written by hand against the
schema. The only build output in the tree, `frontend/dist`, is gitignored.

## Cross-feature rule — and its one exception

Cross-feature reach-in goes through the other feature's `domain` package, not
its `api`. A resource answers for its own feature; what it needs from another
feature it takes from that feature's repository — the owner details resource of
UC-005 lists the owner's pets through `PetRepository`
(`ArchitectureTest.foreignApiIsNeverUsed`; `core` is the shell and stays
reachable from every feature). The `domain` package never looks the other way:
it depends on no `api` (`domainDoesNotDependOnApi`) and on no JAX-RS type at all
(`domainIsFreeOfRest`), so an entity or a repository never knows it is served
over HTTP.

**Exception — routing.** The shell and pages that aggregate actions of other
features (the owner details page of UC-005 links to the add-pet, edit-pet, and
add-visit pages) reach other features' pages in exactly one form:

- a **route path** in a `routerLink` or `router.navigate(...)` —
  `routerLink="/owners/{{ owner.id }}/pets/new"` — no import of the other
  feature's component, no call into it, no state read; the path is only a
  routing key;
- a route nested under another feature's route (`/owners/:ownerId/pets/new`
  under `/owners/:ownerId`) uses that feature's **parameter name**, `ownerId`,
  rather than a second name for the same thing.

Anything else — a form, a service, a helper — is not shared across features in
the frontend; another feature's data arrives through the page's own resource,
which reads that feature's `domain`. No sensor sees this half of the rule.

Because of that exception the *feature*-level dependency graph is cyclic by
design (`owner` → `visit` → `pet` → `owner`), so
`ArchitectureTest.noCyclesBetweenFeatures` slices on `(*).domain..` instead: the
`domain` packages have no cross-feature dependencies at all and must stay that
way.

## Data access (JPA)

- **Panache repositories, not active record, nothing from Spring.** No class
  depends on `org.springframework..` (`ArchitectureTest.noSpring`). Entities live
  in their feature's `domain` (`entitiesLiveInDomain`) as plain mapped classes
  with public fields, and none extends `PanacheEntityBase`
  (`entitiesAreNotActiveRecords`): an entity carrying its own static queries is
  one call away from every class, and the transaction boundary would scatter.
- **Every mapping names its table and columns.** `@Table(name = "pets")`,
  `@Column(name = "birth_date")` — the schema is snake_case and Flyway's
  ([ADR-003](adr/ADR-003-flyway-owns-the-schema.md)); Hibernate never creates or
  checks it (`quarkus.hibernate-orm.schema-management.strategy=none`), so a
  mapping that is wrong fails only when a query touches it. Ids come from the
  table's sequence with `allocationSize = 1`, matching its `INCREMENT BY 1`;
  Hibernate's default of 50 would hand out ids the sequence also hands out.
- **An association across features is its foreign-key column.** `Pet` holds
  `Integer ownerId`, not an `Owner` — a reference to another feature's entity
  is a `domain` → `domain` dependency, the one thing the cycle rule forbids.
  Associations within a feature (`Pet` → `PetType`) are `@ManyToOne` or
  `@ManyToMany`, always `fetch = FetchType.LAZY`.
- **Queries are JPQL over entities, in the repository.** Through Panache's
  `find`, `list`, `count` and `persist`, never `createNativeQuery`. Whatever a
  page shows is loaded **in the repository's query**, with a fetch join:
  ```java
  public List<Pet> findByOwner(int ownerId) {
      return find("select p from Pet p join fetch p.type"
                      + " where p.ownerId = ?1 order by p.name, p.id", ownerId)
              .list();
  }
  ```
- **Parent→children collections use `left join fetch`** in the same query to
  avoid N+1; Hibernate 7 applies `range(offset, …)` to the parents in SQL, so a
  page stays one query. Any repository feeding a list with a to-many must do
  this — the vet directory with its specialties (UC-002) and the owner search
  results with their pet names (UC-004) both need it. Order the collection with
  `@OrderBy` and the list with `order by …, id`, so paging is stable (NFR-003).
- **Lazily loaded lists return `List<T>`** from a `findPage(int offset, int
  limit)`. The resource takes `offset` and `limit` as query parameters and caps
  the limit; the page asks for the next chunk as the user scrolls and stops when
  a chunk comes back short, so no `count()` is needed (NFR-001/002). A `List`,
  not a `Stream`: everything is loaded before the repository's transaction ends.
- **Entities never leave the server.** A resource returns records, never an
  entity or a collection of entities (`resourcesNeverReturnEntities`): Jackson
  would walk the lazy associations outside any transaction, and every column of
  the table would become part of the API by accident.
- **Entities stay validation-free.** No Bean Validation annotations, no
  constructor checks — `domainIsValidationFree` — the Angular form is the
  validation boundary (see below and
  [ADR-005](adr/ADR-005-validation-in-the-form.md)).

## Persistence stereotype

Persistence classes are named `<Entity>Repository` — not `*Queries`, `*Dao`,
`*Store` — and every Panache repository class carries that name
(`ArchitectureTest.panacheRepositoriesAreRepositories`). A repository is
`@ApplicationScoped`, lives in `domain`, and implements
`PanacheRepositoryBase<Entity, Integer>`
(`repositoriesAreNamedAndScoped`). `@ApplicationScoped` is not decoration: it
makes the repository a CDI bean, and only a bean gets its `@Transactional`
interceptor — a repository created with `new` silently runs without a
transaction. `PanacheRepositoryBase` rather than `PanacheRepository` because the
ids are `INTEGER`; the latter fixes them to `Long`.

There is no exception-translation layer: a database failure surfaces as a
Hibernate runtime exception, which `core/ErrorMapper` answers with its message.

## Transaction boundary

**The repository is the transaction boundary**
([ADR-009](adr/ADR-009-repository-is-the-transaction-boundary.md)). Every
repository carries `jakarta.transaction.Transactional` at class level
(`ArchitectureTest.repositoriesAreTransactional`), so each public method — read
or write — runs in a transaction, and a repository method that calls another
joins the one already open:

```java

@ApplicationScoped
@Transactional
public class PetRepository implements PanacheRepositoryBase<Pet, Integer> {

    public Integer insert(Pet pet) {
        persist(pet);
        return pet.id;
    }
}
```

- **Never in a resource.** No class in `api` uses `@Transactional`
  (`resourcesAreNotTransactional`). A transactional resource would let lazy
  associations load quietly while it builds its records — one query per row,
  invisible until the list grows. With the transaction in the repository, a
  collection the query did not fetch fails at once in a test. A resource calls
  repository methods one after the other; it does not wrap them.
- **There is no read-only flag.** `jakarta.transaction.Transactional` has none,
  so a read method must not change the entities it loads: a managed entity
  modified inside the transaction is flushed when it commits.
- **A use case needing two statements to succeed together** gets one method in
  the feature's repository that performs both — not two calls from the
  resource. What that means at runtime is in
  [`process.md`](process.md#transaction-boundaries).

## Angular page and REST resource conventions

- **One resource per feature, under `/api/`.** A `@Path` class outside `core`
  lives in `api` and is named `<Feature>Resource`
  (`ArchitectureTest.resourcesLiveInApi`), and every `@Path` starts with `/api/`
  (`resourcesAreUnderApi`) — `core/SpaRouting` answers every other path with
  `index.html` for the Angular router, so a resource outside `/api` would shadow
  a page. The resource takes its repositories by constructor and returns
  records nested in it, shaped for the page that shows them.
- **One page per use case, per route, inside the shell.** Every page is a
  standalone component `<name>-page.ts` in its feature folder, with one entry in
  `frontend/src/app/app.routes.ts`; the shell's `<router-outlet>` in `app.html`
  renders it, so the drawer and header stay usable on every screen (NFR-005).
  Put the UC id in the class doc comment so grep from a failing test lands on
  the right file.
- **State only for what the template reads.** Dependencies through `inject()`;
  what changes is a `signal`, a constant a `readonly` field; built-in control
  flow (`@if`, `@for`). HTTP calls go through the feature's service, returning
  typed observables of interfaces mirroring the resource's records.
- **Route parameters are component inputs.** `withComponentInputBinding()` is
  on, so `:ownerId` arrives as `ownerId = input.required<string>()`. **Check
  the route table for a parameter that already names the entity before adding a
  parameterized route** — every route under `/owners/…` uses `ownerId`,
  whichever feature owns the page.
- **Styling — never inline styles.** No `style="…"` attribute, no `[style]`
  binding; component styles (`styles` / `styleUrl`) and Angular Material
  components. `src/styles.css` holds only the page-wide basics. If neither covers
  what you need, ask rather than falling back to inline styles.
- **Main menu — the shell's `menu` in `app.ts`.** A `mat-nav-list` inside
  `<nav aria-label="Main menu">` in the drawer, `routerLinkActive` for the
  highlighting — not tabs, not links in the toolbar. It already links every
  top-level page; the toolbar holds only the drawer toggle, logo, and title.
- **Validation lives in the form, not the domain.** Every entity a use case
  writes has an `<entity>-form.ts` component — shared where several use cases
  edit the same entity — built on a reactive form whose validators run the use
  case's field-level rules. It emits a populated value only when the form is
  valid and shows field errors otherwise. It also exposes a way to put a late
  error — one discovered only by a database lookup, such as a duplicate name —
  onto the right control: the resource throws a `WebApplicationException` with
  status and message, `ErrorMapper` returns it as `{"message": …}`, and the page
  calls `setErrors` on the control. A page calls the API only after validation
  passes, and never validates by hand.
- **The form enforces the lengths of the entity model.** Every text column has a
  length in [`../entity_model.md`](../entity_model.md) and a matching `VARCHAR`
  in the migration; the form mirrors it with `Validators.maxLength(n)`, the
  `maxlength` attribute and a `<mat-error>` message, so an over-long value is a
  field error rather than a constraint violation on the error page.
- **The server holds no session state.** What the user is working on lives in
  the page's signals and form; the server keeps nothing between requests. A
  reload or deep link works from the URL alone: `core/SpaRouting` answers it
  with the app, and the page loads its data from the route parameters.
- **`NotFoundException` in the resource for missing entities.** When a lookup by
  path parameter finds nothing, the resource throws
  `jakarta.ws.rs.NotFoundException` with a message; `ErrorMapper` turns it into
  a 404 with that message, and the page shows it in the `ErrorPanel`. A path the
  Angular router does not know falls through to the `**` route,
  `NotFoundPage`.
- **Error pages render inside the shell.** The shell stays functional so the
  user is never stranded (NFR-005). `ErrorMapper` answers every failure with
  `{"message": …}` and logs the detail; `NotFoundPage`, `CrashPage` (route
  `/oups`, calling `/api/oups`) and every page whose request fails delegate
  rendering to `ErrorPanel`, and none shows a stack trace — message only
  (NFR-004).

## Flyway

- Production migrations: `src/main/resources/db/migration/V*.sql`. They
  reproduce [`../entity_model.md`](../entity_model.md) exactly (NFR-009) — the
  migrations effectively *are* the schema DSL. Quarkus runs them at start-up.
- Test seed data: `src/test/resources/db/migration/` (test classpath only —
  Flyway picks both up when tests run; `mvn quarkus:dev` adds it explicitly).
  The test and dev profiles clean and migrate the database on every start.
- **After changing a migration**, change the entity mapping by hand to match —
  nothing regenerates or checks it; a test running the affected query does.

## Build

```bash
mvn quarkus:dev                 # backend on :8080; the postgres database, migrated and seeded
cd frontend && npm start        # Angular dev server on :4200, proxying /api to :8080
cd frontend && npm run build    # the bundle Quarkus serves — browser tests need it
cd frontend && npm test         # Vitest component tests
mvn test                        # *Test: API and browser tests, ArchUnit, traceability sensors
mvn test -Dgroups=sensor        # only ArchUnit and the traceability sensors
mvn verify                      # additionally *IT: Playwright journeys, and the full coverage report
```

What `verify` does, in order:

1. **`initialize`** — `vb-postgres ensure petclinic_test` (exec-maven-plugin):
   the local PostgreSQL server runs and the test database exists.
2. **`process-resources`** — `frontend/dist/petclinic/browser` is copied to
   `META-INF/resources`, which Quarkus serves. Maven does **not** build the
   frontend: without `npm run build` first, there is simply no frontend, and a
   stale build tests yesterday's pages.
3. **`test`** (Surefire) — every `*Test`. `@QuarkusTest` starts the application
   once on a random port; Flyway cleans `petclinic_test` and migrates it with
   the seed; `quarkus-jacoco` records coverage.
4. **`verify`** (Failsafe) — every `*IT`, also `@QuarkusTest`, recording into
   the same coverage data.
5. **Coverage** — `quarkus-jacoco` writes `target/jacoco-report/jacoco.xml`, the
   report Sonar reads.

**The local PostgreSQL server is required** for `test` and `verify` (C-007);
there is no Docker, so Quarkus Dev Services are off. The exception is
`test -Dgroups=sensor`: it runs only the six classes tagged `sensor`, none of
which is a `@QuarkusTest`, so it starts no application and needs neither the
database nor a frontend build.

CI is `.github/workflows/build.yml`: the hook smoke test, then `npm run build`
and `npm test`, then `mvn -B verify` plus the Sonar scanner, on every push to
`main` and every pull request. `.github/workflows/aiup-generate.yml` is the
dispatch hook for AI Unified Process Studio and is not edited by hand.

## Agent guardrails

The traceability sensors and the ArchUnit rules are post-hoc — they catch drift
once somebody runs them. The Claude Code hooks in `.claude/hooks/`, wired up by
`.claude/settings.json`, cover what a test cannot reach: that the sensors ran in
this session, and that a `Status:` line was audited before it was set
([ADR-010](adr/ADR-010-hooks-guard-the-session.md),
[ADR-011](adr/ADR-011-guards-read-state-and-demand-evidence.md)):

| Hook                       | Event                          | What it does                                                                                                                                                                              |
|----------------------------|--------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `session-start.sh`         | `SessionStart`                 | records the commit the session started on and when, drops the previous session's markers, and takes a first reading of every `Status:` line                                              |
| `guard-spec-status.sh`     | `PreToolUse` (Edit, Write)     | refuses an edit that sets a `Status:` to `Done`, `Tested` or `Automated` unless a coverage audit of that specification finished in this session after the last change under `src/`        |
| `check-spec-status.sh`     | `PostToolUse` (every tool)     | rereads every `Status:` line after each tool call and reports one that now asserts coverage without an audit behind it — whichever tool changed it, `sed` and heredocs included           |
| `record-coverage-check.sh` | `SubagentStop` (`uc-coverage`) | records that `aiup-angular-jpa:coverage-check` audited a UC or TC: the evidence the two guards above ask for                                                                              |
| `record-sensor-run.sh`     | `PostToolUse` (Bash)           | after a Maven `test` / `verify`, records the change set the sensors saw — if, and only if, every sensor's Surefire report is fresh and green                                              |
| `require-sensors.sh`       | `Stop`                         | refuses to end a turn while `src/`, `docs/` or `pom.xml` changed and the sensors have not run since the newest change, or while a hook changed and `smoke.sh` has not                     |

Two rules shape them. **A guard reads the repository, not the tool call**: the
status check compares the files with what it saw last, so it holds for an edit
made through Bash as much as for one made through Edit. **Evidence is
positive**: a sensor run counts because `target/surefire-reports/TEST-*.xml`
says every sensor ran and passed after the change, never because the console
showed no failure — so a run in the background, piped through `tail`, narrowed
with `-Dtest=`, or skipped leaves no marker. An audit counts because the
`uc-coverage` agent finished, not because a status change said it did.

The run the Stop hook asks for is `mvn -q test -Dgroups=sensor`: the six sensor
classes carry the JUnit tag `sensor` (`@ArchTag` on the two ArchUnit classes),
and Surefire runs only those. None starts Quarkus, so it takes seconds and works
straight after a clone — there is nothing to generate. A full `mvn test` or
`verify` counts too.

`.claude/hooks/smoke.sh` exercises all of them against a throwaway repository,
records its own green run for the Stop hook, and runs in CI before the builds;
run it after changing one.

`.claude/settings.json` also allows the Maven and npm builds, the smoke test and
read-only git, so a fresh clone can satisfy the hooks without a permission
prompt, and denies edits under `target/`: build output is rebuilt, never
hand-edited. `.claude/settings.local.json` is gitignored, so a clone gets the
shared guardrails and none of the personal permission grants.

The rule for adding another: **if it can be checked by reading the repository, it
is a test, not a hook.** Hooks fire only inside a Claude Code session; an ArchUnit
rule binds CI, every editor, and every contributor.

## Coverage

Coverage is what the quality gate measures, and it spans **both** Java test
layers ([ADR-006](adr/ADR-006-two-test-layers-merged-coverage.md)). Every `*Test`
and `*IT` is a `@QuarkusTest`, and `quarkus-jacoco` records all of them into one
data file, so a line reached only by a Playwright `*IT` counts as covered —
**do not add a use-case test just to cover something a journey test already
exercises.** Only `mvn verify` covers both layers (`mvn clean verify` for a
number with nothing carried over); `mvn test` reports `*Test` alone. The report
measures the Java backend only, not the Angular code. The gate wants 80 % on
new code.

## Testing

The test conventions are the other half of this view and live in
[`testing.md`](testing.md): the Java test layers and what the class suffix
decides, the Vitest specs, `UC<NNN><Name>Test` naming, the `@UseCase` and
`@TestCase` annotations, the traceability sensors, `BrowserTest` and its
role-based locators, REST Assured against `/api`, and what you must **not** do.
Read it before writing or changing anything under `src/test/java/` or a
`*.spec.ts`.

## Definition of done for a use case

1. The `UC-NNN-*.md` specification is the input; where code and specification
   disagree, the specification wins (C-008).
2. The code follows the conventions above; `mvn test` passes, ArchUnit
   included, and so does `npm test` in `frontend/`.
3. At least one test per use case, each `@Test` carrying `@UseCase` (NFR-006).
4. `npm run build`, then `mvn verify`, is green and coverage has not dropped.
5. The `Status:` line is updated only after a coverage check.
6. If the change contradicts a view in this folder, the view changes with it —
   and a decision that could have gone the other way gets an [ADR](adr).
