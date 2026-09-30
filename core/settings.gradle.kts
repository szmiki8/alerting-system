plugins {
    // Resolves and downloads the Java 17 toolchain when no local JDK 17 is found.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "alerting-core"

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

include(
    "alerting-spi",
    "alerting-app",
    "source-newsapi",
    "source-stub",
    "channel-email",
    "channel-slack",
    "channel-log",
)
