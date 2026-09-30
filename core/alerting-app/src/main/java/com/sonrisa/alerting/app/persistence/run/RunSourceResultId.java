package com.sonrisa.alerting.app.persistence.run;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.UUID;

/** Key of a {@link RunSourceResult}: one row per run and source. */
@Embeddable
public record RunSourceResultId(
        @Column(name = "run_id", nullable = false, updatable = false) UUID runId,
        @Column(name = "source_key", nullable = false, updatable = false, length = 64) String sourceKey) {

    public RunSourceResultId {
        Objects.requireNonNull(runId, "runId");
        Objects.requireNonNull(sourceKey, "sourceKey");
    }
}
