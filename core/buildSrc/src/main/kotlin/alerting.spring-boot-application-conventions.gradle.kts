// Conventions for the Spring Boot application module.
plugins {
    id("alerting.java-conventions")
    id("org.springframework.boot")
}

springBoot {
    // Adds build information (group, artifact, version, time) to /actuator/info.
    buildInfo()
}
