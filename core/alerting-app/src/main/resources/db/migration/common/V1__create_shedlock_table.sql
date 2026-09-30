-- Lock table for ShedLock's JDBC provider (ADR-05): one row per scheduled job name. Used from the
-- scheduling task; ShedLock never creates it. DDL as in the ShedLock README (JdbcTemplateLockProvider).
CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    CONSTRAINT pk_shedlock PRIMARY KEY (name)
);
