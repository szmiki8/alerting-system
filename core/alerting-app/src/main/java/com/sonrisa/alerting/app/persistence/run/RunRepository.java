package com.sonrisa.alerting.app.persistence.run;

import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

/** Runs (Section 8.1). The run history is paged with {@link #NEWEST_FIRST}. */
public interface RunRepository extends JpaRepository<Run, UUID> {

    /** Run history order: latest start first; the id makes the order stable across pages. */
    Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("startedAt"), Sort.Order.desc("id"));
}
