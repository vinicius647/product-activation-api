package br.com.vdb;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Checks the hexagonal architecture rules described in the README.
 */
@AnalyzeClasses(packages = "br.com.vdb", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String CORE = "br.com.vdb.core..";
    private static final String DOMAIN = "br.com.vdb.core.domain..";
    private static final String PORT_IN = "br.com.vdb.core.port.in..";
    private static final String PORT_OUT = "br.com.vdb.core.port.out..";
    private static final String SERVICE = "br.com.vdb.core.service..";

    @ArchTest
    static final ArchRule coreDependsOnlyOnJava = classes()
            .that().resideInAPackage(CORE)
            .should().onlyDependOnClassesThat().resideInAnyPackage(CORE, "java..")
            .because("the core must not know Spring, JPA, HTTP or any other framework");

    @ArchTest
    static final ArchRule domainDoesNotDependOnPortsOrServices = classes()
            .that().resideInAPackage(DOMAIN)
            .should().onlyDependOnClassesThat().resideInAnyPackage(DOMAIN, "java..")
            .because("the domain is the center of the hexagon and does not know how it is used");

    @ArchTest
    static final ArchRule portsDoNotDependOnServices = noClasses()
            .that().resideInAnyPackage(PORT_IN, PORT_OUT)
            .should().dependOnClassesThat().resideInAPackage(SERVICE)
            .because("ports are contracts; services implement them");

    @ArchTest
    static final ArchRule inboundAdaptersUseOnlyInboundPorts = noClasses()
            .that().resideInAnyPackage("br.com.vdb.controller..", "br.com.vdb.scheduler..")
            .should().dependOnClassesThat().resideInAnyPackage(PORT_OUT, SERVICE)
            .because("controllers and schedulers call the use cases (port.in), not their implementation");

    @ArchTest
    static final ArchRule adaptersImplementOutboundPorts = classes()
            .that().resideInAPackage("br.com.vdb.adapter")
            .and().areTopLevelClasses()
            .should().beAssignableTo(resideInAPackage(PORT_OUT))
            .because("the adapter package only holds implementations of port.out");

    @ArchTest
    static final ArchRule layers = layeredArchitecture()
            .consideringAllDependencies()
            .layer("Controller").definedBy("br.com.vdb.controller..")
            .layer("Scheduler").definedBy("br.com.vdb.scheduler..")
            .layer("Adapter").definedBy("br.com.vdb.adapter..")
            .layer("Repository").definedBy("br.com.vdb.repository..")
            .layer("Entity").definedBy("br.com.vdb.entity..")
            .layer("Config").definedBy("br.com.vdb.config..")
            .layer("Service").definedBy(SERVICE)

            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Scheduler").mayNotBeAccessedByAnyLayer()
            .whereLayer("Adapter").mayNotBeAccessedByAnyLayer()
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Adapter", "Config")
            .whereLayer("Entity").mayOnlyBeAccessedByLayers("Adapter", "Repository")
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Config");
}
