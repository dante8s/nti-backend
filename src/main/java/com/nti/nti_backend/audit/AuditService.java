package com.nti.nti_backend.audit;

import com.nti.nti_backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditRepository auditRepository;

    // Записати подію
    public void log(
            User actor,
            String action,
            String entityType,
            Long entityId,
            String description) {
        AuditEvent event = AuditEvent.builder()
                .actor(actor)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .build();
        auditRepository.save(event);
    }

    // Отримати всі події для конкретної заявки
    public List<AuditEventDTO> getForApplication(Long applicationId) {
        return auditRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtAsc("APPLICATION", applicationId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // Усі події з необов'язковими фільтрами (для адмін-журналу)
    public List<AuditEventDTO> getAll(String entityType, String action) {
        boolean noFilter = (entityType == null || entityType.isBlank())
                        && (action == null || action.isBlank());
        if (noFilter) {
            return auditRepository.findAllByOrderByCreatedAtDesc()
                    .stream().map(this::toDTO).toList();
        }
        return auditRepository.findFiltered(
                        entityType == null || entityType.isBlank() ? null : entityType,
                        action     == null || action.isBlank()     ? null : action)
                .stream().map(this::toDTO).toList();
    }

    private AuditEventDTO toDTO(AuditEvent e) {
        return new AuditEventDTO(
                e.getId(),
                e.getActor() != null
                        ? e.getActor().getName() : "Система",
                e.getAction(),
                e.getDescription(),
                e.getCreatedAt()
        );
    }
}