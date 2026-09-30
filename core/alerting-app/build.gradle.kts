plugins {
    id("alerting.spring-boot-application-conventions")
}

dependencies {
    implementation(project(":alerting-spi"))
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.security)
    runtimeOnly(libs.micrometer.registry.prometheus)
    annotationProcessor(libs.spring.boot.configuration.processor)

    // Plugins are runtime-only: the application never compiles against a concrete plugin (ADR-01, NFR-14).
    runtimeOnly(project(":source-newsapi"))
    runtimeOnly(project(":source-stub"))
    runtimeOnly(project(":channel-email"))
    runtimeOnly(project(":channel-slack"))
    runtimeOnly(project(":channel-log"))

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.archunit.junit6)
    // Only for the architecture test fixtures (an @Entity to check the controller rule); JPA itself arrives with BE-06.
    testImplementation(libs.jakarta.persistence.api)
}

tasks.test {
    // Lets the secret scan test find the other modules and .env.example.
    systemProperty("alerting.core-dir", rootDir.absolutePath)
}

// BE-02: smoke tests for third-party libraries that later tasks adopt. The suite has its own
// classpath, so these libraries do not leak into the application or its regular tests.
testing {
    suites {
        register<JvmTestSuite>("libraryTest") {
            dependencies {
                implementation(platform(libs.spring.boot.dependencies))
                implementation(platform(libs.awssdk.bom))
                implementation(libs.spring.boot.starter.webmvc)
                implementation(libs.spring.boot.starter.jdbc)
                implementation(libs.spring.boot.starter.mail)
                implementation(libs.spring.boot.starter.test)
                implementation(libs.springdoc.openapi.starter.webmvc.ui)
                implementation(libs.resilience4j.spring.boot4)
                implementation(libs.shedlock.spring)
                implementation(libs.shedlock.provider.jdbc.template)
                implementation(libs.awssdk.sesv2)
                implementation(libs.testcontainers.junit.jupiter)
                implementation(libs.testcontainers.postgresql)
                implementation(libs.wiremock.standalone)
                implementation(libs.greenmail.junit5)
                runtimeOnly(libs.h2)
                runtimeOnly(libs.postgresql)
            }
        }
    }
}

tasks.named("check") {
    dependsOn(testing.suites.named("libraryTest"))
}
