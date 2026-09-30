// Shared Java setup for every core module: Java 17 toolchain, compiler flags,
// Spring Boot BOM for dependency versions, JUnit Platform for tests.
plugins {
    java
}

group = "com.sonrisa"
version = "0.1.0-SNAPSHOT"

val libs = the<VersionCatalogsExtension>().named("libs")
fun lib(alias: String) = libs.findLibrary(alias).get()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    // Spring Boot manages the versions of everything it covers, also in plain library modules.
    val bom = platform(lib("spring-boot-dependencies"))
    implementation(bom)
    annotationProcessor(bom)

    testImplementation(lib("junit-jupiter"))
    testImplementation(lib("assertj-core"))
    testRuntimeOnly(lib("junit-platform-launcher"))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 17
    // -parameters: Spring needs parameter names (binding, constructor injection).
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all,-processing,-serial"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
