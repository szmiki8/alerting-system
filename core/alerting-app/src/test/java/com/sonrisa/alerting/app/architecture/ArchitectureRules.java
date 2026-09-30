package com.sonrisa.alerting.app.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;

/**
 * Module and package rules of architecture Section 6.2 (BE-03). The Gradle configuration is the first
 * guard (plugins are runtime-only dependencies of the application); these rules are the second.
 *
 * <p>Modules are told apart by package: {@code ..alerting.spi}, {@code ..alerting.app},
 * {@code ..alerting.source.<key>} and {@code ..alerting.channel.<key>} for the plugins.
 */
final class ArchitectureRules {

    static final String BASE = "com.sonrisa.alerting";
    static final String SPI = BASE + ".spi..";
    static final String APP = BASE + ".app..";
    static final String[] PLUGINS = {BASE + ".source..", BASE + ".channel.."};

    /** Packages the SPI may use: the JDK and nullness/lifecycle annotations. No Spring (Boot) types. */
    static final String[] SPI_ALLOWED = {"java..", SPI, "org.jspecify.annotations..", "jakarta.annotation.."};

    /** Feature packages of the application (package by feature, Section 6.2). */
    static final String[] APP_FEATURES = {"subscription", "admin", "collection", "delivery", "run", "retention",
            "audit", "security", "persistence", "config",
            // Cross-cutting support packages: API conventions (BE-12), outbound HTTP clients and retries (BE-17).
            "api", "outbound", "resilience",
            // BE-11: plugin registries (Section 7); used by several features, so not part of any one of them.
            "plugin"};

    // Empty module packages are allowed: the plugin modules and the SPI get their first classes in later tasks.

    static final ArchRule PLUGINS_DO_NOT_DEPEND_ON_APP = noClasses()
            .that().resideInAnyPackage(PLUGINS)
            .should().dependOnClassesThat().resideInAPackage(APP)
            .because("plugins depend only on the extension interfaces in alerting-spi (NFR-14)")
            .allowEmptyShould(true);

    static final ArchRule APP_DOES_NOT_DEPEND_ON_PLUGINS = noClasses()
            .that().resideInAPackage(APP)
            .should().dependOnClassesThat().resideInAnyPackage(PLUGINS)
            .because("the application uses plugins only through the extension interfaces (ADR-01, ADR-10)");

    static final ArchRule SPI_HAS_NO_FRAMEWORK_DEPENDENCIES = classes()
            .that().resideInAPackage(SPI)
            .should().onlyDependOnClassesThat().resideInAnyPackage(SPI_ALLOWED)
            .because("the extension interfaces must not depend on Spring Boot or the application (Section 6.2)")
            .allowEmptyShould(true);

    static final ArchRule APP_CLASSES_LIVE_IN_FEATURE_PACKAGES = classes()
            .that().resideInAPackage(APP)
            .should().resideInAPackage(BASE + ".app")
            .orShould().resideInAnyPackage(featurePackages())
            .because("the application is organised by feature (Section 6.2)");

    static final ArchRule FEATURE_PACKAGES_ARE_FREE_OF_CYCLES = slices()
            .matching(BASE + ".app.(*)..")
            .should().beFreeOfCycles()
            .allowEmptyShould(true);

    static final ArchRule CONTROLLERS_DO_NOT_EXPOSE_ENTITIES = methods()
            .that().areDeclaredInClassesThat().areMetaAnnotatedWith(Controller.class)
            .should(notUseJpaEntitiesInSignature())
            .because("the REST API must not expose internal entities (Section 9.2)")
            .allowEmptyShould(true);

    /**
     * Outbound HTTP clients are built only through {@code OutboundHttpClients}, which requires timeouts
     * (BE-17, NFR-08). These factories create clients without the integration's timeouts. The rule covers
     * the plugin modules too: they cannot use {@code OutboundHttpClients} and instead build their client from
     * Spring Boot's {@code RestClient.Builder} with their own timeouts (OP-18).
     */
    static final ArchRule HTTP_CLIENTS_ARE_BUILT_WITH_TIMEOUTS = noClasses()
            .that().resideInAPackage(BASE + "..")
            .should().callCodeUnitWhere(clientFactoryWithoutTimeouts())
            .because("every outbound HTTP client needs explicit timeouts (NFR-08); use OutboundHttpClients,"
                    + " or in a plugin Spring Boot's RestClient.Builder with the plugin's timeouts (OP-18)")
            .allowEmptyShould(true);

    private ArchitectureRules() {
    }

    private static String[] featurePackages() {
        String[] packages = new String[APP_FEATURES.length];
        for (int i = 0; i < APP_FEATURES.length; i++) {
            packages[i] = BASE + ".app." + APP_FEATURES[i] + "..";
        }
        return packages;
    }

    private static DescribedPredicate<JavaCall<?>> clientFactoryWithoutTimeouts() {
        return new DescribedPredicate<>("an HTTP client factory without timeouts (RestClient.create/builder, "
                + "new RestTemplate, WebClient.create/builder, java.net.http.HttpClient.newHttpClient/newBuilder)") {
            @Override
            public boolean test(JavaCall<?> call) {
                String name = call.getName();
                return switch (call.getTargetOwner().getName()) {
                    case "org.springframework.web.client.RestClient",
                            "org.springframework.web.reactive.function.client.WebClient" ->
                            name.equals("create") || name.equals("builder");
                    case "org.springframework.web.client.RestTemplate" ->
                            name.equals(JavaConstructor.CONSTRUCTOR_NAME);
                    case "java.net.http.HttpClient" -> name.equals("newHttpClient") || name.equals("newBuilder");
                    default -> false;
                };
            }
        };
    }

    private static ArchCondition<JavaMethod> notUseJpaEntitiesInSignature() {
        return new ArchCondition<>("not use JPA entities as parameter or return types") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                List<JavaType> signatureTypes = new ArrayList<>(method.getParameterTypes());
                signatureTypes.add(method.getReturnType());
                for (JavaType type : signatureTypes) {
                    // All involved raw types also cover generics such as ResponseEntity<List<Entity>>.
                    for (JavaClass rawType : type.getAllInvolvedRawTypes()) {
                        if (rawType.isAnnotatedWith("jakarta.persistence.Entity")) {
                            events.add(SimpleConditionEvent.violated(method, String.format(
                                    "%s uses entity %s in its signature", method.getFullName(), rawType.getName())));
                        }
                    }
                }
            }
        };
    }
}
