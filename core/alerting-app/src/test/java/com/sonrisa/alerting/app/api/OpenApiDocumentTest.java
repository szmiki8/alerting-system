package com.sonrisa.alerting.app.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The contract is published in every profile; Swagger UI only in local and demo (ADR-13, BE-12). */
class OpenApiDocumentTest {

    static final String SWAGGER_UI = "/swagger-ui/index.html";

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles("test")
    @Import(DefaultProfile.DocumentedTestController.class)
    class DefaultProfile {

        @Autowired
        MockMvc mvc;

        @Test
        void publishesTheContractWithTheProblemTypes() throws Exception {
            String document = mvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(document).contains("\"openapi\":\"3.1", "\"title\":\"Alerting System API\"",
                    "\"Problem\":", "\"FieldProblem\":", "/api/v1/test/documented",
                    "\"4XX\":{\"description\":\"Client error (see the problem types)\","
                            + "\"content\":{\"application/problem+json\":{\"schema\":"
                            + "{\"$ref\":\"#/components/schemas/Problem\"}}}}");
            for (ProblemType type : ProblemType.values()) {
                assertThat(document).contains(type.uri().toString());
            }
        }

        @Test
        void hidesSwaggerUi() throws Exception {
            mvc.perform(get(SWAGGER_UI)).andExpect(status().isNotFound());
        }

        @RestController
        static class DocumentedTestController {

            @GetMapping("/api/v1/test/documented")
            String documented() {
                return "ok";
            }
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles({"local", "test"})
    class LocalProfile {

        @Autowired
        MockMvc mvc;

        @Test
        void servesSwaggerUi() throws Exception {
            mvc.perform(get(SWAGGER_UI)).andExpect(status().isOk());
        }
    }

    /**
     * Starting the demo profile in this JVM would switch the console to JSON logs for later tests (a
     * system property that outlives the context), so the demo setting is checked in its YAML file.
     */
    @Test
    void demoProfileEnablesSwaggerUi() throws Exception {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("demo", new ClassPathResource("application-demo.yaml"));

        assertThat(sources).anySatisfy(source ->
                assertThat(source.getProperty("springdoc.swagger-ui.enabled")).hasToString("true"));
    }
}
