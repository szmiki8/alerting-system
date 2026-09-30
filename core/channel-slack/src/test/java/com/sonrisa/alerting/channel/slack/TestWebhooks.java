package com.sonrisa.alerting.channel.slack;

import java.net.URI;
import java.time.Duration;

/**
 * Fake webhook URLs and clients for tests. The URLs are assembled from parts so that no string in the
 * repository looks like a real webhook to secret scanners.
 */
final class TestWebhooks {

    static final String WORKSPACE = "T" + "0TESTTEAM";
    static final String WEBHOOK = "B" + "0TESTHOOK";
    static final String SECRET = "abcdEFGH1234" + "ijklMNOP5678";
    static final String PATH = "/services/" + WORKSPACE + "/" + WEBHOOK + "/" + SECRET;
    static final String URL = "https://hooks.slack.com" + PATH;

    private TestWebhooks() {
    }

    static SlackWebhookUrl webhook() {
        return SlackWebhookUrl.parse(URL).orElseThrow();
    }

    /** A client against the given base URI (WireMock), built like the production client. */
    static SlackWebhookClient client(String baseUri, Duration connectTimeout, Duration readTimeout) {
        return SlackTestClients.against(URI.create(baseUri), connectTimeout, readTimeout);
    }
}
