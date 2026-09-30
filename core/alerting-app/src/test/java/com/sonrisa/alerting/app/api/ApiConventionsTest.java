package com.sonrisa.alerting.app.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * BE-12 acceptance criteria, proven with a test-only controller (there are no business endpoints yet).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ApiConventionsTest.ConventionsTestController.class)
class ApiConventionsTest {

    static final String BASE = "/api/v1/test/conventions";
    static final String SECRET_WEBHOOK = "https://hooks.example.org/services/T000/B000/secret-token-4711";
    static final String SUBMITTED_EMAIL = "not-an-email-4711";
    static final String INTERNAL_MESSAGE = "internal detail 4711";

    @Autowired
    MockMvc mvc;

    @Test
    void validationErrorsListEveryInvalidField() throws Exception {
        String body = """
                {"name": " ", "email": "%s", "webhookUrl": "%s"}""".formatted(SUBMITTED_EMAIL, SECRET_WEBHOOK);

        MvcResult result = mvc.perform(post(BASE + "/subscriptions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:alerting:problem:validation"))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors", hasSize(3)))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("name", "email", "webhookUrl")))
                .andExpect(jsonPath("$.errors[?(@.field == 'email')].message").value("must be a well-formed email address"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(SECRET_WEBHOOK, "secret-token-4711", SUBMITTED_EMAIL);
    }

    @Test
    void validRequestPasses() throws Exception {
        mvc.perform(post(BASE + "/subscriptions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ann", "email": "ann@example.org",
                                 "webhookUrl": "https://hooks.slack.com/services/T000/B000/abc"}"""))
                .andExpect(status().isAccepted());
    }

    @Test
    void unreadableBodyDoesNotEchoItsContent() throws Exception {
        MvcResult result = mvc.perform(post(BASE + "/subscriptions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"webhookUrl\": \"" + SECRET_WEBHOOK + "\", broken"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:alerting:problem:bad-request"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("secret-token-4711", "broken");
    }

    @Test
    void typeMismatchDoesNotEchoTheValue() throws Exception {
        // Only "instance" (the request path the client called) may contain it; title and detail are fixed.
        mvc.perform(get(BASE + "/items/secret-4711"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:alerting:problem:bad-request"))
                .andExpect(jsonPath("$.title").value("Bad request"))
                .andExpect(jsonPath("$.detail").value("The request is malformed."))
                .andExpect(jsonPath("$.instance").value(BASE + "/items/secret-4711"));
    }

    @Test
    void unknownRouteIsNotFoundProblem() throws Exception {
        mvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:alerting:problem:not-found"))
                .andExpect(jsonPath("$.detail").value("The requested resource does not exist."));
    }

    @Test
    void wrongMethodIsMethodNotAllowedProblem() throws Exception {
        mvc.perform(delete(BASE + "/subscriptions").with(csrf()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:alerting:problem:method-not-allowed"));
    }

    @Test
    void nonJsonBodyIsUnsupportedMediaTypeProblem() throws Exception {
        mvc.perform(post(BASE + "/subscriptions").with(csrf())
                        .contentType(MediaType.TEXT_PLAIN).content("name=Ann"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.type").value("urn:alerting:problem:unsupported-media-type"));
    }

    @Test
    void unexpectedErrorHidesInternals() throws Exception {
        MvcResult result = mvc.perform(get(BASE + "/failure"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:alerting:problem:internal-error"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(INTERNAL_MESSAGE, "IllegalStateException", "trace", "at com.");
    }

    @Test
    void applicationCodeCanRaiseATypedProblem() throws Exception {
        mvc.perform(get(BASE + "/webhook-failure"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:alerting:problem:webhook-not-verified"))
                .andExpect(jsonPath("$.title").value("Webhook not verified"));
    }

    @Test
    void winterTimeHasTheCetOffset() throws Exception {
        mvc.perform(get(BASE + "/time/2026-01-15T10:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.at").value("2026-01-15T11:00:00+01:00"));
    }

    @Test
    void summerTimeHasTheCestOffset() throws Exception {
        mvc.perform(get(BASE + "/time/2026-07-15T10:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.at").value("2026-07-15T12:00:00+02:00"));
    }

    @RestController
    @RequestMapping(BASE)
    static class ConventionsTestController {

        record SubscriptionRequest(
                @NotBlank String name,
                @NotBlank @Email String email,
                @NotBlank @Pattern(regexp = "https://hooks\\.slack\\.com/services/.+") String webhookUrl) {
        }

        record TimeResponse(Instant at) {
        }

        @PostMapping("/subscriptions")
        @ResponseStatus(HttpStatus.ACCEPTED)
        void subscribe(@Valid @RequestBody SubscriptionRequest request) {
        }

        @GetMapping("/items/{id}")
        String item(@PathVariable long id) {
            return "item " + id;
        }

        @GetMapping("/failure")
        String failure() {
            throw new IllegalStateException(INTERNAL_MESSAGE);
        }

        @GetMapping("/webhook-failure")
        String webhookFailure() {
            throw new ProblemException(ProblemType.WEBHOOK_NOT_VERIFIED, new IllegalStateException(INTERNAL_MESSAGE));
        }

        @GetMapping("/time/{instant}")
        TimeResponse time(@PathVariable String instant) {
            return new TimeResponse(Instant.parse(instant));
        }
    }
}
