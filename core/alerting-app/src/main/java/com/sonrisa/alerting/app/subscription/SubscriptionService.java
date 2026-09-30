package com.sonrisa.alerting.app.subscription;

import com.sonrisa.alerting.app.persistence.crypto.AddressFingerprinter;
import com.sonrisa.alerting.app.persistence.subscriber.Subscriber;
import com.sonrisa.alerting.app.persistence.subscriber.SubscriberRepository;
import com.sonrisa.alerting.app.plugin.SubscriberTypeRegistry;
import com.sonrisa.alerting.spi.subscriber.FieldError;
import com.sonrisa.alerting.spi.subscriber.NormalisedAddress;
import com.sonrisa.alerting.spi.subscriber.SubscriberInput;
import com.sonrisa.alerting.spi.subscriber.SubscriberType;
import com.sonrisa.alerting.spi.subscriber.VerificationResult;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Signs up subscribers of any type (architecture Sections 6.1, 10.1, 10.2; FR-01 to FR-05, ADR-09, ADR-11).
 *
 * <p>Steps: find the enabled {@link SubscriberType}; let it validate and normalise the input; compute the
 * fingerprint of the normalised address; if a subscriber with that fingerprint exists, answer "accepted" without
 * any change; otherwise let the type verify the address (for example the Slack welcome message) and store an
 * ACTIVE subscriber with its masked form, encrypted when the type declares the address secret.
 *
 * <p><b>Transactions.</b> This service is deliberately not transactional and refuses to run inside a caller's
 * transaction: the verification contacts an external service and must not hold a database connection or locks
 * (BE-14), and a failed insert must not mark an outer transaction rollback-only. Each repository call runs in its
 * own short transaction.
 *
 * <p><b>Concurrency.</b> Two requests for the same new address may both pass the existence check. The unique
 * fingerprint column lets only one insert win; the other sees the violation, finds the winner's row and answers
 * "accepted" too. In that race both requests run the verification (a Slack workspace may get two welcome
 * messages), which is accepted as rare and harmless.
 *
 * <p><b>Logging.</b> Only the type key, the subscriber ID and the masked address (NFR-04, NFR-16); never the
 * address, the display name or the fingerprint.
 */
@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriberTypeRegistry types;
    private final SubscriberRepository subscribers;
    private final AddressFingerprinter fingerprinter;
    private final Clock clock;

    @Autowired
    public SubscriptionService(SubscriberTypeRegistry types, SubscriberRepository subscribers,
            AddressFingerprinter fingerprinter) {
        this(types, subscribers, fingerprinter, Clock.systemUTC());
    }

    SubscriptionService(SubscriberTypeRegistry types, SubscriberRepository subscribers,
            AddressFingerprinter fingerprinter, Clock clock) {
        this.types = types;
        this.subscribers = subscribers;
        this.fingerprinter = fingerprinter;
        this.clock = clock;
    }

    /**
     * Tells whether sign-ups of the given type are possible, that is whether its plugin is enabled.
     *
     * @param typeKey a subscriber type key, for example {@code email}
     * @return {@code true} when an enabled subscriber type has this key
     */
    public boolean offers(String typeKey) {
        return types.contains(typeKey);
    }

    /**
     * Subscribes the address in {@code input} with the subscriber type {@code typeKey}.
     *
     * @param typeKey the key of an enabled subscriber type, for example {@code email}
     * @param input the raw sign-up input
     * @return {@link SubscriptionResult#accepted()} for a new and for an existing address alike, or why the
     *     sign-up was refused; in the refused cases nothing is stored
     * @throws IllegalStateException when called inside an active transaction
     */
    public SubscriptionResult subscribe(String typeKey, SubscriberInput input) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("SubscriptionService.subscribe must not run inside a transaction:"
                    + " the verification calls an external service");
        }
        Optional<SubscriberType> found = types.find(typeKey);
        if (found.isEmpty()) {
            log.info("Sign-up refused: no enabled subscriber type '{}'", typeKey);
            return SubscriptionResult.unknownType();
        }
        SubscriberType type = found.get();

        List<FieldError> errors = type.validate(input);
        if (!errors.isEmpty()) {
            log.info("Sign-up refused: invalid input for type={}, fields={}", type.key(),
                    errors.stream().map(error -> error.field() + ":" + error.code()).toList());
            return SubscriptionResult.invalid(errors);
        }

        NormalisedAddress address = type.normalise(input);
        String fingerprint = fingerprinter.fingerprint(address.value());
        String masked = type.mask(address);
        String displayName = displayName(input);

        Optional<Subscriber> existing = subscribers.findByAddressFingerprint(fingerprint);
        if (existing.isPresent()) {
            log.info("Already subscribed: type={}, id={}, address={}", type.key(), existing.get().getId(), masked);
            return SubscriptionResult.accepted();
        }

        if (!verify(type, address, displayName, masked)) {
            return SubscriptionResult.notVerified();
        }

        Subscriber subscriber = new Subscriber(UUID.randomUUID(), type.key(), displayName, address.value(),
                type.addressIsSecret(), fingerprint, masked, clock.instant());
        try {
            subscribers.saveAndFlush(subscriber);
        } catch (DataIntegrityViolationException e) {
            // Most likely a concurrent sign-up of the same address won the unique fingerprint; anything else is a
            // real error.
            Subscriber winner = subscribers.findByAddressFingerprint(fingerprint).orElseThrow(() -> e);
            log.info("Already subscribed (concurrent sign-up): type={}, id={}, address={}", type.key(),
                    winner.getId(), masked);
            return SubscriptionResult.accepted();
        }
        log.info("Subscribed: type={}, id={}, address={}", type.key(), subscriber.getId(), masked);
        return SubscriptionResult.accepted();
    }

    /** Runs the type's verification; an exception counts as a failed verification. */
    private static boolean verify(SubscriberType type, NormalisedAddress address, String displayName, String masked) {
        VerificationResult result;
        try {
            result = type.verify(address, displayName);
        } catch (RuntimeException e) {
            // The message might contain the address, so only the exception class is logged.
            log.warn("Verification failed with an exception: type={}, address={}, exception={}", type.key(), masked,
                    e.getClass().getName());
            return false;
        }
        if (!result.passed()) {
            log.info("Verification failed: type={}, address={}, reason={}", type.key(), masked, result.reason());
            return false;
        }
        return true;
    }

    /** The display name without surrounding white space; {@code null} when none was given. */
    private static String displayName(SubscriberInput input) {
        String name = input.displayName();
        return name == null || name.isBlank() ? null : name.strip();
    }
}
