package com.sonrisa.alerting.app.resilience;

import com.sonrisa.alerting.spi.channel.DeliveryResult;
import com.sonrisa.alerting.spi.source.EventSourceException;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Connects the SPI result types (BE-10) to the retry rules (BE-17): a source's
 * {@link EventSourceException} and a channel's {@link DeliveryResult} say themselves whether a failure is
 * transient or permanent, so they are asked before the generic {@link StandardFailureClassifier}.
 *
 * <ul>
 *   <li>{@link EventSourceException}: {@code TRANSIENT} is retried, {@code PERMANENT} is not;</li>
 *   <li>{@link DeliveryResult}: a transient failure is retried, waiting at least its retry-after time;
 *       a permanent failure is not; a delivered result is a success.</li>
 * </ul>
 */
@Component
@Order(0)
public class SpiFailureClassifier implements FailureClassifier {

    @Override
    public Optional<Classification> classify(Object outcome) {
        if (outcome instanceof EventSourceException exception) {
            return Optional.of(exception.isTransient()
                    ? Classification.transientFailure()
                    : Classification.permanentFailure());
        }
        if (outcome instanceof DeliveryResult result) {
            return switch (result.outcome()) {
                case DELIVERED -> Optional.empty();
                case TRANSIENT_FAILURE -> Optional.of(result.retryAfter() != null
                        ? Classification.transientFailure(result.retryAfter())
                        : Classification.transientFailure());
                case PERMANENT_FAILURE -> Optional.of(Classification.permanentFailure());
            };
        }
        return Optional.empty();
    }
}
