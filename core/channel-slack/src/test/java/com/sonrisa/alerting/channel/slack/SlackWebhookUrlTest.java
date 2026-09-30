package com.sonrisa.alerting.channel.slack;

import static com.sonrisa.alerting.channel.slack.TestWebhooks.SECRET;
import static com.sonrisa.alerting.channel.slack.TestWebhooks.URL;
import static com.sonrisa.alerting.channel.slack.TestWebhooks.WEBHOOK;
import static com.sonrisa.alerting.channel.slack.TestWebhooks.WORKSPACE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SlackWebhookUrlTest {

    private static final String IDS = WORKSPACE + "/" + WEBHOOK + "/" + SECRET;

    @Test
    void acceptsTheCanonicalForm() {
        SlackWebhookUrl url = SlackWebhookUrl.parse(URL).orElseThrow();

        assertThat(url.canonical()).isEqualTo(URL);
        assertThat(Pattern.matches(SlackWebhookUrl.CANONICAL_PATTERN, url.canonical())).isTrue();
    }

    static Stream<String> equivalentSpellings() {
        return Stream.of(
                URL,
                "  " + URL + "\t\n",
                URL + "/",
                "HTTPS://HOOKS.SLACK.COM/services/" + IDS,
                "Https://Hooks.Slack.Com/services/" + IDS,
                "https://hooks.slack.com:443/services/" + IDS);
    }

    @ParameterizedTest
    @MethodSource("equivalentSpellings")
    void equivalentSpellingsNormaliseToOneCanonicalForm(String spelling) {
        assertThat(SlackWebhookUrl.parse(spelling)).get()
                .isEqualTo(SlackWebhookUrl.parse(URL).orElseThrow())
                .extracting(SlackWebhookUrl::canonical).isEqualTo(URL);
    }

    static Stream<String> rejected() {
        return Stream.of(
                "",
                "   ",
                "not a url",
                "http://hooks.slack.com/services/" + IDS,                     // no TLS
                "https://hooks.slack.com.evil.example/services/" + IDS,       // other host
                "https://evil.example/services/" + IDS,
                "https://evil.example/https://hooks.slack.com/services/" + IDS,
                "https://hooks.slack.com@evil.example/services/" + IDS,       // user info tricks
                "https://user@hooks.slack.com/services/" + IDS,
                "https://evil.example#@hooks.slack.com/services/" + IDS,
                "https://hooks.slack.com\\@evil.example/services/" + IDS,
                "https://slack.com/services/" + IDS,
                "https://hooks.slack-gov.com/services/" + IDS,                // GovSlack is not supported
                "https://127.0.0.1/services/" + IDS,
                "https://hooks.slack.com:8443/services/" + IDS,               // other port
                "https://hooks.slack.com/services/" + IDS + "/extra",         // extra path
                "https://hooks.slack.com/services/" + IDS + "//",
                "https://hooks.slack.com/services/" + IDS + "?a=b",           // query
                "https://hooks.slack.com/services/" + IDS + "?",
                "https://hooks.slack.com/services/" + IDS + "#top",           // fragment
                "https://hooks.slack.com/workflows/" + IDS,                   // not an incoming webhook
                "https://hooks.slack.com/services/../services/" + IDS,
                "https://hooks.slack.com/services/%54" + IDS.substring(1),    // percent-encoding
                "https://hooks.slack.com/services/" + WORKSPACE.toLowerCase() + "/" + WEBHOOK + "/" + SECRET,
                "https://hooks.slack.com/services/" + WORKSPACE + "/" + WEBHOOK + "/short",
                "https://hooks.slack.com/services/" + WORKSPACE + "/" + WEBHOOK,
                "https://hooks.slack.com/services/" + IDS.replace("/", "\n/"),
                URL + "/" + "x".repeat(300));
    }

    @ParameterizedTest
    @MethodSource("rejected")
    void rejectsEverythingElse(String input) {
        assertThat(SlackWebhookUrl.parse(input)).isEmpty();
    }

    @Test
    void rejectsNullAndOverlongInput() {
        assertThat(SlackWebhookUrl.parse(null)).isEmpty();
        assertThat(SlackWebhookUrl.parse(" ".repeat(10) + URL + " ".repeat(300))).isPresent();
        assertThat(SlackWebhookUrl.parse(URL + "a".repeat(SlackWebhookUrl.MAX_INPUT_LENGTH))).isEmpty();
    }

    @Test
    void maskShowsOnlyTheHostAndTheLastFourCharacters() {
        SlackWebhookUrl url = SlackWebhookUrl.parse(URL).orElseThrow();

        assertThat(url.masked()).isEqualTo("https://hooks.slack.com/services/...5678");
        assertThat(url.masked()).doesNotContain(WORKSPACE, WEBHOOK, SECRET.substring(0, 20));
        assertThat(url.toString()).doesNotContain(SECRET).contains("...5678");
    }
}
