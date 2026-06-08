package com.nti.nti_backend.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditRepository
        extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(
            String entityType, Long entityId
    );

    List<AuditEvent> findAllByOrderByCreatedAtDesc();

    @Query("""
        SELECT e FROM AuditEvent e
        WHERE (:entityType IS NULL OR e.entityType = :entityType)
          AND (:action     IS NULL OR e.action     = :action)
        ORDER BY e.createdAt DESC
        """)
    List<AuditEvent> findFiltered(
            @Param("entityType") String entityType,
            @Param("action")     String action
    );
}