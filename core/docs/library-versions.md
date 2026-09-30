# Library versions (BE-02 spike)

Checked on 2026-09-30 against Spring Boot 4.1.1 and a Java 17 toolchain (Eclipse Temurin / Amazon Corretto 17).
All versions are pinned in `gradle/libs.versions.toml`. Libraries that Spring Boot manages have no version
in the catalog; the Boot BOM (`spring-boot-dependencies`) decides.

How each library was verified:

- **Latest version**: `maven-metadata.xml` on Maven Central (repo1.maven.org), read on the check date.
- **Java 17**: the class file major version of the library jar is 61 (Java 17) or lower, and the smoke test runs on the Java 17 toolchain.
- **Smoke test**: the `libraryTest` test suite in `alerting-app` (`src/libraryTest`). It has its own classpath, so these libraries do not reach the application before the task that adopts them. `./gradlew build` runs it (it is wired into `check`).

## Build platform

| Item | Version | Notes and source |
|---|---|---|
| Gradle (wrapper) | 9.8.0 | Current release (services.gradle.org/versions/current). Distribution checksum pinned in `gradle-wrapper.properties` (from services.gradle.org/distributions/gradle-9.8.0-bin.zip.sha256). |
| Foojay toolchain resolver | 1.0.0 | Latest on plugins.gradle.org. Downloads a JDK 17 when none is installed; applied in `settings.gradle.kts` and `buildSrc/settings.gradle.kts`. |
| Spring Boot | 4.1.1 | Latest 4.1.x patch on Maven Central (4.2.0 is at milestone 2). ADR-15. Brings Spring Framework 7.0.9, Spring Security 7.1.1, Jetty 12.1.12, JUnit Jupiter 6.0.3, Testcontainers 2.0.5, Micrometer 1.17.1 (from the 4.1.1 BOM). |

## Libraries named in BE-02

| Library | Version | Java 17 | Smoke test | Notes and source |
|---|---|---|---|---|
| springdoc-openapi (`springdoc-openapi-starter-webmvc-ui`) | 3.1.1 | Yes, class files are Java 17 (major 61) | `SpringdocSmokeTest` | "springdoc-openapi 3.x is compatible with spring-boot 4" and 3.1.1 is named the latest stable (springdoc.org/faq.html). The FAQ gives no Java baseline; the jar check and the smoke test on Java 17 confirm it. **Swagger UI toggle:** `springdoc.swagger-ui.enabled=false` removes the UI and keeps `/v3/api-docs` (FAQ; checked by the test). |
| Resilience4j (`resilience4j-spring-boot4`) | 2.4.0 | Yes (major 61) | `Resilience4jSmokeTest` | 2.4.0 is the latest release and adds Spring Boot 4 support (github.com/resilience4j/resilience4j/releases). **Confirmed:** the Resilience4j 2.4.0 BOM lists only `resilience4j-spring-boot3`, not `-boot4` (resilience4j-bom-2.4.0.pom on Maven Central), so the catalog pins the exact version and no BOM is used. The module is compiled against Spring Boot 4.0.0; registries bind from properties under 4.1.1 (checked by the test). |
| ShedLock (`shedlock-spring`, `shedlock-provider-jdbc-template`) | 7.10.1 | Yes (major 61; parent POM `jdk.version` 17) | `ShedLockSmokeTest` (H2) | ShedLock 7.x: "Minimal JVM version 17", "Tested with Spring 7.0, 6.2, Spring Boot 4.x" (github.com/lukas-krecan/ShedLock README). The 7.10.1 build uses Spring 7.0.9 and Spring Boot 4.1.1 (shedlock-parent-7.10.1.pom). |
| AWS SDK for Java v2 (`software.amazon.awssdk:bom`, `sesv2`) | 2.55.8 | Yes (Java 8 baseline) | `SesV2WireMockSmokeTest` | Latest release on Maven Central (released 2026-09-29; the SDK publishes almost daily, so update when BE-37 starts). The test sends an email through the SESv2 client to a WireMock stub. |
| ArchUnit (`archunit-junit6`) | 1.5.1 | Yes (major 52) | BE-03 architecture tests | Spring Boot 4.1 manages JUnit 6, so the JUnit 6 artifact is used (`archunit-junit6` exists since 1.5.0; Maven Central). |
| Testcontainers (`testcontainers-postgresql`, `testcontainers-junit-jupiter`) | 2.0.5 (Boot-managed) | Yes | `PostgresTestcontainersSmokeTest` (image `postgres:17-alpine`) | Managed by the Spring Boot 4.1.1 BOM, so no version in the catalog. Testcontainers 2 renamed the modules (`testcontainers-postgresql`) and moved `PostgreSQLContainer` to `org.testcontainers.postgresql`. The test is skipped, not failed, when Docker is missing. |
| WireMock (`wiremock-standalone`) | 3.13.2 | Yes (major 55) | `SesV2WireMockSmokeTest` | 3.13.2 is the latest stable; 4.0.0 is still beta (4.0.0-beta.39). See the incompatibility below. |
| GreenMail (`greenmail-junit5`) | 2.1.14 | Yes (major 52) | `GreenMailSmokeTest` | Latest 2.1.x on Maven Central. Receives mail sent through Spring's `JavaMailSenderImpl` with the Boot-managed Jakarta Mail 2.1.5 / Angus Mail 2.0.5. Its JUnit 5 extension works on JUnit 6. Chosen over the Mailpit Testcontainers image for tests because it needs no Docker; Mailpit stays the local mail sink in Docker Compose (BE-44). |

## Other checks from BE-02

- **Spring Security 7 SPA CSRF (for BE-13):** `http.csrf(csrf -> csrf.spa())` exists since Spring Security 7.0. It sets up a cookie-based token repository and a request handler that resolves the raw token value, which fits Angular's `XSRF-TOKEN` cookie / `X-XSRF-TOKEN` header pattern (Spring Security 7.0 reference, `CsrfConfigurer#spa` and "What's New", via context7 `/websites/spring_io_spring-security_reference_7_0`). Spring Boot 4.1.1 manages Spring Security 7.1.1.
- **Actuator on Spring Boot 4 (for BE-05):** `EndpointRequest` moved to `org.springframework.boot.security.autoconfigure.actuate.web.servlet` (module `spring-boot-security`); `/actuator/prometheus` needs `micrometer-registry-prometheus` (Spring Boot 4.1 reference, via context7 `/spring-projects/spring-boot/v4.1.0`).

## Incompatibility found

| Library | Problem | Resolution |
|---|---|---|
| `org.wiremock:wiremock-jetty12` 3.13.2 | WireMock 3.13.x is built for Jetty 12.0 (it asks for `jetty-server` 12.0.30). Spring Boot 4.1.1's BOM raises Jetty to 12.1.12, and WireMock then fails at start-up: `NoSuchMethodError: 'org.eclipse.jetty.util.component.Environment org.eclipse.jetty.util.component.Environment.ensure(java.lang.String)'` (reproduced on 2026-09-30 with the smoke test). | Use `org.wiremock:wiremock-standalone` 3.13.2, which shades its own Jetty and does not see Boot's version. Same API (`WireMockExtension`). The user did not object in an earlier review. Alternatives, if the shaded jar is ever a problem: force Jetty 12.0.x only for the test classpath, or move to WireMock 4 once it is stable. |

No other incompatibility was found.
