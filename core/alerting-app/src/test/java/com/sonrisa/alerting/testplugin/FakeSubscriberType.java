package com.sonrisa.alerting.testplugin;

import com.sonrisa.alerting.spi.subscriber.FieldError;
import com.sonrisa.alerting.spi.subscriber.NormalisedAddress;
import com.sonrisa.alerting.spi.subscriber.SubscriberInput;
import com.sonrisa.alerting.spi.subscriber.SubscriberType;
import com.sonrisa.alerting.spi.subscriber.VerificationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * A configurable subscriber type for the subscription service tests (BE-14): the address is required and must
 * contain {@code @}; normalisation strips and lower-cases it; the masked form keeps the first character. The
 * verification is replaceable per test and every call is recorded, with whether a transaction was open.
 */
public class FakeSubscriberType implements SubscriberType {

    /** One call of {@link #verify}. */
    public record VerifyCall(String address, String displayName, boolean inTransaction) {
    }

    private final String key;
    private final String channelKey;
    private final boolean secret;
    private final List<VerifyCall> verifyCalls = new CopyOnWriteArrayList<>();
    private volatile Function<NormalisedAddress, VerificationResult> verifier = address -> VerificationResult.verified();

    public FakeSubscriberType(String key, String channelKey, boolean secret) {
        this.key = key;
        this.channelKey = channelKey;
        this.secret = secret;
    }

    /** Replaces the verification; {@link #reset()} restores "verified". */
    public void verifyWith(Function<NormalisedAddress, VerificationResult> verifier) {
        this.verifier = verifier;
    }

    public List<VerifyCall> verifyCalls() {
        return List.copyOf(verifyCalls);
    }

    public void reset() {
        verifyCalls.clear();
        verifier = address -> VerificationResult.verified();
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String channelKey() {
        return channelKey;
    }

    @Override
    public boolean addressIsSecret() {
        return secret;
    }

    @Override
    public List<FieldError> validate(SubscriberInput input) {
        List<FieldError> errors = new ArrayList<>();
        String address = input.address();
        if (address == null || address.isBlank()) {
            errors.add(new FieldError(SubscriberInput.FIELD_ADDRESS, "required", "Enter an address."));
        } else if (!address.contains("@")) {
            errors.add(new FieldError(SubscriberInput.FIELD_ADDRESS, "invalid-format", "Enter a valid address."));
        }
        if (input.displayName() != null && input.displayName().length() > 50) {
            errors.add(new FieldError(SubscriberInput.FIELD_DISPLAY_NAME, "too-long", "The name is too long."));
        }
        return errors;
    }

    @Override
    public NormalisedAddress normalise(SubscriberInput input) {
        return new NormalisedAddress(input.address().strip().toLowerCase(Locale.ROOT));
    }

    @Override
    public VerificationResult verify(NormalisedAddress address, String displayName) {
        verifyCalls.add(new VerifyCall(address.value(), displayName,
                TransactionSynchronizationManager.isActualTransactionActive()));
        return verifier.apply(address);
    }

    @Override
    public String mask(NormalisedAddress address) {
        return address.value().charAt(0) + "***";
    }
}
