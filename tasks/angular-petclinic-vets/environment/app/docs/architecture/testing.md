# Testing

Read this **before writing or modifying any test** under `src/test/java/` or any `*.spec.ts` under `frontend/`.

## Two Java test layers — the class suffix picks the Maven phase

| Layer               | Suffix     | Runs in      | Framework                                                          | Skill                              |
|---------------------|------------|--------------|--------------------------------------------------------------------|------------------------------------|
| Use case test       | `*Test`    | `mvn test`   | `@QuarkusTest` + Playwright for Java (`BrowserTest`), REST Assured | the backend test skill (`aiup-angular-jpa:spring-boot-test`) |
| Journey test        | `*IT`      | `mvn verify` | `@QuarkusTest` + Playwright for Java (`BrowserTest`), `@TestCase`  | `aiup-angular-jpa:playwright-test` |
| Component test      | `*.spec.ts`| `npm test`   | Vitest through `ng test`, beside the component                     | `aiup-angular-jpa:vitest-test`     |

Surefire picks up `*Test`, Failsafe picks up `*IT` (both declared in `pom.xml`). A use case test named `*IT` or a
journey test named `*Test` runs in the wrong phase, and `TestLayerConventionsTest` fails the build when one does: no
class whose name starts with `UC` may end in `IT`, and every class annotated `@TestCase` must. The rule is worth
enforcing because the failure is otherwise silent: nothing errors, the wrong Maven plugin simply never picks the class
up, and a test that never runs reports as passing. `UC<NNN>…Test` is the default for use cases, browser or not;
`*IT` is for test cases (`TC-*`), the end-to-end journeys. Vitest specs are a third, fast layer for a component's own
rendering logic; the Java sensors do not see them, so they never count towards a use case's coverage.

## Framework

- **`@QuarkusTest`** starts the application once per test run — the REST API, the database connection and the built
  Angular frontend Quarkus serves as static content — on a random port (`%test.quarkus.http.test-port=0`).
  Reference: https://quarkus.io/guides/getting-started-testing
- A use case test that exercises a page extends `BrowserTest` and drives headless Chromium through
  **Playwright for Java**. A test of the API alone uses **REST Assured** (`given().when().get("/api/…")`) and needs no
  base class (`UC010ViewApplicationErrorTest`). One class may do both. They all run against the same local PostgreSQL
  test database as the rest of the tests.
- **Use the backend test skill (`aiup-angular-jpa:spring-boot-test`)** to scaffold a test for a use case, and
  `aiup-angular-jpa:vitest-test` for a component spec. Prefer them over hand-rolling — they know the conventions in this
  file (locators, `@UseCase`, naming, etc.).
- **No Spring, no Testcontainers, no Vaadin.** Any `org.springframework.*`, `org.testcontainers.*` or `com.vaadin.*`
  import in a test is a mistake — the project is `@QuarkusTest`, Playwright and REST Assured. The frontend's
  `@playwright/test` dependency only pins the Chromium build both sides share; the browser tests are written in Java.

## Test class naming — `UC<NNN><UseCaseName>Test`

Tests that verify a use case are named after the use case, **not** the page or the resource. Format:
`UC<NNN><UseCaseNameInPascalCase>Test`, e.g `UC001ViewWelcomePageTest`, `UC004FindOwnersByLastNameTest`,
`UC007AddPetToOwnerTest`. The `NNN` matches the `UC-NNN-*.md` file in `docs/use_cases/`.

If a page or resource is touched by multiple use cases, write one `UC<NNN>…Test` class per use case rather than one
`XxxResourceTest` covering all of them. Keep the test file in the **package of the resource under test**
(`owner.api`), or the feature package for a page without a resource (`welcome`), and name the class after the UC.
A Vitest spec sits beside its component and names the use case in its `describe('UC-004: Find Owners by Last Name')`.

**Exception — cross-cutting tests.** `ArchitectureTest` (the ArchUnit rules behind
[`development.md`](development.md)), `TestLayerConventionsTest` and `BrowserTest` verify no single use case, so they
sit in the root package under their own names. Everything that *does* verify a use case follows the format above.

## `@UseCase` on every test method

Every `@Test` method that verifies UC behaviour must carry `ai.unifiedprocess.petclinic.UseCase` **on the method**
(never on the class — the annotation is `@Target(METHOD)` and cannot go on a class):

```java
@Test
@UseCase(id = "UC-004", scenario = "A2: Exactly One Match", businessRules = "BR-001")
void exactlyOneMatchNavigatesDirectlyToOwnerDetails() { ... }
```

- `id` — required, matches a `docs/use_cases/UC-NNN-*.md` file.
- `scenario` — optional, defaults to `"Main Success Scenario"`. Set it for alternative flows (`"A1: Validation Errors"`,
  `"A2: Owner not found"`).
- `businessRules` — optional array of BR IDs. One rule as `businessRules = "BR-001"`, several as
  `businessRules = {"BR-001", "BR-002"}` — **never** as one comma-separated string `"BR-001, BR-002"`.
  `UseCaseTraceabilityTest` rejects anything that is not a bare `BR-NNN` identifier.

- **Placement** — `@UseCase` belongs on a method of a `UC<NNN><Name>Test`, and nowhere else; the `NNN` in the class name
  must match the `id` it annotates. (`UseCaseTraceabilityTest` would also accept a `UC<NNN><Name>IT`, but
  `TestLayerConventionsTest` forbids that name here: a use case test that needs a browser is still a `*Test`.) A journey
  test does not borrow it to claim one of the use cases it walks through; that is what `@TestCase` is for.
  `UseCaseTraceabilityTest` enforces both halves.

This is the machine-readable spec → test link. `docs/` is the source of truth, so every test points at the exact spec
element it covers. Vitest specs cannot carry the annotation; their `describe` names the use case for the reader only.

## `@TestCase` on every journey test

Every `TC<NNN><Name>IT` class must carry `ai.unifiedprocess.petclinic.TestCase` **on the class** (never on a method —
the annotation is `@Target(TYPE)`), because a test case has exactly one coverage unit: the journey.

```java
@TestCase(id = "TC-003", useCases = {"UC-003", "UC-004", "UC-007", "UC-009"})
@QuarkusTest
class TC003VisitBookedForNewCustomerIT extends BrowserTest { ... }
```

- `id` — required, matches a `docs/test_cases/TC-NNN-*.md` file, and must agree with the `NNN` in the class name.
- `useCases` — required, the journey's itinerary: exactly the use cases the document's Flow table links, no more and no
  fewer. This is the part the class name cannot express, and it is what the annotation is for — add a step to the Flow
  without adding it here and `TestCaseTraceabilityTest` says so.

The two annotations split cleanly: `@UseCase` on a method of a `UC<NNN>…Test`, `@TestCase` on a `TC<NNN>…IT` class.
Each sensor rejects its own annotation anywhere else.

## The traceability sensors

`docs/` is the source of truth, and four sensors make that claim checkable. All run in `mvn test`, all read the
documents from disk rather than trusting a summary, and all list every violation at once so the failure reads as a
work list:

| Sensor                         | Reads                                        | Links the two sides by         |
|--------------------------------|----------------------------------------------|--------------------------------|
| `UseCaseTraceabilityTest`      | `docs/use_cases/` ↔ `@UseCase` methods       | the `@UseCase` annotation      |
| `TestCaseTraceabilityTest`     | `docs/test_cases/` ↔ `TC<NNN>…IT` classes    | the class name and `@TestCase` |
| `BusinessRuleTraceabilityTest` | `docs/business_rules.md` ↔ `docs/use_cases/` | the `GR-NNN` id and its link   |
| `RequirementTraceabilityTest`  | `docs/requirements.md` ↔ `docs/use_cases/`   | the `**Requirements:**` line   |

The shared markdown handling (the `**Status:**` pattern, the file scan, the violation report) lives in
`SpecDocuments`, so the document format is described in one place.

All six sensor classes — these four and the two ArchUnit classes, `ArchitectureTest` and `TestLayerConventionsTest` —
carry the JUnit tag `sensor`. `mvn -q test -Dgroups=sensor` runs them alone, in seconds: none of them is a
`@QuarkusTest`, so they start no Quarkus application and need no database or frontend build. It is the run the Stop
hook asks for (ADR-011); the full suite remains the definition of done.

### Use cases — `UseCaseTraceabilityTest`

It reads
every `docs/use_cases/*.md` and every `@UseCase` annotation on the test classpath and compares the two, in both
directions.

**Referential integrity** — applies to every use case, whatever its status. An annotation may not point at something
that does not exist:

- every `id` resolves to a `docs/use_cases/UC-NNN-*.md` file,
- every `scenario` is either the main success scenario or a literal `### A1: …` heading in that file,
- every `businessRules` entry is a `BR-NNN` identifier and a literal `### BR-NNN: …` heading in that file.

So renaming an alternative flow in a spec breaks the build until the annotations follow. That is the point.

**Coverage** — applies only to a use case whose `Status:` is `Done` or `Tested`. Such a use case must have a test for
its main success scenario, for *every* alternative flow, and for *every* business rule. Any other status (`Draft`,
`Specified`, …) is exempt, because this project writes the specification before the code and an unimplemented use case
is a normal intermediate state, not a defect.

The consequence is that **the `Status:` line is an assertion, not a label**. Setting it to `Done` switches the sensor
on for that use case; if the tests are not there, `mvn test` says so and names each gap. Use
`aiup-angular-jpa:coverage-check` (or the `aiup-angular-jpa:uc-coverage` sub-agent) before you change a status line.

When the sensor fails, the honest fixes are to write the missing test or to correct the status — never to weaken the
check.

### Business rules — `BusinessRuleTraceabilityTest`

This one links two documents rather than a document and a test. A rule several use cases share is stated once in
[`../business_rules.md`](../business_rules.md) as `GR-NNN`; each use case keeps its own `BR-NNN` heading and says which
shared rule it realizes. That saves the rule from being written down twice — but only while both sides still point at
each other, and markdown enforces nothing. What the sensor checks:

- every `GR-NNN` a use case references is a `## GR-NNN: …` heading in the catalogue,
- every link into the catalogue from anywhere under `docs/` lands on a heading that really exists — so a rule cannot be
  renamed without the documents pointing at it following along,
- the `**Realized by:**` line of a rule names exactly the use case rules that reference it, in both directions: a claim
  nobody honours fails, and a reference nobody claims fails too,
- the summary table at the top agrees with the rules below it — that table is the one duplication the catalogue keeps,
  which is why it is the one that has to be checked,
- every rule in the catalogue is realized by **at least two** use cases. A rule one use case needs belongs in that use
  case, where it is read together with the flow it constrains.

There is no status line here and nothing is exempt: unlike a use case, a shared rule has no "not written yet" state —
it exists or it does not.

### Requirements — `RequirementTraceabilityTest`

The first arrow of `Requirement → Use Case → … → Test`. Every use case names the requirements it realizes in a
`**Requirements:** [FR-014, FR-015, C-011](../requirements.md)` line right after its `**Status:**` — the format the
`aiup-core:use-case-spec` skill writes and `aiup-core:spec-review` reads — and
[`../requirements.md`](../requirements.md) mirrors it in a *Use Cases* column, so the trace reads from either end.
What the sensor checks:

- every id a use case lists is a row of the catalogue, and every use case the catalogue names exists,
- the *Use Cases* cell of a requirement names exactly the use cases whose `**Requirements:**` line lists it — in both
  directions, as with the `**Realized by:**` line of a shared rule,
- every `FR-NNN` is realized by at least one use case, and every use case realizes at least one `FR-NNN`. An `NFR-NNN`
  or `C-NNN` may stand without one (`—`): it constrains how the system is built or tested, and the architecture
  answers for it, not a use case,
- the requirements a `GR-NNN` comes from (the *From* column of the business rule catalogue) are listed by the use
  cases applying that rule, so the chain through `business_rules.md` does not skip a link,
- a requirement's status follows its use cases: *Verified* when every one is `Done` or `Tested`, *Implemented* when
  every one is at least `Implemented` — the same derivation `spec-review` reports as `REQ_STATUS_DRIFT`.

The link is made in the use case; the column is its mirror. When a use case starts realizing a requirement, add the id
to its `**Requirements:**` line and the use case to the requirement's row, and the sensor confirms the two agree.

### Test cases — `TestCaseTraceabilityTest`

A test case has one coverage unit — the journey — realized by one class named `TC<NNN><Name>IT` and carrying
`@TestCase`. The name and the annotation are both links, and the sensor makes them agree rather than letting either
drift: the id is checked against the class name, and `useCases` against the Flow table. That itinerary is the
annotation's
reason to exist — without it the annotation would only restate the class name, which is a second place for the same fact
to go wrong. What the sensor checks:

- every `**Status:**` is one of `Draft`, `Reviewed`, `Approved`, `Automated`, `Obsolete`,
- every `[UC-NNN](../use_cases/…)` link in a Flow row resolves to a file that really exists and really is that use case,
- a test case whose status is `Automated` has a `TC<NNN>…IT` class on the test classpath,
- every `TC<NNN>…IT` class has a test case document behind it — no orphan journeys,
- every `TC<NNN>…IT` class carries `@TestCase`, and nothing else does,
- the annotated `id` agrees with the class name,
- the annotated `useCases` are real use cases and are exactly the ones the Flow table links,
- an `Automated` test case only walks through use cases that are themselves `Done` or `Tested`; a green end-to-end test
  standing on a `Draft` use case means one of the two status lines is lying.

As with use cases, `Automated` is the assertion that switches the sensor on. Everything below it is exempt, because
this project writes the test case before the test.

## Test base class

A test that drives a page extends `ai.unifiedprocess.petclinic.BrowserTest` and carries `@QuarkusTest` itself
(the base class is not annotated):

- Launches one headless Chromium per test class (`@BeforeAll`) and opens a fresh `page` per test (`@BeforeEach`,
  default timeout 20 s), closing both afterwards — subclasses manage no browser lifecycle; the base class is the one
  place.
- `open(route)` navigates to a route of the application relative to the test server's root (`@TestHTTPResource("/")`):
  `open("")` for the welcome page, `open("owners/6")` for a parameterized one.
- **No per-test rollback.** The browser reaches the application over real HTTP, and each request commits its own
  transaction in the repository, so there is nothing for a test to roll back. A test that changes data puts the seed
  back in `@AfterEach` — targeted deletes, or an injected `Flyway` with `clean()` + `migrate()` — and must be idempotent.
- Seed-data IDs come from `src/test/resources/db/migration/V2__seed_reference_data.sql` (George Franklin is owner 1,
  Jean Coleman owner 6, …). When a test needs one, add a named constant to `BrowserTest` (`OWNER_FRANKLIN_ID`, …)
  rather than hard-coding a numeric ID in the test.
- For not-found / error flows open the location string, `open("owners/999999")`: the page asks the API, gets a 404
  with `{"message": …}`, and shows the error panel; an unknown route falls through to the `**` route (`NotFoundPage`).
- Tests that never open a page (`UC010ViewApplicationErrorTest`) do not extend it; `@QuarkusTest` and REST Assured are
  enough.

Test seed data lives **only** on the test classpath (`src/test/resources/db/migration/V2__seed_reference_data.sql`). Do
**not** put seed data under `src/main/resources/db/migration/`.

## Core Playwright and REST Assured API

- `open("owners/find")` — loads the route in the browser; `page` is the scope for every locator.
- `page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Find Owner"))` — find an element by its
  accessibility role and accessible name. This is the preferred locator. Others, in order of preference:
    - `page.getByLabel("Last name")` — a form field by its label.
    - `page.getByText("…")` — rendered text that has no role of its own.
    - `locator.filter(new Locator.FilterOptions().setHasText("Davis"))`, `locator.nth(i)` — narrow a list.
- `locator.fill("Dav")` / `locator.click()` / `locator.press("Enter")` — simulate user interaction through the real
  browser; Playwright waits until the element is visible, enabled and stable before acting.
- `PlaywrightAssertions.assertThat(locator)` — `isVisible()`, `hasText(…)`, `hasCount(n)`, `hasAttribute(…)`;
  `assertThat(page).hasURL(…)`. Every one retries until it holds or the timeout expires.
- REST Assured for the API: `given().queryParam("lastName", "Dav").when().get("/api/owners").then().statusCode(200)
  .body("lastName", contains("Davis", "Davis"))` — the base URI and port are set by `@QuarkusTest`.

## No direct field access — use locators

A Java test cannot reach an Angular component's fields at all: the component lives in the browser. Tests locate what
the user sees — by role, label and text — in the rendered page. This decouples the test from the template's internal
layout and exercises the whole path, HTTP and API included. Never `page.locator("css or xpath")`.

- Find input fields by label: `page.getByLabel("Last name")`
- Find buttons by name: `page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Find Owner"))`
- Find the result table (usually one per page): `page.getByRole(AriaRole.TABLE)`
- Scope to a parent: `menu.getByRole(AriaRole.LINK)`
- Verify rendered text: `assertThat(page.getByText("Jane Doe")).isVisible()`

Interact through the locator:

```java
page.getByLabel("First Name").fill("Jane");
page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add Owner")).click();
page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Type")).click();   // mat-select
page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName("dog")).click();
```

Check validation state via the locator (Angular Material marks an invalid field `aria-invalid`):

```java
assertThat(page.getByLabel("Telephone")).hasAttribute("aria-invalid", "true");
assertThat(page.getByText("not found")).isVisible();
```

In a Vitest spec the same rule holds: query the rendered DOM (`fixture.nativeElement.querySelector('nav[aria-label="Main
menu"]')`), not the component's signals.

## Scoping a locator vs. a page-wide locator

Scope a locator to its container when the page contains the same thing more than once — a row of a table, the
`"Main menu"` navigation, one of two forms with a field of the same label:

```java
Locator menu = page.getByRole(AriaRole.NAVIGATION, new Page.GetByRoleOptions().setName("Main menu"));
Locator davis = page.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHasText("Harold"));
assertThat(davis.getByRole(AriaRole.CELL).nth(1)).hasText("Davis");
```

A page-wide `page.getByRole(…)` is fine when the role and name are unique on the page.

## Positive vs. negative locator assertions

**Element should exist** — assert it is visible:

```java
assertThat(page.getByRole(AriaRole.IMG,
        new Page.GetByRoleOptions().setName("Pets at the clinic"))).isVisible();
```

**Element should NOT exist** — Angular's `@if` removes an element from the DOM rather than hiding it, so assert the
locator matches nothing:

```java
assertThat(page.getByRole(AriaRole.TABLE)).hasCount(0);
```

For the API, a negative is a body matcher: `.body(not(containsString("at ai.unifiedprocess")))`.

## No public test-only getters

Do **not** add anything to a component or a resource so tests can reach it: no `data-testid` attributes, no public
fields, no test-only endpoints. Tests use roles, labels and text. If a role locator cannot find an element, the page is
missing an accessible name, and the fix is an `aria-label` the user benefits from too.

## Assertions

- **Never `assertTrue(locator.isVisible())`** or `assertNotNull` on a locator. `isVisible()` is a snapshot taken
  before Angular may have rendered, and a locator is never null — both prove nothing about rendering. Use
  `PlaywrightAssertions.assertThat`, which waits.
- **Always go through `assertThat(locator)`**, which retries and names the locator in the failure:
  ```java
  assertThat(page.getByRole(AriaRole.IMG,
          new Page.GetByRoleOptions().setName("PetClinic logo"))).isVisible();
  assertThat(page.getByRole(AriaRole.NAVIGATION, new Page.GetByRoleOptions().setName("Main menu"))
          .getByRole(AriaRole.LINK)).hasText(new String[] {"Home", "Find Owners", "Veterinarians", "Error"});
  ```
  Never `Thread.sleep` or `page.waitForTimeout` to "let the page settle" — a fixed wait is either too short or wasted.
- **Never `.allTextContents().stream().anyMatch(...)`.** Narrow the locator (`filter`, scoping) or assert the whole list
  with `hasText(String[])` — the failure message then shows expected against actual.
- **Rendered-state assertions** go through role, label and text locators and assert with `hasText`, so the render path
  is exercised end-to-end. **API assertions** use REST Assured's `body(path, matcher)` on the JSON, not a deserialized
  object poked field by field.
- **Navigation assertions** check `assertThat(page).hasURL(Pattern.compile(".*/owners/1$"))`, not domain state pulled
  back out of the API.

## Integration test configuration

There is no Docker and no Testcontainers. The tests use a local PostgreSQL server on `localhost:5432`
(`vb-postgres start`); every Maven build makes sure it is running and that the `petclinic_test` database exists
(`exec-maven-plugin`, `vb-postgres ensure petclinic_test`). Quarkus Dev Services are off. The `%test` profile in
`application.properties` points at `petclinic_test`, and Flyway **cleans and migrates** it when the test application
starts, applying `src/test/resources/db/migration/V2__seed_reference_data.sql` with the schema — every run starts from
the same seed. `mvn quarkus:dev` does the same against the `postgres` database.

## Playwright `*IT` tests

Journey tests are the second Java layer, not a replacement for the use case tests. Rules that differ from the above:

- Class name `TC<NNN><TestCaseName>IT` for a test case document under `docs/test_cases/` (e.g.
  `TC003VisitBookedForNewCustomerIT`), in `ai.unifiedprocess.petclinic.e2e`. A use case test is never `*IT`, even when
  it needs a browser (`TestLayerConventionsTest`).
- `@QuarkusTest`, extend `BrowserTest`, annotate `@TestCase`. Playwright for Java (`com.microsoft.playwright:playwright`,
  test scope) is already in `pom.xml`; nothing else is needed.
- **Build the frontend first** (`cd frontend && npm run build`): Quarkus serves `frontend/dist/petclinic/browser`, and a
  missing build is simply no frontend — every browser test then fails to find anything.
- Locate elements by role, label and text as above — never raw `page.locator(...)`, never `Thread.sleep`.
- Data a journey creates is cleaned up in `@AfterEach` (targeted deletes, or Flyway clean + migrate, idempotent). A test
  case's **Postconditions** section is the cleanup contract.
- `@TestCase` on the class; `@UseCase` never appears here.
- Run with `mvn verify` (all) or `mvn verify -Dit.test=TC003VisitBookedForNewCustomerIT`;
  `BrowserTest` always launches headless — set `PWDEBUG=1` to watch the browser with the Playwright Inspector.
- Coverage: `quarkus-jacoco` writes the report of the `@QuarkusTest` runs to `target/jacoco-report`.
