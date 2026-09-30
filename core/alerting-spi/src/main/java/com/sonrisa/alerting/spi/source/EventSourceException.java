package com.sonrisa.alerting.spi.source;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * A failed fetch, classified as transient or permanent (Section 7.1). The message is logged, so it must
 * not contain credentials (for example an API key in a URL) or personal data.
 */
public class EventSourceException extends Exception {

    private static final long serialVersionUID = 1L;

    /** How the application should react to a failed fetch. */
    public enum Kind {
        /** Temporary problem (time-out, connection error, HTTP 5xx, rate limit): retrying may help. */
        TRANSIENT,
        /** Retrying will not help until the configuration changes (invalid API key, bad filter values). */
        PERMANENT
    }

    private final Kind kind;

    public EventSourceException(Kind kind, String message) {
        this(kind, message, null);
    }

    public EventSourceException(Kind kind, String message, @Nullable Throwable cause) {
        super(Objects.requireNonNull(message, "message"), cause);
        this.kind = Objects.requireNonNull(kind, "kind");
    }

    public static EventSourceException transientFailure(String message, @Nullable Throwable cause) {
        return new EventSourceException(Kind.TRANSIENT, message, cause);
    }

    public static EventSourceException permanentFailure(String message, @Nullable Throwable cause) {
        return new EventSourceException(Kind.PERMANENT, message, cause);
    }

    public Kind kind() {
        return kind;
    }

    public boolean isTransient() {
        return kind == Kind.TRANSIENT;
    }
}
