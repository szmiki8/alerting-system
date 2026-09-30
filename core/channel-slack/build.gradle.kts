// Plugin module: depends only on the extension interfaces (architecture Section 6.2), plus Spring Boot for
// its auto-configuration and its own RestClient with timeouts (OP-18). Never on alerting-app.
plugins {
    id("alerting.java-library-conventions")
    // Test fixtures: a webhook client against a stand-in for Slack, for the application's integration tests
    // (BE-19). Only test code sees them; production code accepts the Slack host only.
    `java-test-fixtures`
}

dependencies {
    implementation(project(":alerting-spi"))
    // Auto-configuration, RestClient.Builder, ClientHttpRequestFactoryBuilder and HttpClientSettings (OP-18).
    implementation(libs.spring.boot.starter.restclient)
    // Validated @ConfigurationProperties (alerting.channels.slack.*).
    implementation(libs.spring.boot.starter.validation)
    annotationProcessor(libs.spring.boot.configuration.processor)

    testFixturesImplementation(platform(libs.spring.boot.dependencies))
    testFixturesImplementation(libs.spring.boot.starter.restclient)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.wiremock.standalone)
}
