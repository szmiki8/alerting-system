package com.sonrisa.alerting.app.persistence.run;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Per-source results of runs (Section 8.1). */
public interface RunSourceResultRepository extends JpaRepository<RunSourceResult, RunSourceResultId> {

    List<RunSourceResult> findByIdRunIdOrderByIdSourceKey(UUID runId);
}
