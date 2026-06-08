package com.nti.nti_backend.application;

import com.nti.nti_backend.Application;
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
import java.util.Map;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApplicationController {

    private static final String STUDENT_OR_SUPER_ADMIN =
            "hasAnyRole('STUDENT','SUPER_ADMIN')";

    private final ApplicationService appService;

    // Створити draft
    @PostMapping("/applications")
    @PreAuthorize(STUDENT_OR_SUPER_ADMIN)
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
    @PreAuthorize(STUDENT_OR_SUPER_ADMIN)
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
    @PreAuthorize(STUDENT_OR_SUPER_ADMIN)
    public ResponseEntity<ApplicationDTO> submit(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(appService.submit(id, user.getId()));
    }

    // Мої заявки
    @GetMapping("/applications/my")
    @PreAuthorize(STUDENT_OR_SUPER_ADMIN)
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
    @PreAuthorize(STUDENT_OR_SUPER_ADMIN)
    public ResponseEntity<ApplicationDTO> getMyByCall(
            @AuthenticationPrincipal User user,
            @PathVariable Long callId) {
        return appService.getMyByCall(user.getId(), callId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Одна заявка (власник, ментор, комісія, адмін)
    @GetMapping("/applications/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getOne(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(appService.getByIdForViewer(id, user));
        } catch (RuntimeException e) {
            if ("Немає доступу".equals(e.getMessage())) {
                return ResponseEntity.status(403).build();
            }
            if ("Заявку не знайдено".equals(e.getMessage())) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    // Upload документу
    @PostMapping(
            value = "/applications/{id}/documents/{documentType}",
            consumes = "multipart/form-data"
    )
    @PreAuthorize(STUDENT_OR_SUPER_ADMIN)
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

    // Змінити статус — адмін або уповноважений комісії (SUPER_EVALUATOR)
    @PatchMapping("/admin/applications/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_EVALUATOR')")
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

    // Add product owner
    @PatchMapping("/applications/{id}/product-owner")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApplicationDTO> setProductOwner(
            @PathVariable Long id,
            @RequestParam Long userId,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(appService.setProductOwner(id, userId, currentUser));
    }

    @GetMapping("/applications/by-call/{callId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FIRM', 'FIRM_USER')")
    public ResponseEntity<List<ApplicationDTO>> getByCall(
            @PathVariable Long callId
    ) {
        return ResponseEntity.ok(appService.getByCall(callId));
    }

    /** Завершити проект — лідер команди */
    @PatchMapping("/applications/{id}/complete")
    @PreAuthorize(STUDENT_OR_SUPER_ADMIN)
    public ResponseEntity<?> completeProject(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(appService.completeProject(id, user.getId(), false));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Завершити проект — адмін */
    @PatchMapping("/admin/applications/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> completeProjectAdmin(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(appService.completeProject(id, user.getId(), true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Список запитів на завершення — для адміна */
    @GetMapping("/admin/applications/completion-requests")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> getCompletionRequests() {
        return ResponseEntity.ok(appService.getCompletionRequests());
    }

    /** Адмін підтверджує завершення проекту */
    @PatchMapping("/admin/applications/{id}/approve-completion")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> approveCompletion(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(appService.approveCompletion(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** Адмін відхиляє запит на завершення */
    @PatchMapping("/admin/applications/{id}/reject-completion")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> rejectCompletion(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(appService.rejectCompletion(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** Історія проектів поточного користувача */
    @GetMapping("/applications/my/projects")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjectHistoryDTO> getMyProjects(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(appService.getMyProjects(user.getId()));
    }
}

