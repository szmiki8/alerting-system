package com.sonrisa.alerting.channel.slack;

import com.sonrisa.alerting.spi.channel.DeliveryResult;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Posts one JSON message to one Slack incoming webhook and classifies the answer. Used for the welcome
 * message of the {@code slack} subscriber type (BE-18) and later by the Slack notification channel (BE-34).
 * Sends exactly once per call: retries and pacing are the application's job.
 *
 * <p><b>Security.</b> Only {@link SlackWebhookUrl}s can be addressed, and the host is fixed to
 * {@code https://hooks.slack.com}: the client takes only the IDs and the secret from the URL. Redirects are
 * <em>not followed</em>, so a redirect cannot lead the request to another host; a 3xx answer is reported
 * as a failure. The request URI is given as a template, so metrics see
 * {@code /services/{workspace}/{webhook}/{secret}}, never the secret. Failure reasons never contain the URL
 * or exception messages (which may contain it).
 *
 * <p><b>Timeouts</b> (OP-18): the client is built from Spring Boot's {@code RestClient.Builder} with a
 * request factory that has this client's own connect and read timeouts; the global
 * {@code spring.http.clients.*} settings remain the fallback for everything else.
 *
 * <p><b>Classification</b> (Slack docs "Sending messages using incoming webhooks", changelog 2016-05-17
 * "Changes to errors for incoming webhooks", "Rate limits"):
 * <ul>
 *   <li>2xx with body {@code ok}: delivered;</li>
 *   <li>429: transient, with the {@code Retry-After} seconds when given;</li>
 *   <li>5xx (for example 500 {@code rollup_error}): transient;</li>
 *   <li>other 4xx (400 {@code invalid_payload}, 403 {@code invalid_token}/{@code action_prohibited},
 *       404 {@code no_service}/{@code channel_not_found}, 410 {@code channel_is_archived}): permanent;</li>
 *   <li>3xx (not followed) and 2xx with another body: transient (unexpected; Slack does not do this);</li>
 *   <li>timeouts and connection errors: transient.</li>
 * </ul>
 * The reason names the status and Slack's error code, for example {@code Slack: HTTP 404 no_service}.
 */
public class SlackWebhookClient {

    /** The only base URI used in production. */
    static final URI SLACK_BASE_URI = URI.create("https://" + SlackWebhookUrl.HOST);

    private static final String PATH_TEMPLATE = "/services/{workspace}/{webhook}/{secret}";
    private static final int MAX_BODY_BYTES = 256;
    private static final Pattern ERROR_CODE = Pattern.compile("[a-z_]{1,64}");

    private final RestClient restClient;

    /**
     * A client for {@code https://hooks.slack.com}.
     *
     * @param builder a fresh {@code RestClient.Builder} (Spring Boot's prototype bean)
     * @param requestFactoryBuilder Spring Boot's request factory builder
     * @param globalSettings the global {@code spring.http.clients.*} settings
     * @param connectTimeout connect timeout of this client; positive
     * @param readTimeout read timeout of this client; positive
     */
    public SlackWebhookClient(RestClient.Builder builder, ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder,
            HttpClientSettings globalSettings, Duration connectTimeout, Duration readTimeout) {
        this(builder, requestFactoryBuilder, globalSettings, connectTimeout, readTimeout, SLACK_BASE_URI);
    }

    /** Test-only: the same client against another base URI (a WireMock server). Not reachable from configuration. */
    SlackWebhookClient(RestClient.Builder builder, ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder,
            HttpClientSettings globalSettings, Duration connectTimeout, Duration readTimeout, URI baseUri) {
        requirePositive(connectTimeout, "connect");
        requirePositive(readTimeout, "read");
        HttpClientSettings settings = globalSettings
                .withTimeouts(connectTimeout, readTimeout)
                .withRedirects(HttpRedirects.DONT_FOLLOW);
        this.restClient = builder
                .requestFactory(requestFactoryBuilder.build(settings))
                .baseUrl(baseUri.toString())
                .build();
    }

    /**
     * Posts the JSON payload to the webhook, once.
     *
     * @param webhook the webhook to call
     * @param jsonPayload the message as Slack JSON, for example {@code {"text":"Hello"}}
     * @return delivered, transient failure (with retry-after for a 429 that names one) or permanent failure
     */
    public DeliveryResult post(SlackWebhookUrl webhook, String jsonPayload) {
        Objects.requireNonNull(webhook, "webhook");
        Objects.requireNonNull(jsonPayload, "jsonPayload");
        try {
            return restClient.post()
                    .uri(PATH_TEMPLATE, webhook.workspaceId(), webhook.webhookId(), webhook.secret())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonPayload)
                    .exchange((request, response) -> classify(response.getStatusCode().value(),
                            response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER), readBody(response.getBody())));
        } catch (RestClientException failure) {
            return DeliveryResult.transientFailure(isTimeout(failure) ? "Slack: timeout" : "Slack: connection failed");
        }
    }

    /** Maps a Slack answer to a result (see the class comment). */
    static DeliveryResult classify(int status, @Nullable String retryAfter, String body) {
        String code = ERROR_CODE.matcher(body).matches() ? " " + body : "";
        if (status >= 200 && status < 300) {
            return "ok".equals(body)
                    ? DeliveryResult.delivered()
                    : DeliveryResult.transientFailure("Slack: HTTP " + status + " with an unexpected body");
        }
        String reason = "Slack: HTTP " + status + code;
        if (status == 429) {
            return retryAfterSeconds(retryAfter)
                    .map(wait -> DeliveryResult.transientFailure(reason, wait))
                    .orElseGet(() -> DeliveryResult.transientFailure(reason));
        }
        if (status >= 400 && status < 500) {
            return DeliveryResult.permanentFailure(reason);
        }
        // 5xx; or a 3xx, which is never followed (another host must not be reached).
        return DeliveryResult.transientFailure(status < 400 ? reason + " (redirect not followed)" : reason);
    }

    /** Slack sends {@code Retry-After} as a number of seconds; anything else is ignored. */
    static Optional<Duration> retryAfterSeconds(@Nullable String value) {
        if (value == null || !value.strip().matches("[0-9]{1,9}")) {
            return Optional.empty();
        }
        long seconds = Long.parseLong(value.strip());
        return seconds > 0 ? Optional.of(Duration.ofSeconds(seconds)) : Optional.empty();
    }

    private static String readBody(InputStream body) throws IOException {
        return new String(body.readNBytes(MAX_BODY_BYTES), StandardCharsets.UTF_8).strip();
    }

    private static boolean isTimeout(Throwable failure) {
        Throwable cause = failure;
        for (int depth = 0; cause != null && depth < 10; depth++, cause = cause.getCause()) {
            if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }

    private static void requirePositive(Duration timeout, String kind) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("The Slack webhook client needs a positive " + kind + " timeout");
        }
    }
}
