package com.sonrisa.alerting.channel.slack;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.web.client.RestClient;

/**
 * Test fixture (not part of the plugin): {@link SlackWebhookClient}s that post to a stand-in for Slack, such as
 * a WireMock server, instead of {@code https://hooks.slack.com}. Tests of other modules use it to replace the
 * {@code slackVerificationClient} bean (a test-only bean override); there is deliberately no configuration
 * property for the base URL, so production code can reach the Slack host only.
 */
public final class SlackTestClients {

    /** Name of the plugin's webhook client bean for the welcome message. */
    public static final String VERIFICATION_CLIENT_BEAN = "slackVerificationClient";

    private SlackTestClients() {
    }

    /**
     * A client like the production one, but against {@code baseUri}. It posts to
     * {@code <baseUri>/services/<workspace>/<webhook>/<secret>}.
     *
     * @param baseUri the stand-in's base URI, for example {@code http://localhost:8089}
     * @param connectTimeout connect timeout; positive
     * @param readTimeout read timeout; positive
     */
    public static SlackWebhookClient against(URI baseUri, Duration connectTimeout, Duration readTimeout) {
        return new SlackWebhookClient(RestClient.builder(), ClientHttpRequestFactoryBuilder.detect(),
                HttpClientSettings.defaults(), connectTimeout, readTimeout, baseUri);
    }
}
