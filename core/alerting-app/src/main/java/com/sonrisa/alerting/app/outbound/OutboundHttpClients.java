package com.sonrisa.alerting.app.outbound;

import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * The one way to build outbound HTTP clients (BE-17, architecture Section 13.3): a {@link RestClient}
 * builder with the connect and read timeouts of the calling integration. It starts from Spring Boot's
 * {@code RestClient.Builder} (message converters, observations and therefore HTTP client metrics) and the
 * global {@code spring.http.clients.*} settings (redirects, SSL), and replaces the timeouts.
 *
 * <p>A client without explicit timeouts cannot be built here, and an architecture rule forbids the static
 * factories ({@code RestClient.create()}, {@code RestClient.builder()}, {@code new RestTemplate()}, the
 * JDK {@code HttpClient} factories) that would bypass this class.
 */
@Component
public class OutboundHttpClients {

    private final ObjectProvider<RestClient.Builder> restClientBuilders;
    private final ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder;
    private final HttpClientSettings globalSettings;

    public OutboundHttpClients(ObjectProvider<RestClient.Builder> restClientBuilders,
            ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder, HttpClientSettings globalSettings) {
        this.restClientBuilders = restClientBuilders;
        this.requestFactoryBuilder = requestFactoryBuilder;
        this.globalSettings = globalSettings;
    }

    /**
     * A new builder for the named client with the given timeouts. Callers add the base URL, default
     * headers and so on.
     *
     * @param clientName name of the integration, used in error messages (for example {@code newsapi})
     * @throws IllegalArgumentException if a timeout is missing, zero or negative
     */
    public RestClient.Builder builder(String clientName, HttpClientTimeouts timeouts) {
        if (timeouts == null) {
            throw new IllegalArgumentException("HTTP client '" + clientName + "' has no timeouts configured");
        }
        requirePositive(clientName, "connect", timeouts.connectTimeout());
        requirePositive(clientName, "read", timeouts.readTimeout());
        HttpClientSettings settings = globalSettings.withTimeouts(timeouts.connectTimeout(), timeouts.readTimeout());
        // Spring Boot's RestClient.Builder bean is prototype-scoped: every call gets its own builder.
        return restClientBuilders.getObject().requestFactory(requestFactoryBuilder.build(settings));
    }

    private static void requirePositive(String clientName, String kind, Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException(
                    "HTTP client '" + clientName + "' needs a positive " + kind + " timeout, got " + timeout);
        }
    }
}
