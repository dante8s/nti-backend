package com.nti.nti_backend.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditRepository
        extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(
            String entityType, Long entityId
    );
}