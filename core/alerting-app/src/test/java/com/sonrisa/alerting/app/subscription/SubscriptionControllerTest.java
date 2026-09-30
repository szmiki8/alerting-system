package com.sonrisa.alerting.app.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sonrisa.alerting.app.api.ProblemResponseWriter;
import com.sonrisa.alerting.app.security.ApplicationSecurityConfiguration;
import com.sonrisa.alerting.spi.subscriber.FieldError;
import com.sonrisa.alerting.spi.subscriber.SubscriberInput;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Web slice test of the sign-up endpoints (BE-16, BE-19): request mapping, Bean Validation, the mapping of every
 * {@link SubscriptionResult} to HTTP, CSRF and the disabled-plugin answer. The service is a mock; the full flow
 * with the real plugins is in {@link SubscriptionApiIntegrationTest}.
 */
@WebMvcTest(SubscriptionController.class)
@ActiveProfiles("test")
@Import({ApplicationSecurityConfiguration.class, ProblemResponseWriter.class})
class SubscriptionControllerTest {

    static final String EMAIL_PATH = "/api/v1/subscriptions/email";
    static final String SLACK_PATH = "/api/v1/subscriptions/slack";
    static final String WEBHOOK = "https://hooks.slack.com/services/" + "T0TESTTEAM/B0TESTHOOK/"
            + "abcdEFGH1234" + "ijklMNOP5678";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    SubscriptionService service;

    ResultActions postJson(String path, String json) throws Exception {
        return mvc.perform(post(path).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    static String quoted(String value) {
        return "\"" + value + "\"";
    }

    void expectValidationProblem(ResultActions result) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:alerting:problem:validation"));
    }

    @Nested
    class Email {

        @BeforeEach
        void enabled() {
            given(service.offers("email")).willReturn(true);
        }

        @Test
        void acceptedAnswers202WithTheGenericMessageAndPassesTheInputOn() throws Exception {
            given(service.subscribe(eq("email"), any())).willReturn(SubscriptionResult.accepted());

            postJson(EMAIL_PATH, """
                    {"name": "Alice", "email": "alice@example.com"}""")
                    .andExpect(status().isAccepted())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(content().json("""
                            {"message": "Thank you. Your subscription has been received."}""", JsonCompareMode.STRICT));

            verify(service).subscribe("email", new SubscriberInput("Alice", "alice@example.com"));
        }

        @Test
        void subscriberTypeErrorsAreReportedWithTheJsonFieldNames() throws Exception {
            given(service.subscribe(eq("email"), any())).willReturn(SubscriptionResult.invalid(List.of(
                    new FieldError(SubscriberInput.FIELD_DISPLAY_NAME, "invalid-characters", "No line breaks."),
                    new FieldError(SubscriberInput.FIELD_ADDRESS, "invalid-format", "Enter a valid email address."))));

            MvcResult result = postJson(EMAIL_PATH, """
                    {"name": "Al\\tice", "email": "not-an-email-4711"}""")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("urn:alerting:problem:validation"))
                    .andExpect(jsonPath("$.errors", hasSize(2)))
                    .andExpect(jsonPath("$.errors[0].field").value("name"))
                    .andExpect(jsonPath("$.errors[0].message").value("No line breaks."))
                    .andExpect(jsonPath("$.errors[1].field").value("email"))
                    .andExpect(jsonPath("$.errors[1].message").value("Enter a valid email address."))
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).doesNotContain("not-an-email-4711");
        }

        @Test
        void missingFieldsFailBeanValidationWithoutCallingTheService() throws Exception {
            expectValidationProblem(postJson(EMAIL_PATH, "{}"));
            postJson(EMAIL_PATH, "{}")
                    .andExpect(jsonPath("$.errors", hasSize(2)))
                    .andExpect(jsonPath("$.errors[0].field").value("email"))
                    .andExpect(jsonPath("$.errors[1].field").value("name"));

            verify(service, never()).subscribe(anyString(), any());
        }

        @Test
        void blankNameFailsBeanValidation() throws Exception {
            expectValidationProblem(postJson(EMAIL_PATH, """
                    {"name": "  ", "email": "alice@example.com"}"""));
            postJson(EMAIL_PATH, """
                    {"name": "  ", "email": "alice@example.com"}""")
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].field").value("name"));
        }

        @Test
        void lengthLimitsAreEnforced() throws Exception {
            String longestName = "n".repeat(EmailSubscriptionRequest.NAME_MAX_LENGTH);
            // Local part 64, domain labels of at most 63 characters: 64 + 1 + 189 = 254.
            String longestEmail = "a".repeat(64) + "@" + "d".repeat(60) + "." + "d".repeat(60) + "."
                    + "d".repeat(EmailSubscriptionRequest.EMAIL_MAX_LENGTH - 192) + ".test";
            assertThat(longestEmail).hasSize(EmailSubscriptionRequest.EMAIL_MAX_LENGTH);
            given(service.subscribe(eq("email"), any())).willReturn(SubscriptionResult.accepted());

            postJson(EMAIL_PATH, "{\"name\": " + quoted(longestName) + ", \"email\": " + quoted(longestEmail) + "}")
                    .andExpect(status().isAccepted());

            postJson(EMAIL_PATH, "{\"name\": " + quoted(longestName + "n") + ", \"email\": "
                    + quoted("x" + longestEmail) + "}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasSize(2)))
                    .andExpect(jsonPath("$.errors[0].field").value("email"))
                    .andExpect(jsonPath("$.errors[0].message").value("size must be between 0 and 254"))
                    .andExpect(jsonPath("$.errors[1].field").value("name"))
                    .andExpect(jsonPath("$.errors[1].message").value("size must be between 0 and 100"));
        }

        @Test
        void unknownTypeResultIsNotFound() throws Exception {
            given(service.subscribe(eq("email"), any())).willReturn(SubscriptionResult.unknownType());

            postJson(EMAIL_PATH, """
                    {"name": "Alice", "email": "alice@example.com"}""")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("urn:alerting:problem:not-found"));
        }
    }

    @Nested
    class Slack {

        @BeforeEach
        void enabled() {
            given(service.offers("slack")).willReturn(true);
        }

        @Test
        void acceptedWithoutLabelPassesANullDisplayName() throws Exception {
            given(service.subscribe(eq("slack"), any())).willReturn(SubscriptionResult.accepted());

            postJson(SLACK_PATH, "{\"webhookUrl\": " + quoted(WEBHOOK) + "}")
                    .andExpect(status().isAccepted())
                    .andExpect(content().json("""
                            {"message": "Thank you. Your subscription has been received."}""", JsonCompareMode.STRICT));

            verify(service).subscribe("slack", new SubscriberInput(null, WEBHOOK));
        }

        @Test
        void labelIsPassedAsDisplayName() throws Exception {
            given(service.subscribe(eq("slack"), any())).willReturn(SubscriptionResult.accepted());

            postJson(SLACK_PATH, "{\"webhookUrl\": " + quoted(WEBHOOK) + ", \"label\": \"#alerts\"}")
                    .andExpect(status().isAccepted());

            verify(service).subscribe("slack", new SubscriberInput("#alerts", WEBHOOK));
        }

        @Test
        void subscriberTypeErrorsAreReportedWithTheJsonFieldNamesWithoutEchoingTheUrl() throws Exception {
            given(service.subscribe(eq("slack"), any())).willReturn(SubscriptionResult.invalid(List.of(
                    new FieldError(SubscriberInput.FIELD_DISPLAY_NAME, "invalid-format", "No control characters."),
                    new FieldError(SubscriberInput.FIELD_ADDRESS, "invalid-format", "Enter a Slack webhook URL."))));
            String notSlack = "https://evil.example/services/secret-4711";

            MvcResult result = postJson(SLACK_PATH, "{\"webhookUrl\": " + quoted(notSlack)
                    + ", \"label\": \"a\\u0007b\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("urn:alerting:problem:validation"))
                    .andExpect(jsonPath("$.errors[0].field").value("label"))
                    .andExpect(jsonPath("$.errors[1].field").value("webhookUrl"))
                    .andExpect(jsonPath("$.errors[1].message").value("Enter a Slack webhook URL."))
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).doesNotContain(notSlack, "secret-4711");
        }

        @Test
        void missingUrlAndTooLongInputFailBeanValidation() throws Exception {
            postJson(SLACK_PATH, "{\"label\": " + quoted("l".repeat(SlackSubscriptionRequest.LABEL_MAX_LENGTH + 1))
                    + "}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasSize(2)))
                    .andExpect(jsonPath("$.errors[0].field").value("label"))
                    .andExpect(jsonPath("$.errors[0].message").value("size must be between 0 and 100"))
                    .andExpect(jsonPath("$.errors[1].field").value("webhookUrl"))
                    .andExpect(jsonPath("$.errors[1].message").value("must not be blank"));

            String tooLong = WEBHOOK + "x".repeat(SlackSubscriptionRequest.WEBHOOK_URL_MAX_LENGTH - WEBHOOK.length() + 1);
            MvcResult result = postJson(SLACK_PATH, "{\"webhookUrl\": " + quoted(tooLong) + "}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].field").value("webhookUrl"))
                    .andExpect(jsonPath("$.errors[0].message").value("size must be between 0 and 256"))
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).doesNotContain("ijklMNOP5678");
            verify(service, never()).subscribe(anyString(), any());
        }

        @Test
        void labelAtItsLimitIsAccepted() throws Exception {
            given(service.subscribe(eq("slack"), any())).willReturn(SubscriptionResult.accepted());

            postJson(SLACK_PATH, "{\"webhookUrl\": " + quoted(WEBHOOK) + ", \"label\": "
                    + quoted("l".repeat(SlackSubscriptionRequest.LABEL_MAX_LENGTH)) + "}")
                    .andExpect(status().isAccepted());
        }

        @Test
        void failedVerificationIs422WebhookNotVerified() throws Exception {
            given(service.subscribe(eq("slack"), any())).willReturn(SubscriptionResult.notVerified());

            MvcResult result = postJson(SLACK_PATH, "{\"webhookUrl\": " + quoted(WEBHOOK) + "}")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.type").value("urn:alerting:problem:webhook-not-verified"))
                    .andExpect(jsonPath("$.status").value(422))
                    .andReturn();

            assertThat(result.getResponse().getContentAsString()).doesNotContain("ijklMNOP5678");
        }
    }

    @Nested
    class Disabled {

        @Test
        void disabledPluginsAnswer404NotFoundWhateverTheBody() throws Exception {
            given(service.offers(anyString())).willReturn(false);

            for (String path : List.of(EMAIL_PATH, SLACK_PATH)) {
                for (String body : List.of("{\"name\": \"Alice\", \"email\": \"alice@example.com\"}",
                        "{\"webhookUrl\": " + quoted(WEBHOOK) + "}", "{}")) {
                    postJson(path, body)
                            .andExpect(status().isNotFound())
                            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                            .andExpect(jsonPath("$.type").value("urn:alerting:problem:not-found"))
                            .andExpect(jsonPath("$.title").value("Not found"));
                }
            }
            verify(service, never()).subscribe(anyString(), any());
        }
    }

    @Nested
    class Protocol {

        @Test
        void withoutCsrfTokenTheAnswerIs403AndTheServiceIsNotCalled() throws Exception {
            for (String path : List.of(EMAIL_PATH, SLACK_PATH)) {
                mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\": \"Alice\", \"email\": \"alice@example.com\"}"))
                        .andExpect(status().isForbidden())
                        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                        .andExpect(jsonPath("$.type").value("urn:alerting:problem:csrf-token-invalid"));
                mvc.perform(post(path).with(csrf().useInvalidToken()).contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.type").value("urn:alerting:problem:csrf-token-invalid"));
            }
            verify(service, never()).offers(anyString());
        }

        @Test
        void bodyThatIsNotJsonIsABadRequest() throws Exception {
            given(service.offers(anyString())).willReturn(true);

            postJson(EMAIL_PATH, "{\"name\": ")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("urn:alerting:problem:bad-request"));
        }

        @Test
        void onlyJsonIsAccepted() throws Exception {
            mvc.perform(post(EMAIL_PATH).with(csrf()).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .content("name=Alice&email=alice%40example.com"))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.type").value("urn:alerting:problem:unsupported-media-type"));
        }
    }
}
