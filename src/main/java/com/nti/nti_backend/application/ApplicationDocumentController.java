package com.nti.nti_backend.application;

import com.nti.nti_backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * View / status of application documents — extracted from {@link ApplicationController}
 * to reduce changes to the shared controller when file handling changes.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApplicationDocumentController {

    private final ApplicationService appService;

    @GetMapping("/applications/{id}/documents/status")
    @PreAuthorize("hasAnyRole('STUDENT','MENTOR','EVALUATOR','SUPER_EVALUATOR','ADMIN','SUPER_ADMIN','FIRM','FIRM_USER')")
    public ResponseEntity<List<DocumentStatusDTO>> getDocumentStatus(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(appService.getDocumentStatus(id, user));
        } catch (RuntimeException e) {
            if ("Access denied".equals(e.getMessage())) {
                return ResponseEntity.status(403).build();
            }
            if ("Application not found".equals(e.getMessage())) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    /**
     * View / download file (inline for iframe; disposition=attachment — save to disk).
     */
    @GetMapping("/applications/{id}/documents/{documentType}")
    @PreAuthorize("hasAnyRole('STUDENT','MENTOR','EVALUATOR','SUPER_EVALUATOR','ADMIN','SUPER_ADMIN','FIRM','FIRM_USER')")
    public ResponseEntity<Resource> getApplicationDocument(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @PathVariable String documentType,
            @RequestParam(defaultValue = "inline") String disposition) {
        try {
            DocumentType dt = DocumentType.valueOf(documentType);
            ApplicationService.ServedApplicationDocument served =
                    appService.serveApplicationDocument(id, dt, user);
            Resource body = served.resource();
            if (!body.exists() || !body.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            boolean attach = "attachment".equalsIgnoreCase(disposition.trim());
            String cd = attach
                    ? ApplicationService.contentDispositionAttachment(served.filename())
                    : ApplicationService.contentDispositionInline(served.filename());
            return ResponseEntity.ok()
                    .contentType(served.contentType())
                    .header(HttpHeaders.CONTENT_DISPOSITION, cd)
                    .body(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            if ("Access denied".equals(e.getMessage())) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.notFound().build();
        }
    }
}
