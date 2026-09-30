plugins {
    // Version comes from the main build's settings, which already put the plugin on the classpath.
    id("org.gradle.toolchains.foojay-resolver-convention")
}

rootProject.name = "buildSrc"

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    // Share the main build's version catalog with the convention plugins.
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
