package com.sonrisa.alerting.app.subscription;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.sonrisa.alerting.app.persistence.crypto.AddressFingerprinter;
import com.sonrisa.alerting.app.persistence.subscriber.Subscriber;
import com.sonrisa.alerting.app.persistence.subscriber.SubscriberRepository;
import com.sonrisa.alerting.app.persistence.subscriber.SubscriberStatus;
import com.sonrisa.alerting.app.plugin.SubscriberTypeRegistry;
import com.sonrisa.alerting.channel.slack.SlackSubscriberType;
import com.sonrisa.alerting.channel.slack.SlackTestClients;
import com.sonrisa.alerting.channel.slack.SlackWebhookClient;
import com.sonrisa.alerting.channel.slack.SlackWebhookUrl;
import com.sonrisa.alerting.spi.subscriber.FieldError;
import com.sonrisa.alerting.spi.subscriber.SubscriberInput;
import com.sonrisa.alerting.spi.subscriber.SubscriberType;
import jakarta.servlet.http.Cookie;
import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * The sign-up endpoints end to end (BE-16, BE-19): the real {@code email} and {@code slack} subscriber types from
 * the plugin modules, the subscription service, the database, CSRF as the UI does it (cookie from
 * {@code GET /api/v1/csrf}, value in the {@code X-XSRF-TOKEN} header), and WireMock standing in for Slack.
 *
 * <p>The real email and Slack channels follow in BE-33 and BE-34, so the test-only channels of
 * {@code com.sonrisa.alerting.testplugin} are enabled with the plugins (OP-21). The Slack plugin's webhook client
 * bean is replaced by one that posts to WireMock (test-only bean override; production code can reach only the
 * Slack host). Test commits are real, so every test uses fresh addresses.
 */
@SpringBootTest(properties = {
    "alerting.channels.email.enabled=true", "alerting.channels.test-email.enabled=true",
    "alerting.channels.slack.enabled=true", "alerting.channels.test-slack.enabled=true"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class SubscriptionApiIntegrationTest {

    static final String EMAIL_PATH = "/api/v1/subscriptions/email";
    static final String SLACK_PATH = "/api/v1/subscriptions/slack";
    static final String CONFIRMATION = """
            {"message": "Thank you. Your subscription has been received."}""";

    /** Stands in for {@code https://hooks.slack.com}; started before the Spring context asks for the client. */
    static final WireMockServer SLACK = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        SLACK.start();
    }

    @TestBean(name = SlackTestClients.VERIFICATION_CLIENT_BEAN, methodName = "slackClientAgainstWireMock")
    SlackWebhookClient slackVerificationClient;

    static SlackWebhookClient slackClientAgainstWireMock() {
        return SlackTestClients.against(URI.create(SLACK.baseUrl()), Duration.ofSeconds(1), Duration.ofSeconds(1));
    }

    @AfterAll
    static void stopSlack() {
        SLACK.stop();
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    SubscriberRepository subscribers;

    @Autowired
    AddressFingerprinter fingerprinter;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    SubscriberTypeRegistry types;

    @BeforeEach
    void resetSlack() {
        SLACK.resetAll();
    }

    /** Signs up like the UI: fetch the CSRF cookie, then POST with the cookie and the header. */
    ResultActions signUp(String path, String json) throws Exception {
        Cookie csrf = mvc.perform(MockMvcRequestBuilders.get("/api/v1/csrf")).andReturn().getResponse()
                .getCookie("XSRF-TOKEN");
        assertThat(csrf).isNotNull();
        return mvc.perform(MockMvcRequestBuilders.post(path)
                .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    Optional<Subscriber> stored(String normalisedAddress) {
        return subscribers.findByAddressFingerprint(fingerprinter.fingerprint(normalisedAddress));
    }

    long subscriberCount() {
        return subscribers.count();
    }

    static String json(String... keysAndValues) {
        StringBuilder json = new StringBuilder("{");
        for (int i = 0; i < keysAndValues.length; i += 2) {
            json.append(i == 0 ? "" : ", ").append('"').append(keysAndValues[i]).append("\": \"")
                    .append(keysAndValues[i + 1]).append('"');
        }
        return json.append('}').toString();
    }

    @Nested
    class Email {

        final String marker = UUID.randomUUID().toString().substring(0, 8);
        final String address = "reader-" + marker + "@example.org";
        final String name = "Reader " + marker;

        @Test
        void newAddressIsAcceptedAndStored(CapturedOutput output) throws Exception {
            signUp(EMAIL_PATH, json("name", name, "email", address))
                    .andExpect(status().isAccepted())
                    .andExpect(content().json(CONFIRMATION, JsonCompareMode.STRICT));

            Subscriber subscriber = stored(address).orElseThrow();
            assertThat(subscriber.getType()).isEqualTo("email");
            assertThat(subscriber.getStatus()).isEqualTo(SubscriberStatus.ACTIVE);
            assertThat(subscriber.getDisplayName()).isEqualTo(name);
            assertThat(subscriber.getAddress()).isEqualTo(address);
            assertThat(subscriber.getAddressMasked()).isEqualTo("r***@example.org");
            assertThat(output.getAll()).doesNotContain(address, marker).contains("r***@example.org");
        }

        @Test
        void sameAddressAgainGetsTheIdenticalAnswerAndStoresNothingNew() throws Exception {
            String first = signUp(EMAIL_PATH, json("name", name, "email", address))
                    .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
            long count = subscriberCount();

            String second = signUp(EMAIL_PATH, json("name", "Someone else", "email",
                    " " + address.toUpperCase(Locale.ROOT) + " "))
                    .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();

            assertThat(second).isEqualTo(first);
            assertThat(subscriberCount()).isEqualTo(count);
            assertThat(stored(address).orElseThrow().getDisplayName()).isEqualTo(name);
        }

        @Test
        void invalidInputIs400WithFieldErrorsAndStoresNothing(CapturedOutput output) throws Exception {
            long count = subscriberCount();
            String malformed = "not-an-email-" + marker;

            // Passes the request's Bean Validation; the subscriber type (the final authority) refuses both fields.
            MvcResult result = signUp(EMAIL_PATH, json("name", "Line\\nbreak", "email", malformed))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.type").value("urn:alerting:problem:validation"))
                    .andExpect(jsonPath("$.errors", hasSize(2)))
                    .andExpect(jsonPath("$.errors[0].field").value("name"))
                    .andExpect(jsonPath("$.errors[1].field").value("email"))
                    .andExpect(jsonPath("$.errors[1].message")
                            .value("Enter a valid email address, for example name@example.com."))
                    .andReturn();

            // Refused by Bean Validation before the subscriber type is asked.
            signUp(EMAIL_PATH, json("name", " ", "email", address))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].field").value("name"));

            assertThat(subscriberCount()).isEqualTo(count);
            assertThat(stored(address)).isEmpty();
            assertThat(result.getResponse().getContentAsString()).doesNotContain(malformed);
            assertThat(output.getAll()).doesNotContain(malformed, address);
        }

        @Test
        void withoutCsrfTokenTheAnswerIs403AndNothingIsStored() throws Exception {
            mvc.perform(MockMvcRequestBuilders.post(EMAIL_PATH).contentType(MediaType.APPLICATION_JSON)
                            .content(json("name", name, "email", address)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.type").value("urn:alerting:problem:csrf-token-invalid"));

            assertThat(stored(address)).isEmpty();
        }

        @Test
        void requestLimitsMirrorTheSubscriberType() {
            SubscriberType email = types.get("email");
            String longestName = "n".repeat(EmailSubscriptionRequest.NAME_MAX_LENGTH);
            // Local part 64, domain labels of at most 63 characters: 64 + 1 + 189 = 254.
            String longestEmail = "a".repeat(64) + "@" + "d".repeat(60) + "." + "d".repeat(60) + "."
                    + "d".repeat(EmailSubscriptionRequest.EMAIL_MAX_LENGTH - 192) + ".test";

            assertThat(email.validate(new SubscriberInput(longestName, longestEmail))).isEmpty();
            assertThat(email.validate(new SubscriberInput(longestName + "n", "x" + longestEmail)))
                    .extracting(FieldError::code).containsExactly("too-long", "too-long");
        }
    }

    @Nested
    class Slack {

        final String marker = UUID.randomUUID().toString().replace("-", "");
        final String secret = marker.substring(0, 24);
        final String path = "/services/T" + marker.substring(0, 9).toUpperCase(Locale.ROOT)
                + "/B" + marker.substring(9, 18).toUpperCase(Locale.ROOT) + "/" + secret;
        final String webhookUrl = "https://hooks.slack.com" + path;
        final String label = "#alerts-" + marker.substring(24);

        void slackAnswers(int status, String body) {
            SLACK.stubFor(post(urlEqualTo(path)).willReturn(aResponse().withStatus(status).withBody(body)));
        }

        @Test
        void newWebhookGetsTheWelcomeMessageAndIsStoredEncrypted(CapturedOutput output) throws Exception {
            slackAnswers(200, "ok");

            signUp(SLACK_PATH, json("webhookUrl", webhookUrl, "label", label))
                    .andExpect(status().isAccepted())
                    .andExpect(content().json(CONFIRMATION, JsonCompareMode.STRICT));

            SLACK.verify(1, postRequestedFor(urlEqualTo(path)).withRequestBody(
                    equalToJson("{\"text\": \"" + SlackSubscriberType.WELCOME_TEXT + "\"}")));
            Subscriber subscriber = stored(webhookUrl).orElseThrow();
            assertThat(subscriber.getType()).isEqualTo("slack");
            assertThat(subscriber.getDisplayName()).isEqualTo(label);
            assertThat(subscriber.isAddressEncrypted()).isTrue();
            assertThat(subscriber.getAddressMasked()).isEqualTo("https://hooks.slack.com/services/..."
                    + secret.substring(20));
            String column = jdbcTemplate.queryForObject("select address from subscriber where id = ?", String.class,
                    subscriber.getId());
            assertThat(column).doesNotContain(secret, "hooks.slack.com");
            assertThat(output.getAll()).doesNotContain(webhookUrl, secret, label);
        }

        @Test
        void labelIsOptional() throws Exception {
            slackAnswers(200, "ok");

            signUp(SLACK_PATH, json("webhookUrl", webhookUrl)).andExpect(status().isAccepted());

            assertThat(stored(webhookUrl).orElseThrow().getDisplayName()).isNull();
        }

        @Test
        void registeredWebhookGetsTheIdenticalAnswerWithoutASecondWelcome() throws Exception {
            slackAnswers(200, "ok");
            String first = signUp(SLACK_PATH, json("webhookUrl", webhookUrl))
                    .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
            long count = subscriberCount();

            // Another spelling of the same webhook (trailing slash, upper-case host).
            String second = signUp(SLACK_PATH, json("webhookUrl",
                    "https://HOOKS.slack.com" + path + "/", "label", label))
                    .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();

            assertThat(second).isEqualTo(first);
            assertThat(subscriberCount()).isEqualTo(count);
            SLACK.verify(1, postRequestedFor(urlEqualTo(path)));
        }

        @Test
        void invalidUrlIs400WithoutAnyCallAndIsNotEchoed(CapturedOutput output) throws Exception {
            String notSlack = "https://evil.example.org" + path;

            MvcResult result = signUp(SLACK_PATH, json("webhookUrl", notSlack, "label", label))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("urn:alerting:problem:validation"))
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].field").value("webhookUrl"))
                    .andReturn();

            SLACK.verify(0, anyRequestedFor(anyUrl()));
            assertThat(result.getResponse().getContentAsString()).doesNotContain(secret, "evil.example.org");
            assertThat(output.getAll()).doesNotContain(secret, "evil.example.org", label);
        }

        @Test
        void tooLongLabelIs400() throws Exception {
            signUp(SLACK_PATH, json("webhookUrl", webhookUrl,
                    "label", "l".repeat(SlackSubscriptionRequest.LABEL_MAX_LENGTH + 1)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].field").value("label"));

            SLACK.verify(0, anyRequestedFor(anyUrl()));
        }

        @Test
        void failedWelcomeMessageIs422AndStoresNothing(CapturedOutput output) throws Exception {
            slackAnswers(404, "no_service");
            long count = subscriberCount();

            MvcResult result = signUp(SLACK_PATH, json("webhookUrl", webhookUrl, "label", label))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.type").value("urn:alerting:problem:webhook-not-verified"))
                    .andExpect(jsonPath("$.status").value(422))
                    .andReturn();

            SLACK.verify(1, postRequestedFor(urlEqualTo(path)));
            assertThat(stored(webhookUrl)).isEmpty();
            assertThat(subscriberCount()).isEqualTo(count);
            assertThat(result.getResponse().getContentAsString()).doesNotContain(secret);
            assertThat(output.getAll()).doesNotContain(webhookUrl, secret, label)
                    .contains("Verification failed: type=slack");
        }

        @Test
        void slackServerErrorIs422Too() throws Exception {
            slackAnswers(500, "rollup_error");

            signUp(SLACK_PATH, json("webhookUrl", webhookUrl))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.type").value("urn:alerting:problem:webhook-not-verified"));

            assertThat(stored(webhookUrl)).isEmpty();
        }

        @Test
        void withoutCsrfTokenTheAnswerIs403AndSlackIsNotCalled() throws Exception {
            mvc.perform(MockMvcRequestBuilders.post(SLACK_PATH).contentType(MediaType.APPLICATION_JSON)
                            .content(json("webhookUrl", webhookUrl)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.type").value("urn:alerting:problem:csrf-token-invalid"));

            SLACK.verify(0, anyRequestedFor(anyUrl()));
            assertThat(stored(webhookUrl)).isEmpty();
        }

        @Test
        void requestLimitsMirrorTheSubscriberType() {
            assertThat(SlackSubscriptionRequest.LABEL_MAX_LENGTH).isEqualTo(SlackSubscriberType.MAX_LABEL_LENGTH);
            assertThat(SlackSubscriptionRequest.WEBHOOK_URL_MAX_LENGTH).isEqualTo(SlackWebhookUrl.MAX_INPUT_LENGTH);
        }
    }
}
