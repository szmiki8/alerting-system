package com.sonrisa.alerting.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** NFR-03: committed configuration never holds a secret value. */
class NoSecretsInRepositoryTest {

    private static final Path CORE_DIR = Path.of(System.getProperty("alerting.core-dir", ".."))
            .toAbsolutePath().normalize();

    @Test
    void committedConfigurationHasNoLiteralSecrets() {
        List<Path> files = SecretScanner.configurationFiles(CORE_DIR);

        assertThat(files)
                .as("scanned files")
                .anyMatch(file -> file.endsWith("alerting-app/src/main/resources/application.yaml"))
                .anyMatch(file -> file.endsWith(".env.example"));
        assertThat(files.stream().flatMap(file -> SecretScanner.scan(file).stream()).toList()).isEmpty();
    }

    @Test
    void scannerFlagsLiteralSecretsAndAcceptsPlaceholders() {
        Path file = Path.of("sample.yaml");
        List<String> lines = List.of(
                "spring:",
                "  datasource:",
                "    password: hunter2",                                  // flagged
                "alerting.sources.newsapi.api-key=abc123",                // flagged
                "ALERTING_MANAGEMENT_OPERATOR_PASSWORD=changeit",          // flagged
                "    client-secret: \"s3cr3t\" # comment",                // flagged
                "    password: ${DB_PASSWORD}",                           // placeholder
                "    token: ${SLACK_TOKEN:}",                             // placeholder, empty default
                "ALERTING_SECURITY_ENCRYPTION_KEY=",                      // empty
                "    # password: in a comment is ignored",
                "    operator-username: operator",                        // not a secret key
                "    api-key: ${NEWSAPI_KEY:default-key}");               // flagged: literal default

        assertThat(SecretScanner.scan(file, lines))
                .extracting(SecretScanner.Finding::line)
                .containsExactly(3, 4, 5, 6, 12);
        assertThat(SecretScanner.scan(file, lines).toString()).doesNotContain("hunter2");
    }
}
