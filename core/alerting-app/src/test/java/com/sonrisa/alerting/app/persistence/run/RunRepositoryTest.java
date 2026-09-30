package com.sonrisa.alerting.app.persistence.run;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sonrisa.alerting.app.persistence.PostgresTestcontainer;
import com.sonrisa.alerting.app.persistence.RepositoryTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

/** RUN, RUN_SOURCE_RESULT and SOURCE_STATE tables and repositories (BE-08), on H2 and on PostgreSQL. */
class RunRepositoryTest {

    static final Instant T0 = Instant.parse("2026-09-01T10:00:00.000001Z");

    @RepositoryTest
    abstract static class Checks {

        @Autowired
        RunRepository runs;

        @Autowired
        RunSourceResultRepository results;

        @Autowired
        SourceStateRepository sourceStates;

        @Autowired
        TestEntityManager entityManager;

        @Autowired
        JdbcTemplate jdbcTemplate;

        @Test
        void storesAFinishedRunWithCounts() {
            Run run = runs.saveAndFlush(new Run(UUID.randomUUID(), RunTrigger.CATCH_UP, T0));
            run.recordCounts(3, 25, 2);
            run.finish(RunStatus.PARTIAL, T0.plusSeconds(90), "newsapi: timeout");
            runs.flush();
            entityManager.clear();

            Run loaded = runs.findById(run.getId()).orElseThrow();

            assertThat(loaded.getTrigger()).isEqualTo(RunTrigger.CATCH_UP);
            assertThat(loaded.getStatus()).isEqualTo(RunStatus.PARTIAL);
            assertThat(loaded.getStartedAt()).isEqualTo(T0);
            assertThat(loaded.getFinishedAt()).isEqualTo(T0.plusSeconds(90));
            assertThat(loaded.getNewEvents()).isEqualTo(3);
            assertThat(loaded.getSent()).isEqualTo(25);
            assertThat(loaded.getFailed()).isEqualTo(2);
            assertThat(loaded.getErrorSummary()).isEqualTo("newsapi: timeout");
        }

        @Test
        void pagesRunHistoryNewestFirst() {
            runs.deleteAll();
            for (int i = 0; i < 3; i++) {
                runs.save(new Run(UUID.randomUUID(), RunTrigger.SCHEDULED, T0.plusSeconds(3600L * i)));
            }
            runs.flush();

            var page = runs.findAll(PageRequest.of(0, 2, RunRepository.NEWEST_FIRST));

            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(page.getContent()).extracting(Run::getStartedAt)
                    .containsExactly(T0.plusSeconds(7200), T0.plusSeconds(3600));
        }

        @Test
        void storesSourceResultsPerRunAndDeletesThemWithTheRun() {
            Run run = runs.saveAndFlush(new Run(UUID.randomUUID(), RunTrigger.MANUAL, T0));
            results.save(new RunSourceResult(new RunSourceResultId(run.getId(), "stub"),
                    RunSourceResultStatus.COMPLETED, 1, 4, null));
            results.save(new RunSourceResult(new RunSourceResultId(run.getId(), "newsapi"),
                    RunSourceResultStatus.FAILED, 3, 0, "HTTP 503"));
            results.flush();
            entityManager.clear();

            assertThat(results.findByIdRunIdOrderByIdSourceKey(run.getId()))
                    .extracting(result -> result.getId().sourceKey(), RunSourceResult::getStatus,
                            RunSourceResult::getAttempts)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple("newsapi", RunSourceResultStatus.FAILED, 3),
                            org.assertj.core.groups.Tuple.tuple("stub", RunSourceResultStatus.COMPLETED, 1));

            // Retention deletes runs; their source results go with them (ON DELETE CASCADE).
            jdbcTemplate.update("delete from run where id = ?", run.getId());
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from run_source_result where run_id = ?", Integer.class, run.getId())).isZero();
        }

        @Test
        void storesSourceState() {
            SourceState state = new SourceState("newsapi");
            state.recordSuccess(T0);
            sourceStates.saveAndFlush(state);
            entityManager.clear();

            assertThat(sourceStates.findById("newsapi")).get()
                    .extracting(SourceState::getLastSuccessAt).isEqualTo(T0);
        }

        @Test
        void rejectsUnknownStatusValues() {
            assertThatThrownBy(() -> jdbcTemplate.update(
                    "insert into run (id, run_trigger, status, started_at, new_events, sent, failed)"
                            + " values (?, 'SCHEDULED', 'DONE', ?, 0, 0, 0)",
                    UUID.randomUUID(), java.sql.Timestamp.from(T0)))
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
