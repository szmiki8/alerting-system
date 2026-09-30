package com.sonrisa.alerting.app.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Runs all Flyway migrations on an empty H2 database (PostgreSQL mode, as configured in application.yaml) and on
 * an empty PostgreSQL database (Testcontainers), then validates them (BE-06). Every new migration is covered
 * automatically.
 */
class MigrationTest {

    /** The tables every migration run must produce. Later tasks add their tables here. */
    static final List<String> EXPECTED_TABLES = List.of(
            "shedlock", "spring_session", "spring_session_attributes",
            "subscriber", "audit_entry",
            "run", "run_source_result", "source_state", "event", "notification");

    /** Indexes for the admin list, delivery and retention queries (Section 8.1, 8.4). */
    static final List<String> EXPECTED_INDEXES = List.of(
            "ix_subscriber_subscribed_at", "ix_subscriber_display_name", "ix_subscriber_address",
            "ix_subscriber_status_type",
            "ix_audit_entry_performed_at",
            "ix_run_started_at", "ix_event_collected_at", "ix_event_run_id",
            "ix_notification_claim", "ix_notification_lease", "ix_notification_subscriber",
            "ix_notification_run_id", "ix_notification_created_at");

    /** Same locations as {@code spring.flyway.locations}, with the vendor folder resolved. */
    static Flyway flyway(DriverManagerDataSource dataSource, String vendor) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/common", "classpath:db/migration/" + vendor)
                .load();
    }

    static void migrateAndCheck(DriverManagerDataSource dataSource, String vendor) throws SQLException {
        Flyway flyway = flyway(dataSource, vendor);

        assertThat(flyway.migrate().success).isTrue();

        flyway.validate();
        MigrationInfo[] all = flyway.info().all();
        assertThat(all).isNotEmpty().allSatisfy(info -> assertThat(info.getState()).isEqualTo(MigrationState.SUCCESS));
        try (Connection connection = dataSource.getConnection()) {
            assertThat(tables(connection)).containsAll(EXPECTED_TABLES);
            assertThat(indexes(connection)).containsAll(EXPECTED_INDEXES);
        }
    }

    static List<String> tables(Connection connection) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (var result = connection.getMetaData().getTables(null, null, "%", new String[] {"TABLE"})) {
            while (result.next()) {
                tables.add(result.getString("TABLE_NAME").toLowerCase(Locale.ROOT));
            }
        }
        return tables;
    }

    static List<String> indexes(Connection connection) throws SQLException {
        List<String> indexes = new ArrayList<>();
        for (String table : EXPECTED_TABLES) {
            try (var result = connection.getMetaData().getIndexInfo(null, null, table, false, false)) {
                while (result.next()) {
                    String name = result.getString("INDEX_NAME");
                    if (name != null) {
                        indexes.add(name.toLowerCase(Locale.ROOT));
                    }
                }
            }
        }
        return indexes;
    }

    @Nested
    class OnH2 {

        @Test
        void allMigrationsApply() throws SQLException {
            var dataSource = new DriverManagerDataSource("jdbc:h2:mem:migration-" + UUID.randomUUID()
                    + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1", "sa", "");

            migrateAndCheck(dataSource, "h2");
        }
    }

    @Nested
    class OnPostgres {

        @Test
        void allMigrationsApply() throws SQLException {
            // A new, empty database in the shared container; other tests may already have migrated the default one.
            var container = PostgresTestcontainer.POSTGRES;
            String database = "migration_" + UUID.randomUUID().toString().replace("-", "");
            try (Connection admin = DriverManager.getConnection(
                    container.getJdbcUrl(), container.getUsername(), container.getPassword())) {
                admin.createStatement().execute("CREATE DATABASE " + database);
            }
            String url = container.getJdbcUrl().replace("/" + container.getDatabaseName(), "/" + database);
            var dataSource = new DriverManagerDataSource(url, container.getUsername(), container.getPassword());

            migrateAndCheck(dataSource, "postgresql");
        }
    }
}
