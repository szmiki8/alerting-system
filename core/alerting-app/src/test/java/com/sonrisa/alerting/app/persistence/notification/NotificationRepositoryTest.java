package com.sonrisa.alerting.app.persistence.notification;

import static com.sonrisa.alerting.app.persistence.TestRecords.T0;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sonrisa.alerting.app.persistence.PostgresTestcontainer;
import com.sonrisa.alerting.app.persistence.RepositoryTest;
import com.sonrisa.alerting.app.persistence.TestRecords;
import com.sonrisa.alerting.app.persistence.event.Event;
import com.sonrisa.alerting.app.persistence.event.EventRepository;
import com.sonrisa.alerting.app.persistence.run.Run;
import com.sonrisa.alerting.app.persistence.run.RunRepository;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/** NOTIFICATION table and repository (BE-08), on H2 and on PostgreSQL. */
class NotificationRepositoryTest {

    static final String EMAIL = "email";

    @RepositoryTest
    @TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
    abstract static class Checks {

        @Autowired
        NotificationRepository notifications;

        @Autowired
        EventRepository events;

        @Autowired
        RunRepository runs;

        @Autowired
        TestEntityManager entityManager;

        @Autowired
        EntityManagerFactory entityManagerFactory;

        @Autowired
        JdbcTemplate jdbcTemplate;

        Run run;

        Event event;

        @BeforeEach
        void createRunAndEvent() {
            notifications.deleteAll();
            run = runs.saveAndFlush(TestRecords.run());
            event = events.saveAndFlush(TestRecords.event("newsapi:" + UUID.randomUUID(), run));
        }

        @Test
        void newNotificationIsPendingAndDue() {
            Notification saved = notifications.saveAndFlush(TestRecords.notification(event, UUID.randomUUID(), EMAIL));
            entityManager.clear();

            Notification loaded = notifications.findById(saved.getId()).orElseThrow();

            assertThat(loaded.getStatus()).isEqualTo(NotificationStatus.PENDING);
            assertThat(loaded.getAttempts()).isZero();
            assertThat(loaded.getNextAttemptAt()).isEqualTo(T0);
            assertThat(loaded.getCreatedAt()).isEqualTo(T0);
            assertThat(loaded.getEventId()).isEqualTo(event.getId());
            assertThat(loaded.getRunId()).isEqualTo(run.getId());
            assertThat(loaded.getLeaseUntil()).isNull();
            assertThat(loaded.getSentAt()).isNull();
        }

        @Test
        void eventAndSubscriberPairIsUnique() {
            UUID subscriberId = UUID.randomUUID();
            notifications.saveAndFlush(TestRecords.notification(event, subscriberId, EMAIL));

            assertThatThrownBy(() -> notifications.saveAndFlush(TestRecords.notification(event, subscriberId, EMAIL)))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        void subscriberIdHasNoForeignKey() {
            // No SUBSCRIBER row exists for this id: the history must survive the deletion of subscribers.
            notifications.saveAndFlush(TestRecords.notification(event, UUID.randomUUID(), EMAIL));

            assertThat(notifications.count()).isEqualTo(1);
        }

        @Test
        void eventMustExist() {
            Event unsaved = TestRecords.event("newsapi:unsaved", run);

            assertThatThrownBy(() -> notifications.saveAndFlush(TestRecords.notification(unsaved, UUID.randomUUID(), EMAIL)))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        void findsClaimableNotifications() {
            Instant now = T0.plusSeconds(3600);
            Notification due = save(n -> { });
            Notification retryDue = save(n -> n.scheduleRetry(now.minusSeconds(1), "HTTP 503"));
            Notification retryLater = save(n -> n.scheduleRetry(now.plusSeconds(60), "HTTP 429"));
            Notification leaseExpired = save(n -> n.claim(now.minusSeconds(1)));
            Notification leased = save(n -> n.claim(now.plusSeconds(60)));
            Notification sent = save(n -> n.markSent(now.minusSeconds(10)));
            Notification failed = save(n -> n.markFailed("HTTP 404"));
            Notification otherChannel = notifications.save(TestRecords.notification(event, UUID.randomUUID(), "slack"));
            notifications.flush();
            entityManager.clear();

            List<Notification> claimable = notifications.findClaimable(EMAIL, now, Limit.of(10));

            // Oldest due first: the new one and the expired lease are due since T0, the retry since now - 1s.
            assertThat(claimable).extracting(Notification::getId)
                    .containsExactlyInAnyOrder(due.getId(), leaseExpired.getId(), retryDue.getId())
                    .doesNotContain(retryLater.getId(), leased.getId(), sent.getId(), failed.getId(),
                            otherChannel.getId());
            assertThat(claimable.get(2).getId()).isEqualTo(retryDue.getId());
            assertThat(notifications.findClaimable(EMAIL, now, Limit.of(2))).hasSize(2);
        }

        @Test
        void insertsInJdbcBatches() {
            List<Event> batchEvents = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                batchEvents.add(events.save(TestRecords.event("newsapi:batch-" + UUID.randomUUID(), run)));
            }
            events.flush();
            List<Notification> batch = new ArrayList<>();
            for (Event batchEvent : batchEvents) {
                for (int i = 0; i < 40; i++) {
                    batch.add(TestRecords.notification(batchEvent, UUID.randomUUID(), EMAIL));
                }
            }
            Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
            statistics.clear();

            notifications.saveAll(batch);
            notifications.flush();

            assertThat(statistics.getEntityInsertCount()).isEqualTo(120);
            // New entities with application ids: no select before the insert ...
            assertThat(statistics.getEntityLoadCount()).isZero();
            // ... and 120 rows in batches of 50 need 3 prepared statements, not 120.
            assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
            assertThat(jdbcTemplate.queryForObject("select count(*) from notification", Integer.class)).isEqualTo(120);
        }

        private Notification save(java.util.function.Consumer<Notification> change) {
            Notification notification = TestRecords.notification(event, UUID.randomUUID(), EMAIL);
            change.accept(notification);
            return notifications.save(notification);
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
