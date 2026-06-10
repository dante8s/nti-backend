package com.nti.nti_backend.qualification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class QualificationController {

    private final QualificationService service;

    // ── Public ────────────────────────────────────────────────────────────────

    @GetMapping("/api/public/qualification-stacks")
    public ResponseEntity<List<QualificationDTO.StackDTO>> getAll() {
        return ResponseEntity.ok(service.getAllStacks());
    }

    @GetMapping("/api/public/qualification-stacks/{key}")
    public ResponseEntity<QualificationDTO.StackDTO> getByKey(@PathVariable String key) {
        return ResponseEntity.ok(service.getByKey(key));
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @GetMapping("/api/admin/qualification-stacks")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<QualificationDTO.StackDTO>> getAllAdmin() {
        return ResponseEntity.ok(service.getAllStacks());
    }

    @PostMapping("/api/admin/qualification-stacks/{stackId}/subjects")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<QualificationDTO.StackDTO> addSubject(
            @PathVariable Long stackId,
            @RequestBody SubjectRequest req) {
        return ResponseEntity.ok(service.addSubject(stackId, req.subjectName()));
    }

    @DeleteMapping("/api/admin/qualification-stacks/{stackId}/subjects/{subjectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<QualificationDTO.StackDTO> removeSubject(
            @PathVariable Long stackId,
            @PathVariable Long subjectId) {
        return ResponseEntity.ok(service.removeSubject(stackId, subjectId));
    }

    public record SubjectRequest(String subjectName) {}
}
