package com.sonrisa.alerting.app.persistence.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonrisa.alerting.app.persistence.PostgresTestcontainer;
import com.sonrisa.alerting.app.persistence.RepositoryTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.jdbc.core.JdbcTemplate;

/** AUDIT_ENTRY table and repository (BE-07), on H2 and on PostgreSQL. */
class AuditEntryRepositoryTest {

    @RepositoryTest
    abstract static class Checks {

        @Autowired
        AuditEntryRepository repository;

        @Autowired
        TestEntityManager entityManager;

        @Autowired
        JdbcTemplate jdbcTemplate;

        @Test
        void storesAdminActionTypeAndTime() {
            Instant at = Instant.parse("2026-09-01T10:15:30.5Z");
            AuditEntry saved = repository.saveAndFlush(new AuditEntry(UUID.randomUUID(), "admin@example.org",
                    AuditAction.SUBSCRIBER_DELETED, "slack", at));
            entityManager.clear();

            AuditEntry loaded = repository.findById(saved.getId()).orElseThrow();

            assertThat(loaded.getAdminEmail()).isEqualTo("admin@example.org");
            assertThat(loaded.getAction()).isEqualTo(AuditAction.SUBSCRIBER_DELETED);
            assertThat(loaded.getSubscriberType()).isEqualTo("slack");
            assertThat(loaded.getPerformedAt()).isEqualTo(at);
        }

        @Test
        void hasNoColumnsForSubscriberData() {
            // Guard for FR-28/FR-30: the table cannot hold the deleted subscriber's name or address.
            var columns = jdbcTemplate.queryForList(
                    "select lower(column_name) from information_schema.columns where lower(table_name) = 'audit_entry'",
                    String.class);

            assertThat(columns).containsExactlyInAnyOrder("id", "admin_email", "action", "subscriber_type",
                    "performed_at");
        }
    }

    @Nested
    class OnH2 extends Checks {
    }

    @Nested
    @ImportTestcontainers(PostgresTestcontainer.class)
    class OnPostgres extends Checks {
    }
}
