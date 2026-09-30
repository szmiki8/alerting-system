package com.sonrisa.alerting.app.persistence.event;

import static com.sonrisa.alerting.app.persistence.TestRecords.T0;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sonrisa.alerting.app.persistence.PostgresTestcontainer;
import com.sonrisa.alerting.app.persistence.RepositoryTest;
import com.sonrisa.alerting.app.persistence.TestRecords;
import com.sonrisa.alerting.app.persistence.run.Run;
import com.sonrisa.alerting.app.persistence.run.RunRepository;
import java.sql.Timestamp;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** EVENT table and repository (BE-08), on H2 and on PostgreSQL. */
class EventRepositoryTest {

    @RepositoryTest
    abstract static class Checks {

        @Autowired
        EventRepository events;

        @Autowired
        RunRepository runs;

        @Autowired
        TestEntityManager entityManager;

        @Autowired
        JdbcTemplate jdbcTemplate;

        Run run;

        @BeforeEach
        void createRun() {
            run = runs.saveAndFlush(TestRecords.run());
        }

        @Test
        void storesAllFieldsOfTheStandardFormat() {
            Event saved = events.saveAndFlush(TestRecords.event("newsapi:abc", run));
            entityManager.clear();

            Event loaded = events.findByEventKey("newsapi:abc").orElseThrow();

            assertThat(loaded.getId()).isEqualTo(saved.getId());
            assertThat(loaded.getSourceKey()).isEqualTo("newsapi");
            assertThat(loaded.getSourceName()).isEqualTo("BBC News");
            assertThat(loaded.getTitle()).isEqualTo("Title of newsapi:abc");
            assertThat(loaded.getContent()).isEqualTo("Content of newsapi:abc");
            assertThat(loaded.getLink()).isEqualTo("https://example.org/newsapi:abc");
            assertThat(loaded.getOccurredAt()).isEqualTo(T0.minusSeconds(600));
            assertThat(loaded.getCollectedAt()).isEqualTo(T0);
            assertThat(loaded.getRunId()).isEqualTo(run.getId());
            assertThat(events.existsByEventKey("newsapi:abc")).isTrue();
            assertThat(events.existsByEventKey("newsapi:other")).isFalse();
        }

        @Test
        void linkIsOptional() {
            Event withoutLink = new Event(UUID.randomUUID(), "stub:1", "stub", "Stub", "Title", "Content", null,
                    T0, T0, run.getId());

            events.saveAndFlush(withoutLink);
            entityManager.clear();

            assertThat(events.findByEventKey("stub:1").orElseThrow().getLink()).isNull();
        }

        @Test
        void eventKeyIsUnique() {
            events.saveAndFlush(TestRecords.event("newsapi:dup", run));

            assertThatThrownBy(() -> events.saveAndFlush(TestRecords.event("newsapi:dup", run)))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        void runMustExist() {
            Run unsaved = TestRecords.run();

            assertThatThrownBy(() -> events.saveAndFlush(TestRecords.event("newsapi:orphan", unsaved)))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"source_key", "source_name", "title", "content", "occurred_at"})
        void mandatoryFieldsAreNotNull(String column) {
            // Insert through SQL, because the entity already refuses nulls; the database must refuse them too.
            String sql = "insert into event (id, event_key, source_key, source_name, title, content, occurred_at,"
                    + " collected_at, run_id) values (?, 'k', ?, ?, ?, ?, ?, ?, ?)";
            Object[] values = {UUID.randomUUID(), "newsapi", "BBC News", "Title", "Content", Timestamp.from(T0),
                    Timestamp.from(T0), run.getId()};
            int index = switch (column) {
                case "source_key" -> 1;
                case "source_name" -> 2;
                case "title" -> 3;
                case "content" -> 4;
                default -> 5;
            };
            values[index] = null;

            assertThatThrownBy(() -> jdbcTemplate.update(sql, values))
                    .isInstanceOf(DataIntegrityViolationException.class);
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
