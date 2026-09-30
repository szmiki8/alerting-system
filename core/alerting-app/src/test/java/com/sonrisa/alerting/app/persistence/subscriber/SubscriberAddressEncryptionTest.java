package com.sonrisa.alerting.app.persistence.subscriber;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonrisa.alerting.app.persistence.PostgresTestcontainer;
import com.sonrisa.alerting.app.persistence.RepositoryTest;
import com.sonrisa.alerting.app.persistence.crypto.AddressFingerprinter;
import com.sonrisa.alerting.app.persistence.crypto.AttributeEncryptor;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Secret addresses (Slack webhook URLs) are encrypted in the table and decrypted through the entity; email
 * addresses stay plain and searchable (BE-09). On H2 and on PostgreSQL.
 */
class SubscriberAddressEncryptionTest {

    static final String WEBHOOK = "https://hooks.slack.com/services/T0001/B0001/abcdefghijklmnopqrstuvwx";
    static final String EMAIL = "ada@example.org";
    static final Instant T0 = Instant.parse("2026-09-01T10:00:00Z");

    @RepositoryTest
    abstract static class Checks {

        @Autowired
        SubscriberRepository repository;

        @Autowired
        AddressFingerprinter fingerprinter;

        @Autowired
        AttributeEncryptor encryptor;

        @Autowired
        TestEntityManager entityManager;

        @Autowired
        JdbcTemplate jdbcTemplate;

        Subscriber slack(String label) {
            return new Subscriber(UUID.randomUUID(), "slack", label, WEBHOOK, true, fingerprinter.fingerprint(WEBHOOK),
                    "https://hooks.slack.com/services/T0001/****", T0);
        }

        Subscriber email() {
            return new Subscriber(UUID.randomUUID(), "email", "Ada", EMAIL, false, fingerprinter.fingerprint(EMAIL),
                    "a***@example.org", T0);
        }

        String storedAddress(UUID id) {
            return jdbcTemplate.queryForObject("select address from subscriber where id = ?", String.class, id);
        }

        @Test
        void webhookUrlIsNotReadableInTheTable() {
            Subscriber saved = repository.saveAndFlush(slack("team"));

            String stored = storedAddress(saved.getId());

            assertThat(stored).startsWith(encryptor.activeKeyId() + ":")
                    .doesNotContain("hooks.slack.com")
                    .doesNotContain("abcdefghijklmnopqrstuvwx");
            assertThat(jdbcTemplate.queryForObject("select address_encrypted from subscriber where id = ?",
                    Boolean.class, saved.getId())).isTrue();
        }

        @Test
        void webhookUrlDecryptsThroughTheEntity() {
            Subscriber saved = repository.saveAndFlush(slack("team"));
            assertThat(saved.getAddress()).isEqualTo(WEBHOOK);
            entityManager.clear();

            Subscriber loaded = repository.findById(saved.getId()).orElseThrow();

            assertThat(loaded).isNotSameAs(saved);
            assertThat(loaded.isAddressEncrypted()).isTrue();
            assertThat(loaded.getAddress()).isEqualTo(WEBHOOK);
        }

        @Test
        void emailAddressStaysPlainAndSearchable() {
            Subscriber saved = repository.saveAndFlush(email());
            entityManager.clear();

            assertThat(storedAddress(saved.getId())).isEqualTo(EMAIL);
            assertThat(repository.findById(saved.getId()).orElseThrow().getAddress()).isEqualTo(EMAIL);
            assertThat(repository.search("ada@", PageRequest.of(0, 10)).getContent())
                    .extracting(Subscriber::getId).containsExactly(saved.getId());
        }

        @Test
        void encryptedAddressesAreNotSearched() {
            repository.deleteAll();
            Subscriber saved = repository.saveAndFlush(slack("team"));
            String ciphertextFragment = storedAddress(saved.getId()).substring(10, 20);

            assertThat(repository.search(ciphertextFragment, PageRequest.of(0, 10))).isEmpty();
            assertThat(repository.search("team", PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
        }

        @Test
        void fingerprintFindsTheWebhookWithoutDecrypting() {
            repository.saveAndFlush(slack("team"));

            assertThat(repository.existsByAddressFingerprint(fingerprinter.fingerprint(WEBHOOK))).isTrue();
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
