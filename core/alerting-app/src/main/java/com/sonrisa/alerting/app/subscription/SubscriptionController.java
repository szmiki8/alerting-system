package com.sonrisa.alerting.app.subscription;

import com.sonrisa.alerting.app.api.FieldProblem;
import com.sonrisa.alerting.app.api.ProblemException;
import com.sonrisa.alerting.app.api.ProblemType;
import com.sonrisa.alerting.spi.subscriber.SubscriberInput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The public sign-up endpoints (BE-16, BE-19; architecture Sections 9.1, 9.2, 10.1, 10.2). Each maps its JSON body
 * to the generic {@link SubscriberInput} of its subscriber type, lets the {@link SubscriptionService} do the work,
 * and maps the {@link SubscriptionResult} to HTTP:
 *
 * <ul>
 *   <li>{@code ACCEPTED}: 202 with the same generic body for new and existing addresses (FR-05);</li>
 *   <li>{@code INVALID}: 400 {@code validation}, with the subscriber type's field names translated back to the
 *       JSON names of the request ({@code displayName} to {@code name}/{@code label}, {@code address} to
 *       {@code email}/{@code webhookUrl});</li>
 *   <li>{@code NOT_VERIFIED}: 422 {@code webhook-not-verified} (Slack welcome message failed, FR-04);</li>
 *   <li>plugin disabled: 404 {@code not-found}, see below.</li>
 * </ul>
 *
 * <p><b>Disabled plugin.</b> When the email or Slack plugin is switched off ({@code alerting.channels.<key>.enabled},
 * OP-22), the endpoint answers 404 {@code not-found}, the same answer as for any resource that this deployment
 * does not offer. 404 rather than 503 because the state is a deliberate, lasting configuration, not a temporary
 * outage: 503 would invite retries, count as a server error in monitoring, and the UI would report a server
 * failure. The endpoint stays in the OpenAPI document in every configuration, so the contract does not depend on
 * the enabled plugins.
 *
 * <p><b>Order of checks.</b> The plugin check comes first, so a disabled endpoint answers 404 whatever the body
 * holds. That is why the request is validated in the method (with the same Bean Validation annotations and the
 * same 400 problem as {@code @Valid}) instead of by {@code @Valid}, which would run before the method. Only a body
 * that is not JSON at all is refused earlier, with 400 {@code bad-request}.
 *
 * <p><b>Personal data.</b> Nothing here logs; the request records hide their values in {@code toString()}, and
 * error bodies carry fixed or subscriber-type messages that never echo the input.
 */
@RestController
@RequestMapping(SubscriptionController.BASE_PATH)
@Tag(name = "Subscriptions", description = "Public sign-up for notifications")
class SubscriptionController {

    static final String BASE_PATH = "/api/v1/subscriptions";
    static final String EMAIL_TYPE = "email";
    static final String SLACK_TYPE = "slack";

    private static final Map<String, String> EMAIL_FIELDS = Map.of(
            SubscriberInput.FIELD_DISPLAY_NAME, "name",
            SubscriberInput.FIELD_ADDRESS, "email");
    private static final Map<String, String> SLACK_FIELDS = Map.of(
            SubscriberInput.FIELD_DISPLAY_NAME, "label",
            SubscriberInput.FIELD_ADDRESS, "webhookUrl");

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String PROBLEM_REF = "#/components/schemas/Problem";

    private final SubscriptionService service;
    private final Validator validator;

    SubscriptionController(SubscriptionService service, Validator validator) {
        this.service = service;
        this.validator = validator;
    }

    @PostMapping(path = "/email", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Subscribe an email address",
            description = "Subscribes the address to email notifications. The answer is the same for a new and an"
                    + " already subscribed address. Requires the CSRF token (X-XSRF-TOKEN header).")
    @ApiResponse(responseCode = "202", description = "Accepted (new or already subscribed address)")
    @ApiResponse(responseCode = "400", description = "`validation`: invalid fields (`name`, `email`) in `errors`;"
            + " `bad-request`: the body is not valid JSON",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_REF)))
    @ApiResponse(responseCode = "403", description = "`csrf-token-invalid`: CSRF token missing or wrong",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_REF)))
    @ApiResponse(responseCode = "404", description = "`not-found`: email sign-up is switched off in this deployment",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_REF)))
    SubscriptionAcceptedResponse subscribeEmail(@RequestBody EmailSubscriptionRequest request) {
        requireOffered(EMAIL_TYPE);
        validate(request);
        SubscriptionResult result = service.subscribe(EMAIL_TYPE, new SubscriberInput(request.name(), request.email()));
        return answer(result, EMAIL_FIELDS);
    }

    @PostMapping(path = "/slack", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Subscribe a Slack channel",
            description = "Checks the webhook URL, posts a short welcome message to it (for a new webhook only) and"
                    + " stores it encrypted. The answer is the same for a new and an already subscribed webhook."
                    + " Requires the CSRF token (X-XSRF-TOKEN header).")
    @ApiResponse(responseCode = "202", description = "Accepted (new or already subscribed webhook)")
    @ApiResponse(responseCode = "400", description = "`validation`: invalid fields (`webhookUrl`, `label`) in"
            + " `errors`; `bad-request`: the body is not valid JSON",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_REF)))
    @ApiResponse(responseCode = "403", description = "`csrf-token-invalid`: CSRF token missing or wrong",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_REF)))
    @ApiResponse(responseCode = "404", description = "`not-found`: Slack sign-up is switched off in this deployment",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_REF)))
    @ApiResponse(responseCode = "422", description = "`webhook-not-verified`: the welcome message could not be"
            + " delivered; nothing was stored",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(ref = PROBLEM_REF)))
    SubscriptionAcceptedResponse subscribeSlack(@RequestBody SlackSubscriptionRequest request) {
        requireOffered(SLACK_TYPE);
        validate(request);
        SubscriptionResult result = service.subscribe(SLACK_TYPE,
                new SubscriberInput(request.label(), request.webhookUrl()));
        return answer(result, SLACK_FIELDS);
    }

    private void requireOffered(String typeKey) {
        if (!service.offers(typeKey)) {
            throw new ProblemException(ProblemType.NOT_FOUND);
        }
    }

    /** Bean Validation of the request; one entry per violation, sorted by field for a stable answer. */
    private void validate(Object request) {
        List<FieldProblem> errors = validator.validate(request).stream()
                .map(violation -> new FieldProblem(fieldName(violation), violation.getMessage()))
                .sorted(Comparator.comparing(FieldProblem::field).thenComparing(FieldProblem::message))
                .toList();
        if (!errors.isEmpty()) {
            throw ProblemException.validation(errors);
        }
    }

    private static String fieldName(ConstraintViolation<?> violation) {
        return violation.getPropertyPath().toString();
    }

    private static SubscriptionAcceptedResponse answer(SubscriptionResult result, Map<String, String> fields) {
        return switch (result.status()) {
            case ACCEPTED -> SubscriptionAcceptedResponse.INSTANCE;
            case INVALID -> throw ProblemException.validation(result.fieldErrors().stream()
                    .map(error -> new FieldProblem(fields.getOrDefault(error.field(), error.field()), error.message()))
                    .toList());
            // Only the Slack type verifies addresses (welcome message); the email type never fails verification.
            case NOT_VERIFIED -> throw new ProblemException(ProblemType.WEBHOOK_NOT_VERIFIED);
            // Only if the plugin disappeared between the check and the call; the registry is fixed at start-up.
            case UNKNOWN_TYPE -> throw new ProblemException(ProblemType.NOT_FOUND);
        };
    }
}
