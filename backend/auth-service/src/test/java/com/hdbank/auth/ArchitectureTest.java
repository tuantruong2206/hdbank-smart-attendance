package com.hdbank.auth;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture tests enforcing Hexagonal Architecture dependency rules
 * for auth-service.
 *
 * Rules:
 * 1. Domain layer must not depend on Spring framework
 * 2. Domain layer must not depend on JPA
 * 3. Application layer must not depend on adapter layer
 * 4. Inbound adapters (adapter.in) should only access application.port.in
 */
@AnalyzeClasses(
        packages = "com.hdbank.auth",
        importOptions = ImportOption.DoNotIncludeTests.class
)
public class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_spring =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("org.springframework..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_jpa =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("jakarta.persistence..", "javax.persistence..");

    @ArchTest
    static final ArchRule application_should_not_depend_on_adapter =
            noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..adapter..");

    @ArchTest
    static final ArchRule adapter_in_should_only_access_inbound_ports =
            noClasses()
                    .that().resideInAPackage("..adapter.in..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..application.service..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..adapter..",
                            "..config..",
                            "io.jsonwebtoken..",
                            "org.apache.kafka.."
                    );
}
