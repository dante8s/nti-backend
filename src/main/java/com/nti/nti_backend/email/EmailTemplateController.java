package com.nti.nti_backend.email;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/email-templates")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class EmailTemplateController {

    private final EmailTemplateService service;

    @GetMapping
    public ResponseEntity<List<EmailTemplateDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailTemplateDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getAll().stream()
                .filter(t -> t.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Template not found")));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmailTemplateDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmailTemplateRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }
}
