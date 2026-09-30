package com.sonrisa.alerting.app.persistence.run;

/**
 * Outcome of one source within a run. Section 8.1 names the column but not its values; these two are the
 * assumption of BE-08.
 */
public enum RunSourceResultStatus {
    COMPLETED,
    FAILED
}
