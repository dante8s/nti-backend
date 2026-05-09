package com.nti.nti_backend.application;

import com.nti.nti_backend.audit.AuditService;
import com.nti.nti_backend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService appService;
    private final DocumentRepository documentRepository;
    private final AuditService auditService;

    // Створити draft
    @PostMapping("/applications")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApplicationDTO> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateApplicationRequest request) {
        return ResponseEntity.ok(appService.createDraft(user, request));
    }

    /**
     * PUT /api/applications/{id}
     * Оновити formData чернетки або NEEDS_REVISION без зміни статусу.
     */
    @PutMapping("/applications/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApplicationDTO> update(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody UpdateApplicationRequest request) {
        return ResponseEntity.ok(
                appService.updateDraft(id, user.getId(), request)
        );
    }

    // Відправити заявку
    @PatchMapping("/applications/{id}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApplicationDTO> submit(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(appService.submit(id, user.getId()));
    }

    // Мої заявки
    @GetMapping("/applications/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<ApplicationDTO>> getMy(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                appService.getMyApplications(user.getId())
        );
    }

    /**
     * GET /api/applications/my/by-call/{callId}
     * Знайти мою заявку для конкретного виклику.
     * Повертає 404 якщо заявки немає — фронт використовує це
     * щоб зрозуміти: відкривати форму для створення чи редагування.
     */
    @GetMapping("/applications/my/by-call/{callId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApplicationDTO> getMyByCall(
            @AuthenticationPrincipal User user,
            @PathVariable Long callId) {
        return appService.getMyByCall(user.getId(), callId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Одна заявка
    @GetMapping("/applications/{id}")
    public ResponseEntity<ApplicationDTO> getOne(
            @PathVariable Long id) {
        return ResponseEntity.ok(appService.getById(id));
    }

    // Статус документів заявки
    @GetMapping("/applications/{id}/documents/status")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<DocumentStatusDTO>> getDocumentStatus(
            @PathVariable Long id) {
        return ResponseEntity.ok(appService.getDocumentStatus(id));
    }

    // Upload документу
    @PostMapping(
            value = "/applications/{id}/documents/{documentType}",
            consumes = "multipart/form-data"
    )
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<DocumentDTO> uploadDocument(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @PathVariable String documentType,
            @RequestParam("file") MultipartFile file) {

        String contentType = file.getContentType();
        boolean isPdf = "application/pdf".equals(contentType);
        boolean isDocx = ("application/vnd.openxmlformats"
                + "-officedocument.wordprocessingml.document")
                .equals(contentType);

        if (!isPdf && !isDocx) return ResponseEntity.badRequest().build();
        if (file.getSize() > 10L * 1024 * 1024) return ResponseEntity.badRequest().build();

        try {
            String uploadDir = "uploads/applications/" + id + "/";
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path path = Paths.get(uploadDir + fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());

            DocumentType docType = DocumentType.valueOf(documentType);
            DocumentDTO result = appService.saveDocument(
                    id, user.getId(),
                    file.getOriginalFilename(),
                    uploadDir + fileName,
                    isPdf ? "PDF" : "DOCX",
                    docType
            );
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Всі заявки — ADMIN
    @GetMapping("/admin/applications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ApplicationDTO>> getAll() {
        return ResponseEntity.ok(appService.getAll());
    }

    // Змінити статус — ADMIN
    @PatchMapping("/admin/applications/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> changeStatus(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest req) {
        try {
            return ResponseEntity.ok(
                    appService.changeStatus(
                            id,
                            ApplicationStatus.valueOf(req.status()),
                            req.comment(),
                            admin
                    )
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }
}