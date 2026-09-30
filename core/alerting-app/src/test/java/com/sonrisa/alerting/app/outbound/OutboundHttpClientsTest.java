package com.sonrisa.alerting.app.outbound;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.sonrisa.alerting.app.resilience.Classification;
import com.sonrisa.alerting.app.resilience.Retries;
import io.github.resilience4j.retry.Retry;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/** Outbound clients against a WireMock server: mandatory timeouts, and retries on real HTTP errors (BE-17). */
@SpringBootTest(properties = {
        "resilience4j.retry.configs.fast.base-config=default",
        "resilience4j.retry.configs.fast.wait-duration=10ms"})
@ActiveProfiles("test")
class OutboundHttpClientsTest {

    static final HttpClientTimeouts SHORT = new HttpClientTimeouts(Duration.ofSeconds(1), Duration.ofMillis(300));

    @RegisterExtension
    static WireMockExtension server = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    @Autowired
    OutboundHttpClients clients;

    @Autowired
    Retries retries;

    RestClient client() {
        return clients.builder("fake", SHORT).baseUrl(server.baseUrl()).build();
    }

    @Test
    void refusesToBuildAClientWithoutTimeouts() {
        assertThatThrownBy(() -> clients.builder("fake", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("'fake' has no timeouts");
        assertThatThrownBy(() -> clients.builder("fake", new HttpClientTimeouts(null, Duration.ofSeconds(1))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("connect timeout");
        assertThatThrownBy(() -> clients.builder("fake", new HttpClientTimeouts(Duration.ofSeconds(1), Duration.ZERO)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("read timeout");
    }

    @Test
    void readTimeoutAppliesAndIsATransientFailure() {
        server.stubFor(get("/slow").willReturn(ok("late").withFixedDelay(3_000)));
        long start = System.nanoTime();

        Throwable failure = catchThrowable(
                () -> client().get().uri("/slow").retrieve().body(String.class));

        assertThat(failure).isInstanceOf(ResourceAccessException.class);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(2));
        assertThat(retries.classify(failure)).contains(Classification.transientFailure());
    }

    @Test
    void serverErrorsAreRetriedUntilTheServiceRecovers() {
        server.stubFor(get("/flaky").inScenario("flaky").whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(503)).willSetStateTo("second"));
        server.stubFor(get("/flaky").inScenario("flaky").whenScenarioStateIs("second")
                .willReturn(aResponse().withStatus(502)).willSetStateTo("up"));
        server.stubFor(get("/flaky").inScenario("flaky").whenScenarioStateIs("up").willReturn(ok("fine")));
        Retry retry = retries.retry("flaky-http", "fast");

        String body = retry.executeSupplier(() -> client().get().uri("/flaky").retrieve().body(String.class));

        assertThat(body).isEqualTo("fine");
        server.verify(3, getRequestedFor(urlEqualTo("/flaky")));
    }

    @Test
    void tooManyRequestsWaitsForRetryAfter() {
        server.stubFor(get("/limited").inScenario("limited").whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "1")).willSetStateTo("open"));
        server.stubFor(get("/limited").inScenario("limited").whenScenarioStateIs("open").willReturn(ok("done")));
        Retry retry = retries.retry("limited-http", "fast");
        long start = System.nanoTime();

        String body = retry.executeSupplier(() -> client().get().uri("/limited").retrieve().body(String.class));

        assertThat(body).isEqualTo("done");
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThanOrEqualTo(Duration.ofSeconds(1));
    }

    @Test
    void clientErrorsAreNotRetried() {
        server.stubFor(get("/gone").willReturn(aResponse().withStatus(410)));
        Retry retry = retries.retry("gone-http", "fast");

        assertThatThrownBy(() -> retry.executeSupplier(() -> client().get().uri("/gone").retrieve().body(String.class)))
                .hasMessageContaining("410");
        server.verify(1, getRequestedFor(urlEqualTo("/gone")));
    }
}
