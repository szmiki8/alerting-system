package com.sonrisa.alerting.app.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The sign-up endpoints with the plugins switched off, as in every profile until the channels exist (OP-21, OP-22):
 * both answer 404 {@code not-found}, and the OpenAPI document still describes them with their responses and field
 * limits, so the contract does not depend on the enabled plugins (BE-16, BE-19, ADR-13).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubscriptionApiContractTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JsonMapper jsonMapper;

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/subscriptions/email", "/api/v1/subscriptions/slack"})
    void disabledPluginAnswers404NotFound(String path) throws Exception {
        mvc.perform(post(path).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Alice\", \"email\": \"alice@example.com\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:alerting:problem:not-found"))
                .andExpect(jsonPath("$.title").value("Not found"))
                .andExpect(jsonPath("$.detail").value("The requested resource does not exist."));
    }

    JsonNode apiDocs() throws Exception {
        return jsonMapper.readTree(mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    static List<String> names(JsonNode object) {
        List<String> names = new ArrayList<>();
        object.propertyNames().forEach(names::add);
        return names;
    }

    static List<String> texts(JsonNode array) {
        List<String> texts = new ArrayList<>();
        array.forEach(node -> texts.add(node.asString()));
        return texts;
    }

    @Test
    void openApiDocumentsTheEmailOperation() throws Exception {
        JsonNode docs = apiDocs();
        JsonNode operation = docs.at("/paths/~1api~1v1~1subscriptions~1email/post");

        assertThat(names(operation.get("responses"))).contains("202", "400", "403", "404");
        assertThat(operation.at("/requestBody/content/application~1json/schema/$ref").asString())
                .isEqualTo("#/components/schemas/EmailSubscriptionRequest");
        assertThat(operation.at("/responses/202/content/application~1json/schema/$ref").asString())
                .isEqualTo("#/components/schemas/SubscriptionAcceptedResponse");
        assertThat(operation.at("/responses/400/content/application~1problem+json/schema/$ref").asString())
                .isEqualTo("#/components/schemas/Problem");

        JsonNode request = docs.at("/components/schemas/EmailSubscriptionRequest");
        assertThat(texts(request.get("required"))).containsExactlyInAnyOrder("name", "email");
        assertThat(request.at("/properties/name/maxLength").asInt()).isEqualTo(100);
        assertThat(request.at("/properties/email/maxLength").asInt()).isEqualTo(254);
        assertThat(docs.at("/components/schemas/SubscriptionAcceptedResponse/properties/message").isObject())
                .isTrue();
    }

    @Test
    void openApiDocumentsTheSlackOperation() throws Exception {
        JsonNode docs = apiDocs();
        JsonNode operation = docs.at("/paths/~1api~1v1~1subscriptions~1slack/post");

        assertThat(names(operation.get("responses"))).contains("202", "400", "403", "404", "422");
        assertThat(operation.at("/requestBody/content/application~1json/schema/$ref").asString())
                .isEqualTo("#/components/schemas/SlackSubscriptionRequest");
        assertThat(operation.at("/responses/422/content/application~1problem+json/schema/$ref").asString())
                .isEqualTo("#/components/schemas/Problem");

        JsonNode request = docs.at("/components/schemas/SlackSubscriptionRequest");
        assertThat(texts(request.get("required"))).containsExactly("webhookUrl");
        assertThat(request.at("/properties/webhookUrl/maxLength").asInt()).isEqualTo(256);
        assertThat(request.at("/properties/label/maxLength").asInt()).isEqualTo(100);
    }
}
