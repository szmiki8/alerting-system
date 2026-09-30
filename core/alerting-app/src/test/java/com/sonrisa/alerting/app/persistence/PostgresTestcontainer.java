package com.sonrisa.alerting.app.persistence;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Reusable PostgreSQL for integration tests (BE-06). Use it with
 * {@code @ImportTestcontainers(PostgresTestcontainer.class)}: the {@link ServiceConnection} replaces the
 * datasource of the test context, so the application runs against PostgreSQL with its normal configuration.
 *
 * <p>One container per test JVM, started on first use and removed by Testcontainers when the JVM ends. Test
 * contexts share it, so tests must not rely on an empty database. These tests need Docker; without it they fail
 * rather than silently skip the PostgreSQL path.
 */
public interface PostgresTestcontainer {

    /** Same image as the library smoke test. */
    String IMAGE = "postgres:17-alpine";

    @ServiceConnection
    PostgreSQLContainer POSTGRES = start();

    /**
     * Command-line arguments that point an application started with {@code SpringApplication.run} (no test
     * context, so no service connection) at the shared container.
     */
    static String[] datasourceArguments() {
        return new String[] {
            "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
            "--spring.datasource.username=" + POSTGRES.getUsername(),
            "--spring.datasource.password=" + POSTGRES.getPassword()};
    }

    private static PostgreSQLContainer start() {
        PostgreSQLContainer container = new PostgreSQLContainer(IMAGE);
        container.start();
        return container;
    }
}
