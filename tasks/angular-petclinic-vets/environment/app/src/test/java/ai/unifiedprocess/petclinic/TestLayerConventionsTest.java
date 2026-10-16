package ai.unifiedprocess.petclinic;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Executable version of the naming rule in docs/architecture/testing.md.
 *
 * <p>{@link ArchitectureTest} deliberately excludes the test tree, so the one
 * convention that governs the test tree itself has to live in its own class.
 * A misnamed test is the quietest failure this project has: nothing errors,
 * the wrong Maven plugin simply never picks the class up and it reports as
 * passing by never running at all. That is worth a rule rather than a
 * paragraph.
 *
 * <p>Only the two dangerous directions are asserted. Plenty of {@code *Test}
 * classes are neither use-case tests nor journeys — the sensors, this class —
 * so "every {@code *Test} extends something" would be false.
 */
@ArchTag("sensor")
@AnalyzeClasses(packages = "ai.unifiedprocess.petclinic")
class TestLayerConventionsTest {

    @ArchTest
    static final ArchRule useCaseTestsAreNotNamedIT =
            noClasses()
                    .that().haveSimpleNameStartingWith("UC")
                    .should().haveSimpleNameEndingWith("IT")
                    .because("testing.md: Surefire runs *Test — a use-case test named *IT "
                            + "is skipped by mvn test and runs in the wrong phase");

    @ArchTest
    static final ArchRule journeysAreNamedIT =
            classes()
                    .that().areAnnotatedWith(TestCase.class)
                    .should().haveSimpleNameEndingWith("IT")
                    .because("testing.md: Failsafe runs *IT — a journey named *Test "
                            + "runs a whole end-to-end path inside the use-case test phase");
}
