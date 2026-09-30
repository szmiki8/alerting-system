package com.sonrisa.alerting.app.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

class StandardFailureClassifierTest {

    static final Instant NOW = Instant.parse("2026-07-15T10:00:00Z");

    final StandardFailureClassifier classifier = new StandardFailureClassifier(Clock.fixed(NOW, ZoneOffset.UTC));

    static HttpClientErrorException tooManyRequests(String retryAfter) {
        HttpHeaders headers = new HttpHeaders();
        if (retryAfter != null) {
            headers.set(HttpHeaders.RETRY_AFTER, retryAfter);
        }
        return HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", headers,
                new byte[0], StandardCharsets.UTF_8);
    }

    @Test
    void tooManyRequestsWithSecondsIsTransientWithWait() {
        assertThat(classifier.classify(tooManyRequests("7")))
                .contains(Classification.transientFailure(Duration.ofSeconds(7)));
    }

    @Test
    void tooManyRequestsWithHttpDateIsTransientWithWaitUntilThatTime() {
        assertThat(classifier.classify(tooManyRequests("Wed, 15 Jul 2026 10:00:30 GMT")))
                .contains(Classification.transientFailure(Duration.ofSeconds(30)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"soon", "Wed, 15 Jul 2026 09:00:00 GMT"})
    void tooManyRequestsWithUnusableOrPastRetryAfterIsTransientWithoutWait(String retryAfter) {
        assertThat(classifier.classify(tooManyRequests(retryAfter))).contains(Classification.transientFailure());
    }

    @Test
    void tooManyRequestsWithoutRetryAfterIsTransient() {
        assertThat(classifier.classify(tooManyRequests(null))).contains(Classification.transientFailure());
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 503, 504})
    void serverErrorsAreTransient(int status) {
        var exception = HttpServerErrorException.create(HttpStatus.valueOf(status), "error", HttpHeaders.EMPTY,
                new byte[0], StandardCharsets.UTF_8);

        assertThat(classifier.classify(exception)).contains(Classification.transientFailure());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 410})
    void otherClientErrorsArePermanent(int status) {
        var exception = HttpClientErrorException.create(HttpStatus.valueOf(status), "error", HttpHeaders.EMPTY,
                new byte[0], StandardCharsets.UTF_8);

        assertThat(classifier.classify(exception)).contains(Classification.permanentFailure());
    }

    @Test
    void timeoutsAndIoErrorsAreTransient() {
        assertThat(classifier.classify(new ResourceAccessException("I/O error", new SocketTimeoutException())))
                .contains(Classification.transientFailure());
        assertThat(classifier.classify(new IOException("connection reset")))
                .contains(Classification.transientFailure());
    }

    @Test
    void wrappedExceptionsAreRecognised() {
        assertThat(classifier.classify(new IllegalStateException("wrapper", tooManyRequests("3"))))
                .contains(Classification.transientFailure(Duration.ofSeconds(3)));
    }

    @Test
    void classifiedOutcomesClassifyThemselves() {
        Classified result = () -> Classification.transientFailure(Duration.ofSeconds(2));

        assertThat(classifier.classify(result)).contains(Classification.transientFailure(Duration.ofSeconds(2)));
    }

    @Test
    void unknownExceptionsAndPlainValuesAreNotClassified() {
        assertThat(classifier.classify(new IllegalArgumentException("bug"))).isEmpty();
        assertThat(classifier.classify("ok")).isEmpty();
        assertThat(classifier.classify(null)).isEmpty();
    }
}
