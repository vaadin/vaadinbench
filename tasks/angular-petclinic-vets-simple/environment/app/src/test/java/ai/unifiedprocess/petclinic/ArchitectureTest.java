package ai.unifiedprocess.petclinic;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTag;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Entity;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Path;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Executable version of docs/architecture/development.md.
 * Every rule references the section it enforces.
 */
@ArchTag("sensor")
@AnalyzeClasses(
        packages = "ai.unifiedprocess.petclinic",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String APP_PACKAGE = "ai.unifiedprocess.petclinic";

    // --- Module layout ---

    @ArchTest
    static final ArchRule featuresHaveOnlyApiAndDomain =
            classes()
                    .that().resideInAPackage("ai.unifiedprocess.petclinic.(*)..")
                    .and().resideOutsideOfPackage("..core..")
                    .should().resideInAnyPackage("..api..", "..domain..")
                    .because("development.md: each feature has exactly the sub-packages api and domain");

    @ArchTest
    static final ArchRule noServiceOrDtoLayer =
            noClasses()
                    .should().haveSimpleNameEndingWith("Service")
                    .orShould().haveSimpleNameEndingWith("Dto")
                    .orShould().haveSimpleNameEndingWith("DTO")
                    .because("development.md: no separate service/DTO layering beyond api + domain");

    // --- Cross-feature rule ---

    @ArchTest
    static final ArchRule domainDoesNotDependOnApi =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..api..")
                    .because("development.md: domain is the boundary between features");

    @ArchTest
    static final ArchRule domainIsFreeOfRest =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "jakarta.ws.rs..", "org.jboss.resteasy..", "io.quarkus.resteasy..")
                    .because("development.md: domain holds entities and repositories only");

    // Sliced on domain, not on the whole feature: a feature's api may read another
    // feature's domain (an owner's page lists the owner's pets), which the domain
    // packages themselves must never turn into a cycle.
    @ArchTest
    static final ArchRule noCyclesBetweenFeatures =
            slices()
                    .matching("ai.unifiedprocess.petclinic.(*).domain..")
                    .should().beFreeOfCycles()
                    .because("development.md: domain is the boundary between features");

    // Between features, api never reaches api: a resource answers for its own feature,
    // and what it needs from another feature it takes from that feature's domain.
    // core is the shell and is reachable from everywhere.
    @ArchTest
    static final ArchRule foreignApiIsNeverUsed =
            classes()
                    .that().resideInAPackage(APP_PACKAGE + ".(*).api..")
                    .should(notDependOnAnotherFeaturesApi())
                    .because("development.md: cross-feature reach-in goes through domain");

    // --- Data access (JPA) ---

    @ArchTest
    static final ArchRule noSpring =
            noClasses()
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                    .because("development.md: Quarkus and Panache, no Spring Data");

    @ArchTest
    static final ArchRule entitiesLiveInDomain =
            classes()
                    .that().areAnnotatedWith(Entity.class)
                    .should().resideInAPackage("..domain..")
                    .because("development.md: an entity maps a table of its feature's domain");

    // The repository pattern, not active record: an entity that extends
    // PanacheEntityBase carries its own queries, and every view of the data would
    // then be one static call away from any class in the application.
    @ArchTest
    static final ArchRule entitiesAreNotActiveRecords =
            noClasses()
                    .should().beAssignableTo(PanacheEntityBase.class)
                    .because("development.md: queries live in the feature's repository");

    @ArchTest
    static final ArchRule domainIsValidationFree =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("jakarta.validation..")
                    .because("development.md: the Angular form is the validation boundary");

    // A resource answers with records shaped for the page, never with entities: an
    // entity serialized by Jackson walks its lazy associations outside any transaction,
    // and every column of the table becomes part of the API by accident.
    @ArchTest
    static final ArchRule resourcesNeverReturnEntities =
            methods()
                    .that().areDeclaredInClassesThat().areAnnotatedWith(Path.class)
                    .and().arePublic()
                    .should(notReturnEntities())
                    .because("development.md: the API returns records, never entities");

    // --- Persistence stereotype ---

    @ArchTest
    static final ArchRule repositoriesAreNamedAndScoped =
            classes()
                    .that().haveSimpleNameEndingWith("Repository")
                    .should().beAssignableTo(PanacheRepositoryBase.class)
                    .andShould().beAnnotatedWith(ApplicationScoped.class)
                    .andShould().resideInAPackage("..domain..")
                    .because("development.md: a repository is an @ApplicationScoped Panache repository");

    @ArchTest
    static final ArchRule repositoriesAreTransactional =
            classes()
                    .that().haveSimpleNameEndingWith("Repository")
                    .should().beAnnotatedWith(Transactional.class)
                    .because("development.md: the repository is the transaction boundary");

    // The transaction boundary is the repository, never a resource: what a resource
    // returns must already be loaded, so nothing lazy is left for Jackson to trip over.
    @ArchTest
    static final ArchRule resourcesAreNotTransactional =
            noClasses()
                    .that().resideInAPackage("..api..")
                    .should().dependOnClassesThat().haveFullyQualifiedName(Transactional.class.getName())
                    .because("development.md: transactions are declared in domain, not in api");

    @ArchTest
    static final ArchRule panacheRepositoriesAreRepositories =
            classes()
                    .that().areAssignableTo(PanacheRepositoryBase.class)
                    .and().areNotInterfaces()
                    .should().haveSimpleNameEndingWith("Repository")
                    .because("development.md: not *Queries, *Dao, *Store");

    // --- REST resource conventions ---

    @ArchTest
    static final ArchRule resourcesLiveInApi =
            classes()
                    .that().areAnnotatedWith(Path.class)
                    .and().resideOutsideOfPackage("..core..")
                    .should().resideInAPackage("..api..")
                    .andShould().haveSimpleNameEndingWith("Resource")
                    .because("development.md: one resource per feature, in the feature's api package");

    // Everything under /api is the backend; everything else is the Angular
    // application, which SpaRouting answers with index.html. A resource outside
    // /api would shadow a page of the frontend.
    @ArchTest
    static final ArchRule resourcesAreUnderApi =
            classes()
                    .that().areAnnotatedWith(Path.class)
                    .should(declarePathUnder("/api/"))
                    .because("development.md: every resource path starts with /api/");

    // --- Conditions ---

    private static ArchCondition<JavaClass> notDependOnAnotherFeaturesApi() {
        return new ArchCondition<>("not depend on another feature's api") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    boolean foreignApi = resideInAPackage(APP_PACKAGE + "..api..").test(target)
                            && !feature(target).equals(feature(origin))
                            && !feature(target).equals("core");
                    if (foreignApi) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    /** {@code ai.unifiedprocess.petclinic.owner.api.OwnerResource} → {@code owner}. */
    private static String feature(JavaClass javaClass) {
        String packageName = javaClass.getPackageName();
        if (!packageName.startsWith(APP_PACKAGE + ".")) {
            return "";
        }
        String rest = packageName.substring(APP_PACKAGE.length() + 1);
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }

    private static ArchCondition<JavaMethod> notReturnEntities() {
        return new ArchCondition<>("not return entities") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                for (JavaClass type : method.getReturnType().getAllInvolvedRawTypes()) {
                    if (type.isAnnotatedWith(Entity.class)) {
                        events.add(SimpleConditionEvent.violated(method, method.getFullName()
                                + " returns the entity " + type.getSimpleName()));
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaClass> declarePathUnder(String prefix) {
        return new ArchCondition<>("declare @Path under " + prefix) {
            @Override
            public void check(JavaClass resource, ConditionEvents events) {
                JavaAnnotation<JavaClass> path = resource.getAnnotationOfType(Path.class.getName());
                String value = String.valueOf(path.get("value").orElse(""));
                boolean under = (value.startsWith("/") ? value : "/" + value).startsWith(prefix);
                events.add(new SimpleConditionEvent(resource, under, resource.getSimpleName()
                        + " is at " + value));
            }
        };
    }
}
