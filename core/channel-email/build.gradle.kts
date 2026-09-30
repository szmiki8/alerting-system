// Plugin module: depends only on the extension interfaces and Spring Boot (architecture Section 6.2, OP-18).
plugins {
    id("alerting.java-library-conventions")
}

dependencies {
    implementation(project(":alerting-spi"))
    // Registered by auto-configuration, off unless alerting.channels.email.enabled (plugin module convention).
    implementation(libs.spring.boot.autoconfigure)

    // ApplicationContextRunner for the auto-configuration test.
    testImplementation(libs.spring.boot.starter.test)
}
