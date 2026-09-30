package com.sonrisa.alerting.app.persistence.run;

/** State of a run (Section 8.1). */
public enum RunStatus {
    RUNNING,
    /** All sources and deliveries succeeded. */
    COMPLETED,
    /** Some sources or deliveries failed. */
    PARTIAL,
    FAILED,
    /** Not executed, for example because another run held the lock. */
    SKIPPED
}
