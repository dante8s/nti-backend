package com.nti.nti_backend.audit;

import com.nti.nti_backend.application.Application;
import com.nti.nti_backend.application.ApplicationRepository;
import com.nti.nti_backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;
    private final ApplicationRepository applicationRepository;

    // Загальний журнал аудиту — тільки SUPER_ADMIN
    @GetMapping("/admin/audit")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<AuditEventDTO>> getAll(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action) {
        return ResponseEntity.ok(auditService.getAll(entityType, action));
    }

    @GetMapping("/applications/{id}/audit")
    @PreAuthorize(
            "hasAnyRole('ADMIN','SUPER_ADMIN','STUDENT','EVALUATOR','SUPER_EVALUATOR')"
    )
    public ResponseEntity<List<AuditEventDTO>> getApplicationAudit(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND, "Заявку не знайдено")
                );

        boolean privileged = user.getAuthorities().stream()
                .anyMatch(a -> {
                    String auth = a.getAuthority();
                    return "ROLE_ADMIN".equals(auth)
                            || "ROLE_SUPER_ADMIN".equals(auth)
                            || "ROLE_EVALUATOR".equals(auth)
                            || "ROLE_SUPER_EVALUATOR".equals(auth);
                });
        if (!privileged && !application.getApplicant().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    FORBIDDEN,
                    "Немає доступу до аудиту цієї заявки"
            );
        }

        return ResponseEntity.ok(auditService.getForApplication(id));
    }
}
