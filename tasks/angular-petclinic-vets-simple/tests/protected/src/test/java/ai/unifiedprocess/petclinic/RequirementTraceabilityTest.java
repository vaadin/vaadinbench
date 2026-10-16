package ai.unifiedprocess.petclinic;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ai.unifiedprocess.petclinic.SpecDocuments.assertNoViolations;
import static java.util.stream.Collectors.toCollection;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Traceability sensor for {@code docs/requirements.md}.
 *
 * <p>Every use case names the requirements it realizes in a
 * {@code **Requirements:**} line of its Overview, and the catalogue mirrors that
 * in a <em>Use Cases</em> column, so the trace can be read from either end. Two
 * copies of the same link drift as soon as one of them is edited alone, and a
 * requirement nobody realizes is invisible until someone looks for it. This
 * sensor closes the first arrow of {@code Requirement → Use Case → … → Test},
 * the way {@link BusinessRuleTraceabilityTest} and
 * {@link UseCaseTraceabilityTest} close the ones after it.
 *
 * <p>What it checks:
 *
 * <ul>
 *   <li><strong>Referential integrity</strong> — every id a use case lists is a
 *       row of the catalogue, and every use case the catalogue lists exists.</li>
 *   <li><strong>Both directions agree</strong> — the <em>Use Cases</em> cell of
 *       a requirement names exactly the use cases whose
 *       {@code **Requirements:**} line lists it.</li>
 *   <li><strong>Every functional requirement is realized</strong> by at least
 *       one use case, and every use case realizes at least one functional
 *       requirement. Non-functional requirements and constraints may stand
 *       without one: they shape how the system is built, not what a user does.</li>
 *   <li><strong>Shared rules carry their origin along</strong> — the
 *       requirements a {@code GR-NNN} comes from are listed by the use cases
 *       applying it, so the chain through {@code docs/business_rules.md} does
 *       not skip a link.</li>
 *   <li><strong>The status follows the use cases</strong> — a requirement is
 *       <em>Verified</em> exactly when every use case realizing it is
 *       <em>Done</em> or <em>Tested</em>; the same derivation
 *       {@code /spec-review} applies.</li>
 * </ul>
 *
 * <p>Failures list every violation at once rather than stopping at the first,
 * so the report reads as a work list.
 */
@Tag("sensor")
class RequirementTraceabilityTest {

    private static final Path DOCS_DIR = Path.of("docs");
    private static final Path CATALOGUE = DOCS_DIR.resolve("requirements.md");
    private static final Path BUSINESS_RULES = DOCS_DIR.resolve("business_rules.md");
    private static final Path USE_CASE_DIR = DOCS_DIR.resolve("use_cases");

    /** {@code | FR-001 | Welcome Page | As a visitor, … | UC-001 | High | Verified |} */
    private static final Pattern CATALOGUE_ROW =
            Pattern.compile("^\\|[ \t]*((?:FR|NFR|C)-\\d{3})[ \t]*\\|(.*)\\|[ \t]*$", Pattern.MULTILINE);
    /** The header row of a catalogue table, which names the columns. */
    private static final Pattern CATALOGUE_HEADER =
            Pattern.compile("^\\|[ \t]*ID[ \t]*\\|(.*)\\|[ \t]*$", Pattern.MULTILINE);
    private static final String USE_CASES_COLUMN = "Use Cases";
    private static final String STATUS_COLUMN = "Status";

    /** {@code **Requirements:** [FR-014, FR-015, C-011](../requirements.md)} */
    private static final Pattern REQUIREMENTS_LINE =
            Pattern.compile("^\\*\\*Requirements:\\*\\*[ \t]*+(.*)$", Pattern.MULTILINE);
    private static final Pattern USE_CASE_ID_LINE =
            Pattern.compile("^\\*\\*Use Case ID:\\*\\*[ \t]*(UC-\\d{3})[ \t]*$", Pattern.MULTILINE);
    /** {@code | GR-006 | Unique Pet Name per Owner | UC-007 BR-001, … | FR-015 |} */
    private static final Pattern SHARED_RULE_ROW =
            Pattern.compile("^\\|[ \t]*(GR-\\d{3})[ \t]*\\|[^|]*\\|[^|]*\\|([^|]*)\\|", Pattern.MULTILINE);

    private static final Pattern REQUIREMENT_ID = Pattern.compile("(?:FR|NFR|C)-\\d{3}");
    private static final Pattern USE_CASE_ID = Pattern.compile("UC-\\d{3}");
    private static final Pattern SHARED_RULE_ID = Pattern.compile("GR-\\d{3}");

    /** How far a use case has come, by its {@code **Status:**}; see {@code /spec-review}. */
    private static final Map<String, Integer> USE_CASE_PROGRESS = Map.of(
            "draft", 0, "reviewed", 0, "approved", 0, "implemented", 1, "tested", 2, "done", 2);
    private static final List<String> REQUIREMENT_PROGRESS = List.of("Open", "In Progress", "Implemented", "Verified");

    private static Map<String, Requirement> catalogue;
    private static Map<String, UseCase> useCases;

    /** One row of {@code docs/requirements.md}. */
    private record Requirement(String id, Set<String> claimedUseCases, String status) {

        boolean functional() {
            return id.startsWith("FR-");
        }
    }

    /** The parts of one {@code docs/use_cases/UC-NNN-*.md} this sensor reads. */
    private record UseCase(String id, String status, Set<String> requirements, Set<String> sharedRules, Path file) {
    }

    @BeforeAll
    static void loadCatalogueAndUseCases() throws IOException {
        catalogue = readCatalogue();
        useCases = readUseCases();
    }

    // --- Guard -----------------------------------------------------------------

    /**
     * Without this, a wrong working directory would make every check below pass
     * over an empty input set, a sensor that reports green because it is blind.
     */
    @Test
    void catalogueAndUseCasesAreDiscovered() {
        assertFalse(catalogue.isEmpty(),
                "No FR-/NFR-/C- rows found in " + CATALOGUE.toAbsolutePath()
                        + " Tests must run with the project root as working directory");
        assertFalse(useCases.isEmpty(), "No use case specification found in " + USE_CASE_DIR.toAbsolutePath());
    }

    // --- Referential integrity -------------------------------------------------

    @Test
    void everyListedRequirementExists() {
        assertNoViolations("Use cases listing a requirement that is not in the catalogue",
                useCases.values().stream().flatMap(useCase -> useCase.requirements().stream()
                        .filter(id -> !catalogue.containsKey(id))
                        .map(id -> useCase.id() + " (" + useCase.file() + ") lists " + id
                                + ", which is no row in " + CATALOGUE)));
    }

    @Test
    void everyListedUseCaseExists() {
        assertNoViolations("Requirements naming a use case that does not exist",
                catalogue.values().stream().flatMap(requirement -> requirement.claimedUseCases().stream()
                        .filter(id -> !useCases.containsKey(id))
                        .map(id -> requirement.id() + " names " + id + " in its '" + USE_CASES_COLUMN
                                + "' cell, but there is no " + USE_CASE_DIR + "/" + id + "-*.md")));
    }

    // --- Both directions agree -------------------------------------------------

    @Test
    void theCatalogueAndTheUseCasesAgree() {
        List<String> violations = new ArrayList<>();
        catalogue.values().forEach(requirement -> {
            Set<String> actual = useCasesRealizing(requirement.id());
            requirement.claimedUseCases().stream()
                    .filter(useCases::containsKey)
                    .filter(claimed -> !actual.contains(claimed))
                    .forEach(claimed -> violations.add(requirement.id() + " names " + claimed
                            + ", but the '**Requirements:**' line of " + claimed + " does not list " + requirement.id()));
            actual.stream()
                    .filter(found -> !requirement.claimedUseCases().contains(found))
                    .forEach(found -> violations.add(found + " lists " + requirement.id()
                            + ", but the '" + USE_CASES_COLUMN + "' cell of " + requirement.id() + " does not name it"));
        });
        assertNoViolations("Requirements whose '" + USE_CASES_COLUMN + "' cell disagrees with the use cases",
                violations.stream());
    }

    // --- Coverage --------------------------------------------------------------

    @Test
    void everyFunctionalRequirementIsRealized() {
        assertNoViolations("Functional requirements no use case realizes",
                catalogue.values().stream()
                        .filter(Requirement::functional)
                        .filter(requirement -> useCasesRealizing(requirement.id()).isEmpty())
                        .map(requirement -> requirement.id() + " is listed by no use case's '**Requirements:**' line."
                                + " Either a use case realizes it, or it is not a functional requirement"));
    }

    /**
     * The other direction: a use case that answers to no functional requirement
     * is behaviour nobody asked for, or a requirement nobody wrote down.
     */
    @Test
    void everyUseCaseRealizesAFunctionalRequirement() {
        assertNoViolations("Use cases realizing no functional requirement",
                useCases.values().stream()
                        .filter(useCase -> useCase.requirements().stream().noneMatch(id -> id.startsWith("FR-")))
                        .map(useCase -> useCase.id() + " (" + useCase.file() + ") lists no FR-NNN in its"
                                + " '**Requirements:**' line"));
    }

    // --- Shared rules carry their origin along ---------------------------------

    /**
     * {@code docs/business_rules.md} records which requirements each shared rule
     * comes from. Those requirements are realized by the use cases applying the
     * rule, so the trace from requirement to use case must not depend on reading
     * a third document. A rule may come from several requirements that each of
     * its use cases realizes only one of — {@code GR-008} serves adding a pet in
     * one and updating it in the other — so the check is per rule: every origin
     * is listed by some use case applying it, and every such use case lists some
     * origin.
     */
    @Test
    void useCasesListTheRequirementsBehindTheirSharedRules() throws IOException {
        List<String> violations = new ArrayList<>();
        sharedRuleOrigins().forEach((rule, origins) -> {
            List<UseCase> applying = useCases.values().stream()
                    .filter(useCase -> useCase.sharedRules().contains(rule))
                    .toList();
            origins.stream()
                    .filter(id -> applying.stream().noneMatch(useCase -> useCase.requirements().contains(id)))
                    .forEach(id -> violations.add(rule + " comes from " + id + ", but no use case applying "
                            + rule + " lists " + id + " in its '**Requirements:**' line"));
            applying.stream()
                    .filter(useCase -> useCase.requirements().stream().noneMatch(origins::contains))
                    .forEach(useCase -> violations.add(useCase.id() + " realizes " + rule + ", which comes from "
                            + String.join(", ", origins) + ", but its '**Requirements:**' line lists none of them"));
        });
        assertNoViolations("Shared rules whose origin is lost on the way to the use cases", violations.stream());
    }

    // --- The status follows the use cases --------------------------------------

    @Test
    void theStatusOfARequirementFollowsItsUseCases() {
        List<String> violations = new ArrayList<>();
        catalogue.values().stream()
                .filter(requirement -> REQUIREMENT_PROGRESS.contains(requirement.status()))
                .forEach(requirement -> {
                    List<UseCase> realizing = useCasesRealizing(requirement.id()).stream().map(useCases::get).toList();
                    String derived = derivedStatus(realizing);
                    if (derived != null && !derived.equals(requirement.status())) {
                        violations.add(requirement.id() + " is '" + requirement.status() + "', but its use cases make it '"
                                + derived + "' (" + String.join(", ", realizing.stream()
                                .map(useCase -> useCase.id() + " " + useCase.status()).toList()) + ")");
                    }
                });
        assertNoViolations("Requirements whose status disagrees with their use cases", violations.stream());
    }

    /** Verified when every use case is Done or Tested, Implemented when every one is at least Implemented. */
    private static String derivedStatus(List<UseCase> realizing) {
        if (realizing.isEmpty()) {
            return null;
        }
        List<Integer> ranks = new ArrayList<>();
        for (UseCase useCase : realizing) {
            Integer rank = USE_CASE_PROGRESS.get(useCase.status().toLowerCase());
            if (rank == null) {
                return null;
            }
            ranks.add(rank);
        }
        int min = ranks.stream().mapToInt(Integer::intValue).min().orElseThrow();
        int max = ranks.stream().mapToInt(Integer::intValue).max().orElseThrow();
        if (min == 2) {
            return "Verified";
        }
        if (min == 1) {
            return "Implemented";
        }
        return max > 0 ? "In Progress" : "Open";
    }

    // --- Reading the two sides -------------------------------------------------

    private static Map<String, Requirement> readCatalogue() throws IOException {
        if (!Files.isRegularFile(CATALOGUE)) {
            return Map.of();
        }
        Map<String, Requirement> parsed = new LinkedHashMap<>();
        // Each table names its own columns, so the cells are looked up by header
        // rather than by position: the three tables have different widths.
        List<String> columns = List.of();
        for (String line : Files.readString(CATALOGUE).split("\\R")) {
            Matcher header = CATALOGUE_HEADER.matcher(line);
            if (header.matches()) {
                columns = cells(header.group(1));
                continue;
            }
            Matcher row = CATALOGUE_ROW.matcher(line);
            if (row.matches()) {
                List<String> cells = cells(row.group(2));
                String useCaseCell = cell(cells, columns, USE_CASES_COLUMN);
                if (useCaseCell == null) {
                    throw new IllegalStateException("The table holding " + row.group(1) + " in " + CATALOGUE
                            + " has no '" + USE_CASES_COLUMN + "' column");
                }
                parsed.put(row.group(1), new Requirement(row.group(1), idsIn(USE_CASE_ID, useCaseCell),
                        String.valueOf(cell(cells, columns, STATUS_COLUMN))));
            }
        }
        return parsed;
    }

    /** The cells after the {@code ID} column, header and row alike. */
    private static List<String> cells(String afterId) {
        return Arrays.stream(afterId.split("\\|", -1)).map(String::trim).toList();
    }

    private static String cell(List<String> cells, List<String> columns, String column) {
        int index = columns.indexOf(column);
        return index < 0 || index >= cells.size() ? null : cells.get(index);
    }

    private static Map<String, UseCase> readUseCases() throws IOException {
        Map<String, UseCase> parsed = new LinkedHashMap<>();
        for (Path file : SpecDocuments.filesIn(USE_CASE_DIR, "UC-")) {
            String markdown = Files.readString(file);
            String id = SpecDocuments.require(USE_CASE_ID_LINE, markdown, file, "**Use Case ID:** UC-NNN");
            String status = SpecDocuments.require(SpecDocuments.STATUS, markdown, file, "**Status:** …");
            String requirements = SpecDocuments.require(REQUIREMENTS_LINE, markdown, file,
                    "**Requirements:** [FR-NNN, …](../requirements.md)");
            parsed.put(id, new UseCase(id, status, idsIn(REQUIREMENT_ID, requirements),
                    idsIn(SHARED_RULE_ID, markdown), file));
        }
        return parsed;
    }

    /** {@code GR-NNN} → the requirement ids in the <em>From</em> column of the business rule catalogue. */
    private static Map<String, Set<String>> sharedRuleOrigins() throws IOException {
        Map<String, Set<String>> origins = new LinkedHashMap<>();
        Matcher row = SHARED_RULE_ROW.matcher(Files.readString(BUSINESS_RULES));
        while (row.find()) {
            origins.put(row.group(1), idsIn(REQUIREMENT_ID, row.group(2)));
        }
        return origins;
    }

    // --- Helpers ---------------------------------------------------------------

    private static Set<String> useCasesRealizing(String requirementId) {
        return useCases.values().stream()
                .filter(useCase -> useCase.requirements().contains(requirementId))
                .map(UseCase::id)
                .collect(toCollection(LinkedHashSet::new));
    }

    private static Set<String> idsIn(Pattern pattern, String text) {
        Set<String> found = new LinkedHashSet<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            found.add(matcher.group());
        }
        return found;
    }

}
