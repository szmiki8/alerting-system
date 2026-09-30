plugins {
    id("alerting.spring-boot-application-conventions")
}

dependencies {
    implementation(project(":alerting-spi"))
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.spring.boot.starter.restclient)
    implementation(libs.resilience4j.spring.boot4)
    runtimeOnly(libs.micrometer.registry.prometheus)
    annotationProcessor(libs.spring.boot.configuration.processor)

    // Persistence (BE-06, ADR-04): JPA with Flyway-owned schema; H2 in-memory by default, PostgreSQL in the postgres profile.
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.flyway)
    runtimeOnly(libs.flyway.database.postgresql)
    runtimeOnly(libs.h2)
    runtimeOnly(libs.postgresql)

    // Plugins are runtime-only: the application never compiles against a concrete plugin (ADR-01, NFR-14).
    runtimeOnly(project(":source-newsapi"))
    runtimeOnly(project(":source-stub"))
    runtimeOnly(project(":channel-email"))
    runtimeOnly(project(":channel-slack"))
    runtimeOnly(project(":channel-log"))

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.archunit.junit6)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.wiremock.standalone)
    // Persistence tests (BE-06): data JPA slice and Testcontainers PostgreSQL.
    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
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
