package com.sonrisa.alerting.app.persistence.subscriber;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sonrisa.alerting.app.persistence.PostgresTestcontainer;
import com.sonrisa.alerting.app.persistence.RepositoryTest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/** SUBSCRIBER table and repository (BE-07), on H2 and on PostgreSQL. */
class SubscriberRepositoryTest {

    static final Instant T0 = Instant.parse("2026-09-01T10:15:30.123456Z");

    static Subscriber email(String name, String address, Instant subscribedAt) {
        return new Subscriber(UUID.randomUUID(), "email", name, address, false, fingerprintOf(address),
                address.charAt(0) + "***", subscribedAt);
    }

    /** Stand-in for the keyed fingerprint: an unkeyed SHA-256 in hex has the same shape. */
    static String fingerprintOf(String address) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(address.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @RepositoryTest
    abstract static class Checks {

        @Autowired
        SubscriberRepository repository;

        @Autowired
        TestEntityManager entityManager;

        @BeforeEach
        void startEmpty() {
            // Rolled back with the test transaction.
            repository.deleteAll();
        }

        @Test
        void storesAllAttributesWithUtcInstants() {
            Subscriber saved = repository.saveAndFlush(email("Ada Lovelace", "ada@example.org", T0));
            entityManager.clear();

            Subscriber loaded = repository.findById(saved.getId()).orElseThrow();

            assertThat(loaded).isNotSameAs(saved);
            assertThat(loaded.getType()).isEqualTo("email");
            assertThat(loaded.getDisplayName()).isEqualTo("Ada Lovelace");
            assertThat(loaded.getAddress()).isEqualTo("ada@example.org");
            assertThat(loaded.getAddressFingerprint()).isEqualTo(fingerprintOf("ada@example.org"));
            assertThat(loaded.getAddressMasked()).isEqualTo("a***");
            assertThat(loaded.getStatus()).isEqualTo(SubscriberStatus.ACTIVE);
            assertThat(loaded.getSubscribedAt()).isEqualTo(T0);
            assertThat(loaded.getStatusChangedAt()).isEqualTo(T0);
            assertThat(loaded.toString()).doesNotContain("ada@example.org").doesNotContain("Lovelace");
        }

        @Test
        void statusChangeIsStored() {
            Subscriber saved = repository.saveAndFlush(email("Ada", "ada@example.org", T0));
            Instant later = T0.plusSeconds(60);

            saved.changeStatus(SubscriberStatus.INACTIVE, later);
            repository.flush();
            entityManager.clear();

            Subscriber loaded = repository.findById(saved.getId()).orElseThrow();
            assertThat(loaded.getStatus()).isEqualTo(SubscriberStatus.INACTIVE);
            assertThat(loaded.getStatusChangedAt()).isEqualTo(later);
        }

        @Test
        void fingerprintIsUnique() {
            repository.saveAndFlush(email("First", "same@example.org", T0));
            Subscriber duplicate = email("Second", "same@example.org", T0.plusSeconds(1));

            assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        void findsByFingerprint() {
            Subscriber saved = repository.saveAndFlush(email("Ada", "ada@example.org", T0));

            assertThat(repository.existsByAddressFingerprint(fingerprintOf("ada@example.org"))).isTrue();
            assertThat(repository.existsByAddressFingerprint(fingerprintOf("bob@example.org"))).isFalse();
            assertThat(repository.findByAddressFingerprint(fingerprintOf("ada@example.org"))).contains(saved);
        }

        @Test
        void pagesNewestFirst() {
            for (int i = 0; i < 5; i++) {
                repository.save(email("Subscriber " + i, "s" + i + "@example.org", T0.plusSeconds(i)));
            }
            repository.flush();

            Page<Subscriber> first = repository.search("", PageRequest.of(0, 2, SubscriberRepository.NEWEST_FIRST));
            Page<Subscriber> last = repository.search(null, PageRequest.of(2, 2, SubscriberRepository.NEWEST_FIRST));

            assertThat(first.getTotalElements()).isEqualTo(5);
            assertThat(first.getTotalPages()).isEqualTo(3);
            assertThat(first.getContent()).extracting(Subscriber::getDisplayName)
                    .containsExactly("Subscriber 4", "Subscriber 3");
            assertThat(last.getContent()).extracting(Subscriber::getDisplayName).containsExactly("Subscriber 0");
        }

        @Test
        void searchesNameAndAddressIgnoringCase() {
            repository.save(email("Ada Lovelace", "ada@example.org", T0));
            repository.save(email("Bob Builder", "bob@example.net", T0.plusSeconds(1)));
            repository.save(email("Carol", "carol@EXAMPLE.org", T0.plusSeconds(2)));
            repository.flush();
            var page = PageRequest.of(0, 10, SubscriberRepository.NEWEST_FIRST);

            assertThat(repository.search("LOVELACE", page).getContent()).extracting(Subscriber::getDisplayName)
                    .containsExactly("Ada Lovelace");
            assertThat(repository.search(" example.org ", page).getContent()).extracting(Subscriber::getDisplayName)
                    .containsExactly("Carol", "Ada Lovelace");
            assertThat(repository.search("builder", page).getTotalElements()).isEqualTo(1);
        }

        @Test
        void searchTreatsWildcardsLiterally() {
            repository.save(email("100% sure", "percent@example.org", T0));
            repository.save(email("plain", "plain@example.org", T0.plusSeconds(1)));
            repository.flush();
            var page = PageRequest.of(0, 10);

            assertThat(repository.search("%", page).getContent()).extracting(Subscriber::getDisplayName)
                    .containsExactly("100% sure");
            assertThat(repository.search("_", page).getContent()).isEmpty();
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
