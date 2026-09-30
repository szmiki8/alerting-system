package com.sonrisa.alerting.app.persistence.run;

/** What started a run (Section 8.1). */
public enum RunTrigger {
    /** The collection cron (FR-11, FR-31). */
    SCHEDULED,
    /** An operator through the management endpoint. */
    MANUAL,
    /** Start-up after a missed run (OP-10). */
    CATCH_UP
}
