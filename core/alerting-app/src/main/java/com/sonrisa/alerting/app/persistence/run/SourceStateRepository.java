package com.sonrisa.alerting.app.persistence.run;

import org.springframework.data.jpa.repository.JpaRepository;

/** Runtime state per source key (Section 8.1). */
public interface SourceStateRepository extends JpaRepository<SourceState, String> {
}
