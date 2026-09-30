package com.sonrisa.alerting.channel.slack;

import com.sonrisa.alerting.spi.channel.DeliveryResult;
import com.sonrisa.alerting.spi.subscriber.FieldError;
import com.sonrisa.alerting.spi.subscriber.NormalisedAddress;
import com.sonrisa.alerting.spi.subscriber.SubscriberInput;
import com.sonrisa.alerting.spi.subscriber.SubscriberType;
import com.sonrisa.alerting.spi.subscriber.VerificationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * The {@code slack} subscriber type (FR-03, FR-04, architecture Section 7.2): an incoming webhook URL
 * (the {@link SubscriberInput#address() address}) and an optional label (the
 * {@link SubscriberInput#displayName() display name}). The address is a secret (NFR-04, ADR-09).
 *
 * <ul>
 *   <li>Validation: the URL must be a {@link SlackWebhookUrl Slack incoming webhook URL}; the label is
 *       optional, at most {@value #MAX_LABEL_LENGTH} characters after trimming, without control
 *       characters. Error messages never echo the URL.</li>
 *   <li>Normalisation: the {@link SlackWebhookUrl#canonical() canonical URL}.</li>
 *   <li>Verification: posts the static {@link #WELCOME_TEXT welcome message} with the short
 *       {@code alerting.channels.slack.verification.*} timeouts; only a Slack {@code 200 ok} verifies.</li>
 *   <li>Masking: host and the last four characters of the secret.</li>
 * </ul>
 */
public class SlackSubscriberType implements SubscriberType {

    /** Type key and channel key. */
    public static final String KEY = "slack";

    /** Maximum label length (after trimming); the UI (FE-12) and the API (BE-19) mirror it. */
    public static final int MAX_LABEL_LENGTH = 100;

    /** The welcome message (static, English only, ASM-09). */
    public static final String WELCOME_TEXT = "Hello from the Alerting System! This channel is now subscribed"
            + " and will receive a message for every new event.";

    static final String WELCOME_PAYLOAD = "{\"text\":\"" + WELCOME_TEXT + "\"}";

    private final SlackWebhookClient verificationClient;

    public SlackSubscriberType(SlackWebhookClient verificationClient) {
        this.verificationClient = verificationClient;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String channelKey() {
        return KEY;
    }

    @Override
    public boolean addressIsSecret() {
        return true;
    }

    @Override
    public List<FieldError> validate(SubscriberInput input) {
        List<FieldError> errors = new ArrayList<>(2);
        String label = input.displayName() == null ? "" : input.displayName().strip();
        if (label.length() > MAX_LABEL_LENGTH) {
            errors.add(new FieldError(SubscriberInput.FIELD_DISPLAY_NAME, "too-long",
                    "The label can have at most " + MAX_LABEL_LENGTH + " characters."));
        } else if (label.chars().anyMatch(Character::isISOControl)) {
            errors.add(new FieldError(SubscriberInput.FIELD_DISPLAY_NAME, "invalid-format",
                    "The label must not contain control characters."));
        }
        String address = input.address();
        if (address == null || address.isBlank()) {
            errors.add(new FieldError(SubscriberInput.FIELD_ADDRESS, "required", "Enter a Slack webhook URL."));
        } else if (address.strip().length() > SlackWebhookUrl.MAX_INPUT_LENGTH) {
            errors.add(new FieldError(SubscriberInput.FIELD_ADDRESS, "too-long",
                    "The webhook URL can have at most " + SlackWebhookUrl.MAX_INPUT_LENGTH + " characters."));
        } else if (SlackWebhookUrl.parse(address).isEmpty()) {
            errors.add(new FieldError(SubscriberInput.FIELD_ADDRESS, "invalid-format",
                    "Enter a Slack incoming webhook URL that starts with https://hooks.slack.com/services/."));
        }
        return errors;
    }

    @Override
    public NormalisedAddress normalise(SubscriberInput input) {
        SlackWebhookUrl url = SlackWebhookUrl.parse(input.address())
                .orElseThrow(() -> new IllegalArgumentException("not a Slack incoming webhook URL"));
        return new NormalisedAddress(url.canonical());
    }

    /**
     * Posts the welcome message. The address is checked again, so that nothing but a Slack webhook URL is
     * ever called, whoever calls this method.
     */
    @Override
    public VerificationResult verify(NormalisedAddress address, @Nullable String displayName) {
        Optional<SlackWebhookUrl> url = SlackWebhookUrl.parse(address.value());
        if (url.isEmpty()) {
            return VerificationResult.failed("not a Slack incoming webhook URL; nothing was sent");
        }
        DeliveryResult result = verificationClient.post(url.get(), WELCOME_PAYLOAD);
        if (result.isDelivered()) {
            return VerificationResult.verified();
        }
        String kind = result.outcome() == DeliveryResult.Outcome.PERMANENT_FAILURE ? "permanent" : "transient";
        return VerificationResult.failed("welcome message failed (" + kind + "): " + result.reason());
    }

    @Override
    public String mask(NormalisedAddress address) {
        return SlackWebhookUrl.parse(address.value()).map(SlackWebhookUrl::masked)
                .orElse("https://" + SlackWebhookUrl.HOST + "/services/...");
    }
}
