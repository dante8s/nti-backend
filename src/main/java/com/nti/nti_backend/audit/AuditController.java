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

    // General audit log — SUPER_ADMIN only
    @GetMapping("/admin/audit")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<AuditEventDTO>> getAll(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action) {
        return ResponseEntity.ok(auditService.getAll(entityType, action));
    }

    @GetMapping("/applications/{id}/audit")
    @PreAuthorize(
            "hasAnyRole('ADMIN','SUPER_ADMIN','STUDENT','MENTOR','EVALUATOR','SUPER_EVALUATOR')"
    )
    public ResponseEntity<List<AuditEventDTO>> getApplicationAudit(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND, "Application not found")
                );

        boolean privileged = user.getAuthorities().stream()
                .anyMatch(a -> {
                    String auth = a.getAuthority();
                    return "ROLE_ADMIN".equals(auth)
                            || "ROLE_SUPER_ADMIN".equals(auth)
                            || "ROLE_EVALUATOR".equals(auth)
                            || "ROLE_SUPER_EVALUATOR".equals(auth);
                });

        boolean isTeamMember = application.getTeamSnapshot() != null && application.getTeamSnapshot().stream()
                .anyMatch(member -> member.getUserId() != null && member.getUserId().equals(user.getId()));

        if (!privileged && !application.getApplicant().getId().equals(user.getId()) && !isTeamMember) {
            throw new ResponseStatusException(
                    FORBIDDEN,
                    "Access to this application's audit log is denied"
            );
        }

        return ResponseEntity.ok(auditService.getForApplication(id));
    }
}
