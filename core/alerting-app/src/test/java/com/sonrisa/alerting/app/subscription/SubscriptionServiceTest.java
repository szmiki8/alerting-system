package com.sonrisa.alerting.app.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sonrisa.alerting.app.persistence.crypto.AddressFingerprinter;
import com.sonrisa.alerting.app.persistence.subscriber.Subscriber;
import com.sonrisa.alerting.app.persistence.subscriber.SubscriberRepository;
import com.sonrisa.alerting.app.persistence.subscriber.SubscriberStatus;
import com.sonrisa.alerting.app.plugin.NotificationChannelRegistry;
import com.sonrisa.alerting.app.plugin.SubscriberTypeRegistry;
import com.sonrisa.alerting.app.subscription.SubscriptionResult.Status;
import com.sonrisa.alerting.spi.subscriber.FieldError;
import com.sonrisa.alerting.spi.subscriber.SubscriberInput;
import com.sonrisa.alerting.spi.subscriber.VerificationResult;
import com.sonrisa.alerting.testplugin.FakeSubscriberType;
import com.sonrisa.alerting.testplugin.TestChannel;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** The sign-up steps of the subscription service with fake subscriber types and a mocked repository (BE-14). */
@ExtendWith(OutputCaptureExtension.class)
class SubscriptionServiceTest {

    static final Instant NOW = Instant.parse("2026-09-30T08:15:00Z");
    static final String ADDRESS = "Ada@Example.ORG ";
    static final String NORMALISED = "ada@example.org";

    final FakeSubscriberType plain = new FakeSubscriberType("test-plain", "test-channel", false);
    final FakeSubscriberType secret = new FakeSubscriberType("test-secret", "test-channel", true);
    final AddressFingerprinter fingerprinter = new AddressFingerprinter("unit-test-key".getBytes(StandardCharsets.UTF_8));
    final SubscriberRepository repository = mock(SubscriberRepository.class);
    SubscriptionService service;

    @BeforeEach
    void setUp() {
        SubscriberTypeRegistry types = new SubscriberTypeRegistry(List.of(plain, secret),
                new NotificationChannelRegistry(List.of(new TestChannel("test-channel"))));
        service = new SubscriptionService(types, repository, fingerprinter, Clock.fixed(NOW, ZoneOffset.UTC));
        when(repository.findByAddressFingerprint(any())).thenReturn(Optional.empty());
    }

    Subscriber stored() {
        ArgumentCaptor<Subscriber> captor = ArgumentCaptor.forClass(Subscriber.class);
        verify(repository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    Subscriber existing(String type) {
        return new Subscriber(UUID.randomUUID(), type, "Ada", NORMALISED, false, fingerprinter.fingerprint(NORMALISED),
                "a***", NOW.minusSeconds(3600));
    }

    @Test
    void newAddressIsStoredActiveWithMaskedFormAndFingerprint() {
        SubscriptionResult result = service.subscribe("test-plain", new SubscriberInput("  Ada  ", ADDRESS));

        assertThat(result).isEqualTo(SubscriptionResult.accepted());
        Subscriber subscriber = stored();
        assertThat(subscriber.getType()).isEqualTo("test-plain");
        assertThat(subscriber.getDisplayName()).isEqualTo("Ada");
        assertThat(subscriber.getAddress()).isEqualTo(NORMALISED);
        assertThat(subscriber.isAddressEncrypted()).isFalse();
        assertThat(subscriber.getAddressFingerprint()).isEqualTo(fingerprinter.fingerprint(NORMALISED));
        assertThat(subscriber.getAddressMasked()).isEqualTo("a***");
        assertThat(subscriber.getStatus()).isEqualTo(SubscriberStatus.ACTIVE);
        assertThat(subscriber.getSubscribedAt()).isEqualTo(NOW);
        assertThat(plain.verifyCalls()).extracting(FakeSubscriberType.VerifyCall::address).containsExactly(NORMALISED);
    }

    @Test
    void secretAddressIsMarkedForEncryption() {
        service.subscribe("test-secret", new SubscriberInput(null, ADDRESS));

        Subscriber subscriber = stored();
        assertThat(subscriber.isAddressEncrypted()).isTrue();
        assertThat(subscriber.getDisplayName()).isNull();
    }

    @Test
    void blankDisplayNameIsStoredAsNone() {
        service.subscribe("test-plain", new SubscriberInput("   ", ADDRESS));

        assertThat(stored().getDisplayName()).isNull();
        assertThat(plain.verifyCalls().get(0).displayName()).isNull();
    }

    @Test
    void existingAddressCreatesNothingAndIsNotVerifiedAgain() {
        when(repository.findByAddressFingerprint(fingerprinter.fingerprint(NORMALISED)))
                .thenReturn(Optional.of(existing("test-plain")));

        SubscriptionResult result = service.subscribe("test-plain", new SubscriberInput("Someone else", ADDRESS));

        assertThat(result).isEqualTo(SubscriptionResult.accepted());
        verify(repository, never()).saveAndFlush(any());
        assertThat(plain.verifyCalls()).isEmpty();
    }

    @Test
    void inactiveSubscriberIsVerifiedAgainAndReactivated() {
        Subscriber inactive = existing("test-secret");
        inactive.changeStatus(SubscriberStatus.INACTIVE, NOW.minusSeconds(60));
        when(repository.findByAddressFingerprint(fingerprinter.fingerprint(NORMALISED)))
                .thenReturn(Optional.of(inactive));

        SubscriptionResult result = service.subscribe("test-secret", new SubscriberInput("team", ADDRESS));

        assertThat(result).isEqualTo(SubscriptionResult.accepted());
        assertThat(secret.verifyCalls()).hasSize(1);
        Subscriber saved = stored();
        assertThat(saved.getId()).isEqualTo(inactive.getId());
        assertThat(saved.getStatus()).isEqualTo(SubscriberStatus.ACTIVE);
        assertThat(saved.getStatusChangedAt()).isEqualTo(NOW);
    }

    @Test
    void inactiveSubscriberStaysInactiveWhenVerificationFails() {
        Subscriber inactive = existing("test-secret");
        inactive.changeStatus(SubscriberStatus.INACTIVE, NOW.minusSeconds(60));
        when(repository.findByAddressFingerprint(fingerprinter.fingerprint(NORMALISED)))
                .thenReturn(Optional.of(inactive));
        secret.verifyWith(address -> VerificationResult.failed("HTTP 404 no_service"));

        SubscriptionResult result = service.subscribe("test-secret", new SubscriberInput("team", ADDRESS));

        assertThat(result).isEqualTo(SubscriptionResult.notVerified());
        assertThat(inactive.getStatus()).isEqualTo(SubscriberStatus.INACTIVE);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void newAndExistingAddressesGiveTheSameResult() {
        SubscriptionResult first = service.subscribe("test-plain", new SubscriberInput("Ada", ADDRESS));
        when(repository.findByAddressFingerprint(any())).thenReturn(Optional.of(existing("test-plain")));
        SubscriptionResult second = service.subscribe("test-plain", new SubscriberInput("Ada", ADDRESS));

        assertThat(second).isEqualTo(first).isSameAs(first);
        assertThat(first.status()).isEqualTo(Status.ACCEPTED);
        assertThat(first.fieldErrors()).isEmpty();
    }

    @Test
    void failedVerificationStoresNothing() {
        secret.verifyWith(address -> VerificationResult.failed("HTTP 404 no_service"));

        SubscriptionResult result = service.subscribe("test-secret", new SubscriberInput("team", ADDRESS));

        assertThat(result).isEqualTo(SubscriptionResult.notVerified());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void exceptionDuringVerificationCountsAsNotVerified(CapturedOutput output) {
        secret.verifyWith(address -> {
            throw new IllegalStateException("connection to " + address.value() + " refused");
        });

        SubscriptionResult result = service.subscribe("test-secret", new SubscriberInput("team", ADDRESS));

        assertThat(result.status()).isEqualTo(Status.NOT_VERIFIED);
        verify(repository, never()).saveAndFlush(any());
        assertThat(output.getAll()).contains("java.lang.IllegalStateException").doesNotContain(NORMALISED);
    }

    @Test
    void invalidInputReturnsTheFieldErrorsAndTouchesNothing() {
        SubscriptionResult result = service.subscribe("test-plain", new SubscriberInput("x".repeat(51), "no-at-sign"));

        assertThat(result.status()).isEqualTo(Status.INVALID);
        assertThat(result.fieldErrors()).extracting(FieldError::field, FieldError::code).containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(SubscriberInput.FIELD_ADDRESS, "invalid-format"),
                org.assertj.core.groups.Tuple.tuple(SubscriberInput.FIELD_DISPLAY_NAME, "too-long"));
        verifyNoInteractions(repository);
        assertThat(plain.verifyCalls()).isEmpty();
    }

    @Test
    void unknownOrDisabledTypeIsReported() {
        SubscriptionResult result = service.subscribe("slack", new SubscriberInput("team", ADDRESS));

        assertThat(result).isEqualTo(SubscriptionResult.unknownType());
        verifyNoInteractions(repository);
    }

    @Test
    void uniqueViolationFromAConcurrentSignUpIsAccepted() {
        String fingerprint = fingerprinter.fingerprint(NORMALISED);
        when(repository.findByAddressFingerprint(fingerprint))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing("test-plain")));
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique"));

        SubscriptionResult result = service.subscribe("test-plain", new SubscriberInput("Ada", ADDRESS));

        assertThat(result).isEqualTo(SubscriptionResult.accepted());
    }

    @Test
    void otherIntegrityViolationsAreNotHidden() {
        DataIntegrityViolationException failure = new DataIntegrityViolationException("value too long");
        when(repository.saveAndFlush(any())).thenThrow(failure);

        assertThatThrownBy(() -> service.subscribe("test-plain", new SubscriberInput("Ada", ADDRESS)))
                .isSameAs(failure);
    }

    @Test
    void refusesToRunInsideATransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            assertThatThrownBy(() -> service.subscribe("test-plain", new SubscriberInput("Ada", ADDRESS)))
                    .isInstanceOf(IllegalStateException.class);
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
        verifyNoInteractions(repository);
    }

    @Test
    void logsOnlyTypeIdAndMaskedAddress(CapturedOutput output) {
        service.subscribe("test-plain", new SubscriberInput("Ada Lovelace", ADDRESS));
        Subscriber subscriber = stored();

        assertThat(output.getAll())
                .contains("type=test-plain", "id=" + subscriber.getId(), "address=a***")
                .doesNotContain(NORMALISED)
                .doesNotContainIgnoringCase(ADDRESS.strip())
                .doesNotContain("Lovelace")
                .doesNotContain(subscriber.getAddressFingerprint());
    }

    @Test
    void resultRejectsInconsistentFieldErrors() {
        FieldError error = new FieldError("address", "required", "Enter an address.");
        assertThatThrownBy(() -> new SubscriptionResult(Status.INVALID, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SubscriptionResult(Status.ACCEPTED, List.of(error)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
