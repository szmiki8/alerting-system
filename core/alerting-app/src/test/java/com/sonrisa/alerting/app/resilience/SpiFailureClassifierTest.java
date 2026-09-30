package com.sonrisa.alerting.app.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonrisa.alerting.spi.channel.DeliveryResult;
import com.sonrisa.alerting.spi.source.EventSourceException;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class SpiFailureClassifierTest {

    private final SpiFailureClassifier classifier = new SpiFailureClassifier();

    @Test
    void transientSourceFailureIsRetried() {
        assertThat(classifier.classify(EventSourceException.transientFailure("timeout", null)))
                .contains(Classification.transientFailure());
    }

    @Test
    void permanentSourceFailureIsNotRetried() {
        assertThat(classifier.classify(EventSourceException.permanentFailure("invalid API key", null)))
                .contains(Classification.permanentFailure());
    }

    @Test
    void deliveredResultIsSuccess() {
        assertThat(classifier.classify(DeliveryResult.delivered())).isEmpty();
    }

    @Test
    void transientDeliveryFailureKeepsRetryAfter() {
        assertThat(classifier.classify(DeliveryResult.transientFailure("rate limited", Duration.ofSeconds(3))))
                .contains(Classification.transientFailure(Duration.ofSeconds(3)));
        assertThat(classifier.classify(DeliveryResult.transientFailure("HTTP 503")))
                .contains(Classification.transientFailure());
    }

    @Test
    void permanentDeliveryFailureIsNotRetried() {
        assertThat(classifier.classify(DeliveryResult.permanentFailure("no_service")))
                .contains(Classification.permanentFailure());
    }

    @Test
    void otherOutcomesAreLeftToTheStandardClassifier() {
        assertThat(classifier.classify(new IOException("connection reset"))).isEmpty();
        assertThat(classifier.classify("some value")).isEmpty();
    }
}
