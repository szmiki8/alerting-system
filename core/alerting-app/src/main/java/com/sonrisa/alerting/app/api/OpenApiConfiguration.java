package com.sonrisa.alerting.app.api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

/**
 * The OpenAPI document at {@code /v3/api-docs} (ADR-13, code-first with springdoc). The operations come
 * from the controllers; this class adds the API description and the error contract: the
 * {@code Problem} and {@code FieldProblem} schemas and the list of {@link ProblemType problem types}.
 * Swagger UI is switched on only in the {@code local} and {@code demo} profiles.
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

    static final String PROBLEM_SCHEMA = "Problem";
    static final String FIELD_PROBLEM_SCHEMA = "FieldProblem";

    @Bean
    OpenAPI alertingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Alerting System API")
                        .version("v1")
                        .description(description()));
    }

    /**
     * Adds the {@code Problem} and {@code FieldProblem} schemas, and documents the error responses of every
     * operation as {@code Problem} ({@code application/problem+json}). Done in a customizer because
     * springdoc computes the components itself and drops schemas that no operation references.
     */
    @Bean
    OpenApiCustomizer problemResponses() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            openApi.getComponents()
                    .addSchemas(PROBLEM_SCHEMA, problemSchema())
                    .addSchemas(FIELD_PROBLEM_SCHEMA, fieldProblemSchema());
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                ApiResponses responses = operation.getResponses() != null
                        ? operation.getResponses()
                        : new ApiResponses();
                responses.addApiResponse("4XX", problemResponse("Client error (see the problem types)"));
                responses.addApiResponse("5XX", problemResponse("Server error"));
                operation.setResponses(responses);
            }));
        };
    }

    private static ApiResponse problemResponse(String description) {
        var problemJson = new io.swagger.v3.oas.models.media.MediaType()
                .schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM_SCHEMA));
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(MediaType.APPLICATION_PROBLEM_JSON_VALUE, problemJson));
    }

    private static String description() {
        String types = Arrays.stream(ProblemType.values())
                .map(type -> "- `" + type.uri() + "` (" + type.status().value() + "): " + type.title())
                .collect(Collectors.joining("\n"));
        return """
                Public sign-up and admin API of the Alerting System.

                Errors are RFC 9457 Problem Details (`application/problem+json`, schema `Problem`). Clients \
                branch on `type`; validation problems list the invalid fields in `errors`. Time values are \
                ISO-8601 with the offset of the Central European zone (for example `2026-07-15T12:00:00+02:00`).

                Problem types:
                """ + types + "\n";
    }

    private static Schema<?> problemSchema() {
        List<Object> typeUris = Arrays.stream(ProblemType.values())
                .map(type -> (Object) type.uri().toString())
                .toList();
        return object("RFC 9457 Problem Details as used by this API", Map.of(
                "type", string("Stable problem type identifier; absent (about:blank) for statuses "
                        + "without a type of their own")._enum(typeUris),
                "title", string("Short, fixed summary of the problem type"),
                "status", typed("integer", "HTTP status code"),
                "detail", string("Fixed, human-readable explanation; never contains submitted values"),
                "instance", string("URI reference of this occurrence, if any"),
                Problems.ERRORS, typed("array", "Invalid fields; only in validation problems")
                        .items(new Schema<>().$ref("#/components/schemas/" + FIELD_PROBLEM_SCHEMA))));
    }

    private static Schema<?> fieldProblemSchema() {
        return object("One invalid field of a validation problem", Map.of(
                "field", string("Field name as sent by the client; null for object-level errors"),
                "message", string("Human-readable message; never contains the submitted value")));
    }

    private static Schema<?> object(String description, Map<String, Schema<?>> properties) {
        Schema<?> schema = typed("object", description);
        properties.forEach(schema::addProperty);
        return schema;
    }

    private static Schema<Object> string(String description) {
        return typed("string", description);
    }

    /** Sets the type for both OpenAPI 3.0 ({@code type}) and 3.1 ({@code types}) serialisation. */
    private static Schema<Object> typed(String type, String description) {
        Schema<Object> schema = new Schema<>();
        schema.setType(type);
        schema.setTypes(Set.of(type));
        schema.setDescription(description);
        return schema;
    }
}
