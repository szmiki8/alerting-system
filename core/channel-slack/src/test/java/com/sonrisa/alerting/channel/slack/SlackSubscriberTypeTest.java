package com.sonrisa.alerting.channel.slack;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonrisa.alerting.channel.slack.TestWebhooks.PATH;
import static com.sonrisa.alerting.channel.slack.TestWebhooks.SECRET;
import static com.sonrisa.alerting.channel.slack.TestWebhooks.URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.sonrisa.alerting.spi.subscriber.FieldError;
import com.sonrisa.alerting.spi.subscriber.NormalisedAddress;
import com.sonrisa.alerting.spi.subscriber.SubscriberInput;
import com.sonrisa.alerting.spi.subscriber.VerificationResult;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SlackSubscriberTypeTest {

    @RegisterExtension
    static WireMockExtension slack = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private final SlackSubscriberType type = new SlackSubscriberType(
            TestWebhooks.client(slack.baseUrl(), Duration.ofSeconds(1), Duration.ofMillis(300)));

    private static SubscriberInput input(String label, String url) {
        return new SubscriberInput(label, url);
    }

    @Test
    void describesItself() {
        assertThat(type.key()).isEqualTo("slack");
        assertThat(type.channelKey()).isEqualTo("slack");
        assertThat(type.addressIsSecret()).isTrue();
    }

    @Test
    void validInputWithAndWithoutLabel() {
        assertThat(type.validate(input("Team alerts", URL))).isEmpty();
        assertThat(type.validate(input(null, URL))).isEmpty();
        assertThat(type.validate(input("   ", URL))).isEmpty();
        assertThat(type.validate(input("  " + "x".repeat(SlackSubscriberType.MAX_LABEL_LENGTH) + "  ", URL)))
                .isEmpty();
    }

    @Test
    void missingUrlIsRequired() {
        assertThat(type.validate(input("Team", null))).extracting(FieldError::field, FieldError::code)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("address", "required"));
        assertThat(type.validate(input("Team", "  "))).extracting(FieldError::code).containsExactly("required");
    }

    static Stream<String> nonSlackUrls() {
        return Stream.of(
                URL.replace("https:", "http:"),
                URL.replace("hooks.slack.com", "evil.example"),
                URL + "/x",
                URL + "?x=1",
                "http://169.254.169.254/latest/meta-data/");
    }

    @ParameterizedTest
    @MethodSource("nonSlackUrls")
    void nonSlackUrlsAreRejectedWithoutAnyCall(String url) {
        List<FieldError> errors = type.validate(input("Team", url));

        assertThat(errors).extracting(FieldError::field, FieldError::code)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("address", "invalid-format"));
        assertThat(errors.get(0).message()).doesNotContain(url);
        assertThatThrownBy(() -> type.normalise(input("Team", url))).isInstanceOf(IllegalArgumentException.class);
        assertThat(type.verify(new NormalisedAddress(url), "Team").passed()).isFalse();
        slack.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void overlongUrlIsTooLong() {
        assertThat(type.validate(input(null, URL + "x".repeat(300)))).extracting(FieldError::code)
                .containsExactly("too-long");
    }

    @Test
    void labelLimits() {
        assertThat(type.validate(input("x".repeat(SlackSubscriberType.MAX_LABEL_LENGTH + 1), URL)))
                .extracting(FieldError::field, FieldError::code)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("displayName", "too-long"));
        assertThat(type.validate(input("Team\u0007alerts", URL))).extracting(FieldError::code)
                .containsExactly("invalid-format");
    }

    @Test
    void bothFieldsAreReportedTogether() {
        assertThat(type.validate(input("x".repeat(101), "nope"))).extracting(FieldError::field)
                .containsExactly("displayName", "address");
    }

    @Test
    void equivalentSpellingsNormaliseToTheSameAddress() {
        NormalisedAddress canonical = type.normalise(input(null, URL));
        NormalisedAddress other = type.normalise(input("Team", " HTTPS://Hooks.Slack.com:443" + PATH + "/ "));

        // The fingerprint is computed from the normalised value (OP-20), so equal values mean one fingerprint.
        assertThat(other).isEqualTo(canonical);
        assertThat(canonical.value()).isEqualTo(URL);
    }

    @Test
    void maskShowsHostAndLastCharactersOnly() {
        String masked = type.mask(type.normalise(input(null, URL)));

        assertThat(masked).isEqualTo("https://hooks.slack.com/services/...5678").doesNotContain(SECRET);
        assertThat(type.mask(new NormalisedAddress("garbage"))).isEqualTo("https://hooks.slack.com/services/...");
    }

    @Test
    void verificationPostsTheWelcomeMessageAndSucceedsOnOk() {
        slack.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(200).withBody("ok")));

        VerificationResult result = type.verify(type.normalise(input("Team", URL)), "Team");

        assertThat(result).isEqualTo(VerificationResult.verified());
        slack.verify(1, postRequestedFor(urlEqualTo(PATH))
                .withRequestBody(equalToJson("{\"text\":\"" + SlackSubscriberType.WELCOME_TEXT + "\"}")));
    }

    @Test
    void verificationFailsOnRejectionWithClassifiedReason() {
        slack.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(404).withBody("no_service")));

        VerificationResult result = type.verify(type.normalise(input(null, URL)), null);

        assertThat(result.passed()).isFalse();
        assertThat(result.reason()).isEqualTo("welcome message failed (permanent): Slack: HTTP 404 no_service");
    }

    @Test
    void verificationFailsOnServerErrorRateLimitAndTimeout() {
        NormalisedAddress address = type.normalise(input(null, URL));

        slack.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(500).withBody("rollup_error")));
        assertThat(type.verify(address, null).reason()).isEqualTo(
                "welcome message failed (transient): Slack: HTTP 500 rollup_error");

        slack.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(429).withHeader("Retry-After", "1")));
        assertThat(type.verify(address, null).reason()).isEqualTo(
                "welcome message failed (transient): Slack: HTTP 429");

        slack.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withBody("ok").withFixedDelay(2_000)));
        assertThat(type.verify(address, null).reason()).isEqualTo(
                "welcome message failed (transient): Slack: timeout");
    }
}
