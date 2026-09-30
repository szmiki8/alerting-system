package com.sonrisa.alerting.app.librarycheck;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Testcontainers 2 starts PostgreSQL. Skipped (not failed) on machines without Docker. */
@Testcontainers(disabledWithoutDocker = true)
class PostgresTestcontainersSmokeTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Test
    void connectsToPostgres() throws Exception {
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var result = connection.createStatement().executeQuery("select version()")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString(1)).startsWith("PostgreSQL 17");
        }
    }
}
