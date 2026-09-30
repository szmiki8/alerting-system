package com.sonrisa.alerting.app.persistence.audit;

/** Audited admin actions (FR-30). A new value also needs a migration that extends the check constraint. */
public enum AuditAction {
    SUBSCRIBER_DELETED
}
