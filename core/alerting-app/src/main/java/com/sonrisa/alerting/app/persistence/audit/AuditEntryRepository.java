package com.sonrisa.alerting.app.persistence.audit;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Audit entries (FR-30). Retention deletes them by {@code performedAt} (Section 8.4). */
public interface AuditEntryRepository extends JpaRepository<AuditEntry, UUID> {
}
