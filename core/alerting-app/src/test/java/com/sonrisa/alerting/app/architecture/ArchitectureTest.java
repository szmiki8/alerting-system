package com.sonrisa.alerting.app.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Checks the production classes of all core modules. The plugin modules are on the test runtime
 * classpath because they are runtime dependencies of the application.
 */
@AnalyzeClasses(packages = ArchitectureRules.BASE, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule pluginsDoNotDependOnApp = ArchitectureRules.PLUGINS_DO_NOT_DEPEND_ON_APP;

    @ArchTest
    static final ArchRule appDoesNotDependOnPlugins = ArchitectureRules.APP_DOES_NOT_DEPEND_ON_PLUGINS;

    @ArchTest
    static final ArchRule spiHasNoFrameworkDependencies = ArchitectureRules.SPI_HAS_NO_FRAMEWORK_DEPENDENCIES;

    @ArchTest
    static final ArchRule appClassesLiveInFeaturePackages = ArchitectureRules.APP_CLASSES_LIVE_IN_FEATURE_PACKAGES;

    @ArchTest
    static final ArchRule featurePackagesAreFreeOfCycles = ArchitectureRules.FEATURE_PACKAGES_ARE_FREE_OF_CYCLES;

    @ArchTest
    static final ArchRule controllersDoNotExposeEntities = ArchitectureRules.CONTROLLERS_DO_NOT_EXPOSE_ENTITIES;
}
