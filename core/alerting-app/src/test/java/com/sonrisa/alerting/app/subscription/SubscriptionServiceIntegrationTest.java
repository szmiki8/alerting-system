package com.sonrisa.alerting.app.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonrisa.alerting.app.persistence.PostgresTestcontainer;
import com.sonrisa.alerting.app.persistence.crypto.AddressFingerprinter;
import com.sonrisa.alerting.app.persistence.subscriber.Subscriber;
import com.sonrisa.alerting.app.persistence.subscriber.SubscriberRepository;
import com.sonrisa.alerting.app.persistence.subscriber.SubscriberStatus;
import com.sonrisa.alerting.spi.subscriber.SubscriberInput;
import com.sonrisa.alerting.spi.subscriber.VerificationResult;
import com.sonrisa.alerting.testplugin.FakeSubscriberType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * The subscription service against the real database (BE-14), on H2 and on PostgreSQL: storage with encryption,
 * duplicates, verification outside transactions, and concurrent sign-ups of one address. Test commits are real
 * (no test transaction), so every test uses its own random addresses; the PostgreSQL database is shared.
 */
class SubscriptionServiceIntegrationTest {

    static String randomAddress() {
        return "user-" + UUID.randomUUID() + "@example.org";
    }

    @SpringBootTest(properties = "alerting.channels.test-subscription.enabled=true")
    @ActiveProfiles("test")
    @ExtendWith(OutputCaptureExtension.class)
    abstract static class Checks {

        @Autowired
        SubscriptionService service;

        @Autowired
        SubscriberRepository repository;

        @Autowired
        AddressFingerprinter fingerprinter;

        @Autowired
        JdbcTemplate jdbcTemplate;

        @Autowired
        @Qualifier("testPlainSubscriberType")
        FakeSubscriberType plain;

        @Autowired
        @Qualifier("testSecretSubscriberType")
        FakeSubscriberType secret;

        @AfterEach
        void resetFakes() {
            plain.reset();
            secret.reset();
        }

        int rowsWithAddress(String normalisedAddress) {
            return jdbcTemplate.queryForObject("select count(*) from subscriber where address_fingerprint = ?",
                    Integer.class, fingerprinter.fingerprint(normalisedAddress));
        }

        Subscriber load(String normalisedAddress) {
            return repository.findByAddressFingerprint(fingerprinter.fingerprint(normalisedAddress)).orElseThrow();
        }

        @Test
        void newAddressIsStoredActive() {
            String address = randomAddress();
            Instant before = Instant.now();

            SubscriptionResult result = service.subscribe("test-plain", new SubscriberInput("Ada", address.toUpperCase()));

            assertThat(result).isEqualTo(SubscriptionResult.accepted());
            Subscriber subscriber = load(address);
            assertThat(subscriber.getType()).isEqualTo("test-plain");
            assertThat(subscriber.getStatus()).isEqualTo(SubscriberStatus.ACTIVE);
            assertThat(subscriber.getAddress()).isEqualTo(address);
            assertThat(subscriber.getAddressMasked()).isEqualTo("u***");
            assertThat(subscriber.getSubscribedAt()).isBetween(before.minusSeconds(1), Instant.now().plusSeconds(1));
            assertThat(jdbcTemplate.queryForObject("select address from subscriber where id = ?", String.class,
                    subscriber.getId())).isEqualTo(address);
        }

        @Test
        void secretAddressIsStoredEncrypted() {
            String address = randomAddress();

            service.subscribe("test-secret", new SubscriberInput("team", address));

            Subscriber subscriber = load(address);
            assertThat(subscriber.getAddress()).isEqualTo(address);
            assertThat(subscriber.isAddressEncrypted()).isTrue();
            String stored = jdbcTemplate.queryForObject("select address from subscriber where id = ?", String.class,
                    subscriber.getId());
            assertThat(stored).doesNotContain(address).doesNotContain("example.org");
        }

        @Test
        void existingAddressCreatesNothingAndIsNotVerifiedAgain() {
            String address = randomAddress();

            SubscriptionResult first = service.subscribe("test-secret", new SubscriberInput("team", address));
            SubscriptionResult second = service.subscribe("test-secret", new SubscriberInput("other", " " + address));

            assertThat(second).isEqualTo(first).isEqualTo(SubscriptionResult.accepted());
            assertThat(rowsWithAddress(address)).isEqualTo(1);
            assertThat(load(address).getDisplayName()).isEqualTo("team");
            assertThat(secret.verifyCalls()).hasSize(1);
        }

        @Test
        void verificationRunsOutsideAnyTransaction() {
            service.subscribe("test-secret", new SubscriberInput("team", randomAddress()));

            assertThat(secret.verifyCalls()).singleElement()
                    .extracting(FakeSubscriberType.VerifyCall::inTransaction).isEqualTo(false);
        }

        @Test
        void failedVerificationStoresNothing() {
            String address = randomAddress();
            secret.verifyWith(normalised -> VerificationResult.failed("HTTP 404 no_service"));

            SubscriptionResult result = service.subscribe("test-secret", new SubscriberInput("team", address));

            assertThat(result).isEqualTo(SubscriptionResult.notVerified());
            assertThat(rowsWithAddress(address)).isZero();
        }

        /**
         * All requests pass the "already subscribed?" check before any of them inserts: the verification waits
         * until every request has reached it. So all but one insert hit the unique fingerprint.
         */
        @Test
        void concurrentSignUpsOfOneAddressCreateExactlyOneSubscriber(CapturedOutput output) throws Exception {
            int requests = 6;
            String address = randomAddress();
            CyclicBarrier allChecked = new CyclicBarrier(requests);
            secret.verifyWith(normalised -> {
                try {
                    allChecked.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
                    throw new IllegalStateException("the requests did not meet", e);
                }
                return VerificationResult.verified();
            });
            ExecutorService executor = Executors.newFixedThreadPool(requests);
            try {
                List<Callable<SubscriptionResult>> calls = new ArrayList<>();
                for (int i = 0; i < requests; i++) {
                    String name = "request " + i;
                    calls.add(() -> service.subscribe("test-secret", new SubscriberInput(name, address)));
                }
                List<Future<SubscriptionResult>> results = executor.invokeAll(calls, 30, TimeUnit.SECONDS);

                for (Future<SubscriptionResult> result : results) {
                    assertThat(result.get()).isEqualTo(SubscriptionResult.accepted());
                }
            } finally {
                executor.shutdownNow();
            }
            assertThat(secret.verifyCalls()).hasSize(requests);
            assertThat(rowsWithAddress(address)).isEqualTo(1);
            // The losers took the unique-constraint path, not the "already subscribed" check.
            String loser = "Already subscribed (concurrent sign-up): type=test-secret, id=" + load(address).getId();
            assertThat(output.getAll().split(java.util.regex.Pattern.quote(loser), -1)).hasSize(requests);
        }

        @Test
        void logsShowOnlyTypeIdAndMaskedAddress(CapturedOutput output) {
            String address = randomAddress();

            service.subscribe("test-plain", new SubscriberInput("Ada Lovelace", address));
            service.subscribe("test-plain", new SubscriberInput("Ada Lovelace", address));
            Subscriber subscriber = load(address);

            assertThat(output.getAll())
                    .contains("Subscribed: type=test-plain, id=" + subscriber.getId() + ", address=u***")
                    .contains("Already subscribed: type=test-plain, id=" + subscriber.getId() + ", address=u***")
                    .doesNotContain(address)
                    .doesNotContain("Lovelace")
                    .doesNotContain(subscriber.getAddressFingerprint());
        }

        @Test
        void unknownTypeIsReported() {
            assertThat(service.subscribe("email", new SubscriberInput("Ada", randomAddress())))
                    .isEqualTo(SubscriptionResult.unknownType());
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
