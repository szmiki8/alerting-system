package com.sonrisa.alerting.channel.slack;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.sonrisa.alerting.channel.slack.TestWebhooks.PATH;
import static com.sonrisa.alerting.channel.slack.TestWebhooks.SECRET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.sonrisa.alerting.spi.channel.DeliveryResult;
import com.sonrisa.alerting.spi.channel.DeliveryResult.Outcome;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** The webhook client against WireMock standing in for Slack (error list from the Slack docs, see the class). */
class SlackWebhookClientTest {

    private static final Duration CONNECT = Duration.ofSeconds(1);
    private static final Duration READ = Duration.ofMillis(300);
    private static final String PAYLOAD = "{\"text\":\"Hello\"}";

    @RegisterExtension
    static WireMockExtension slack = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private DeliveryResult postWith(ResponseDefinitionBuilder response) {
        slack.stubFor(post(urlEqualTo(PATH)).willReturn(response));
        return TestWebhooks.client(slack.baseUrl(), CONNECT, READ).post(TestWebhooks.webhook(), PAYLOAD);
    }

    @Test
    void okAnswerIsDeliveredAndTheJsonIsPostedOnce() {
        DeliveryResult result = postWith(aResponse().withStatus(200).withBody("ok"));

        assertThat(result).isEqualTo(DeliveryResult.delivered());
        slack.verify(1, postRequestedFor(urlEqualTo(PATH))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson(PAYLOAD)));
    }

    static Stream<Arguments> slackErrors() {
        return Stream.of(
                Arguments.of(400, "invalid_payload", Outcome.PERMANENT_FAILURE),
                Arguments.of(403, "invalid_token", Outcome.PERMANENT_FAILURE),
                Arguments.of(403, "action_prohibited", Outcome.PERMANENT_FAILURE),
                Arguments.of(404, "no_service", Outcome.PERMANENT_FAILURE),
                Arguments.of(404, "channel_not_found", Outcome.PERMANENT_FAILURE),
                Arguments.of(410, "channel_is_archived", Outcome.PERMANENT_FAILURE),
                Arguments.of(429, "rate_limited", Outcome.TRANSIENT_FAILURE),
                Arguments.of(500, "rollup_error", Outcome.TRANSIENT_FAILURE),
                Arguments.of(503, "", Outcome.TRANSIENT_FAILURE));
    }

    @ParameterizedTest(name = "HTTP {0} {1} is {2}")
    @MethodSource("slackErrors")
    void errorAnswersAreClassified(int status, String code, Outcome outcome) {
        DeliveryResult result = postWith(aResponse().withStatus(status).withBody(code));

        assertThat(result.outcome()).isEqualTo(outcome);
        assertThat(result.reason()).isEqualTo(("Slack: HTTP " + status + " " + code).strip());
        assertThat(result.retryAfter()).isNull();
        slack.verify(1, postRequestedFor(urlEqualTo(PATH)));
    }

    @Test
    void tooManyRequestsPassesRetryAfterBack() {
        DeliveryResult result = postWith(aResponse().withStatus(429).withHeader("Retry-After", "30"));

        assertThat(result.outcome()).isEqualTo(Outcome.TRANSIENT_FAILURE);
        assertThat(result.retryAfter()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void retryAfterOnlyAsPositiveSeconds() {
        assertThat(SlackWebhookClient.retryAfterSeconds("5")).contains(Duration.ofSeconds(5));
        assertThat(SlackWebhookClient.retryAfterSeconds(" 5 ")).contains(Duration.ofSeconds(5));
        assertThat(SlackWebhookClient.retryAfterSeconds("0")).isEmpty();
        assertThat(SlackWebhookClient.retryAfterSeconds("-1")).isEmpty();
        assertThat(SlackWebhookClient.retryAfterSeconds("Wed, 21 Oct 2026 07:28:00 GMT")).isEmpty();
        assertThat(SlackWebhookClient.retryAfterSeconds("99999999999")).isEmpty();
        assertThat(SlackWebhookClient.retryAfterSeconds(null)).isEmpty();
    }

    @Test
    void unexpectedBodyOfASuccessAndLongBodiesAreNotTrusted() {
        assertThat(postWith(aResponse().withStatus(200).withBody("<html>captive portal</html>")).reason())
                .isEqualTo("Slack: HTTP 200 with an unexpected body");
        assertThat(postWith(aResponse().withStatus(404).withBody("x".repeat(10_000))).reason())
                .isEqualTo("Slack: HTTP 404");
    }

    @Test
    void readTimeoutIsATransientFailure() {
        long start = System.nanoTime();

        DeliveryResult result = postWith(aResponse().withStatus(200).withBody("ok").withFixedDelay(3_000));

        assertThat(result).isEqualTo(DeliveryResult.transientFailure("Slack: timeout"));
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(2));
    }

    @Test
    void connectionErrorIsATransientFailure() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }

        DeliveryResult result = TestWebhooks.client("http://localhost:" + closedPort, CONNECT, READ)
                .post(TestWebhooks.webhook(), PAYLOAD);

        assertThat(result).isEqualTo(DeliveryResult.transientFailure("Slack: connection failed"));
    }

    @Test
    void redirectsAreNotFollowed() {
        slack.stubFor(post(urlPathEqualTo("/elsewhere")).willReturn(aResponse().withStatus(200).withBody("ok")));

        DeliveryResult result = postWith(aResponse().withStatus(307)
                .withHeader("Location", slack.baseUrl() + "/elsewhere"));

        assertThat(result.outcome()).isEqualTo(Outcome.TRANSIENT_FAILURE);
        assertThat(result.reason()).isEqualTo("Slack: HTTP 307 (redirect not followed)");
        slack.verify(0, anyRequestedFor(urlPathEqualTo("/elsewhere")));
        slack.verify(1, anyRequestedFor(anyUrl()));
    }

    @Test
    void reasonsNeverContainTheSecret() {
        assertThat(postWith(aResponse().withStatus(404).withBody("no_service")).reason()).doesNotContain(SECRET);
        assertThat(postWith(aResponse().withFixedDelay(3_000)).reason()).doesNotContain(SECRET);
    }

    @Test
    void refusesToBeBuiltWithoutTimeouts() {
        assertThatThrownBy(() -> TestWebhooks.client(slack.baseUrl(), Duration.ZERO, READ))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("connect timeout");
        assertThatThrownBy(() -> TestWebhooks.client(slack.baseUrl(), CONNECT, Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("read timeout");
    }

    @Test
    void productionBaseUriIsTheSlackHost() {
        assertThat(SlackWebhookClient.SLACK_BASE_URI).hasToString("https://hooks.slack.com");
    }
}
