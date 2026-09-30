package com.sonrisa.alerting.spi.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChannelValueTypesTest {

    private static final ZoneId BUDAPEST = ZoneId.of("Europe/Budapest");
    private static final Instant AT = Instant.parse("2026-09-30T10:00:00Z");

    @Test
    void contentRendersTheTimeInTheConfiguredZone() {
        NotificationContent content = new NotificationContent("Title", "Summary", "BBC News", AT,
                URI.create("https://example.org/a"), BUDAPEST);

        // Budapest is on summer time (UTC+2) on this date.
        assertThat(content.occurredAtInZone().getHour()).isEqualTo(12);
    }

    @Test
    void contentNeedsTitleSourceTimeAndZone() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new NotificationContent(" ", "Summary", "Source", AT, null, BUDAPEST));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new NotificationContent("Title", "Summary", "", AT, null, BUDAPEST));
        assertThatNullPointerException()
                .isThrownBy(() -> new NotificationContent("Title", "Summary", "Source", null, null, BUDAPEST));
        assertThatNullPointerException()
                .isThrownBy(() -> new NotificationContent("Title", "Summary", "Source", AT, null, null));
        assertThat(new NotificationContent("Title", "", "Source", AT, null, BUDAPEST).content()).isEmpty();
    }

    @Test
    void recipientHidesItsAddress() {
        Recipient recipient = new Recipient("slack", "https://hooks.slack.com/services/T0/B0/secret", null);

        assertThat(recipient.toString()).doesNotContain("secret").contains("slack");
        assertThatIllegalArgumentException().isThrownBy(() -> new Recipient("slack", " ", null));
        assertThatIllegalArgumentException().isThrownBy(() -> new Recipient("", "a@example.org", "Anna"));
    }

    @Test
    void deliveryResultInvariants() {
        assertThat(DeliveryResult.delivered().isDelivered()).isTrue();
        assertThat(DeliveryResult.transientFailure("HTTP 503").retryAfter()).isNull();
        assertThat(DeliveryResult.transientFailure("HTTP 429", Duration.ofSeconds(30)).retryAfter())
                .isEqualTo(Duration.ofSeconds(30));
        assertThat(DeliveryResult.permanentFailure("HTTP 410").outcome())
                .isEqualTo(DeliveryResult.Outcome.PERMANENT_FAILURE);

        assertThatIllegalArgumentException().isThrownBy(() -> DeliveryResult.transientFailure(" "));
        assertThatIllegalArgumentException().isThrownBy(() -> DeliveryResult.permanentFailure(null));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DeliveryResult(DeliveryResult.Outcome.DELIVERED, "why", null));
        assertThatIllegalArgumentException().isThrownBy(() -> new DeliveryResult(
                DeliveryResult.Outcome.PERMANENT_FAILURE, "HTTP 404", Duration.ofSeconds(1)));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> DeliveryResult.transientFailure("HTTP 429", Duration.ZERO));
    }

    @Test
    void pacingLimitsNeedPositiveRates() {
        PacingLimits slack = new PacingLimits(null, PacingLimits.Rate.perSecond(1));
        assertThat(slack.perRecipient().period()).isEqualTo(Duration.ofSeconds(1));
        assertThat(PacingLimits.unlimited().global()).isNull();

        assertThatIllegalArgumentException().isThrownBy(() -> PacingLimits.Rate.perSecond(0));
        assertThatIllegalArgumentException().isThrownBy(() -> new PacingLimits.Rate(1, Duration.ZERO));
        assertThatIllegalArgumentException().isThrownBy(() -> new PacingLimits.Rate(1, Duration.ofSeconds(-1)));
    }

    @Test
    void deliverRendersThenSends() {
        List<String> sent = new ArrayList<>();
        NotificationChannel<String> channel = new NotificationChannel<>() {
            @Override
            public String key() {
                return "memory";
            }

            @Override
            public String render(NotificationContent content, Recipient recipient) {
                return content.title() + " @ " + content.occurredAtInZone().toLocalTime();
            }

            @Override
            public DeliveryResult send(String message, Recipient recipient) {
                sent.add(message);
                return DeliveryResult.delivered();
            }

            @Override
            public PacingLimits pacingLimits() {
                return PacingLimits.unlimited();
            }
        };

        DeliveryResult result = channel.deliver(
                new NotificationContent("Title", "Summary", "Source", AT, null, BUDAPEST),
                new Recipient("memory", "anna", "Anna"));

        assertThat(result.isDelivered()).isTrue();
        assertThat(sent).containsExactly("Title @ 12:00");
    }
}
