package com.nti.nti_backend.application;

import com.nti.nti_backend.Application;
import com.nti.nti_backend.file.FileTypeValidator;
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
    @PreAuthorize("hasAnyRole('STUDENT','MENTOR','EVALUATOR','SUPER_EVALUATOR','ADMIN','SUPER_ADMIN','FIRM','FIRM_USER')")
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

if (file.getSize() > 10L * 1024 * 1024)
    return ResponseEntity.badRequest().build();

boolean isPdf = false;
boolean isDocx = false;

try {
    isPdf = FileTypeValidator.isPdf(file);
    isDocx = FileTypeValidator.isDocx(file);
} catch (IOException e) {
    return ResponseEntity.badRequest().build();
}

// Перевірка PPTX
boolean isPptx = "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        .equals(file.getContentType());

// PPTX дозволений тільки для RESULT_1 та RESULT_2
boolean isResultDoc = "RESULT_1".equals(documentType) || "RESULT_2".equals(documentType);

if (!isPdf && !isDocx && !(isPptx && isResultDoc))
    return ResponseEntity.badRequest().build();


        try {
            String uploadDir = "uploads/applications/" + id + "/";
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path path = Paths.get(uploadDir + fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());

            DocumentType docType = DocumentType.valueOf(documentType);
            String format = isPdf ? "PDF" : isPptx ? "PPTX" : "DOCX";
            DocumentDTO result = appService.saveDocument(
                    id, user.getId(),
                    file.getOriginalFilename(),
                    uploadDir + fileName,
                    format,
                    docType
            );
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Всі заявки — ADMIN / SUPER_ADMIN
    @GetMapping("/admin/applications")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<ApplicationDTO>> getAll() {
        return ResponseEntity.ok(appService.getAll());
    }

    // Змінити статус — адмін або уповноважений комісії (SUPER_EVALUATOR)
    @PatchMapping("/admin/applications/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','SUPER_EVALUATOR')")
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

    /** Список юзерів для призначення Product Owner — для адмінів */
    @GetMapping("/admin/users/for-po-assignment")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<com.nti.nti_backend.user.UserDTO>> getUsersForPO() {
        return ResponseEntity.ok(appService.getAllUsersForPO());
    }

    /** Список запитів на завершення Program B для Product Owner */
    @GetMapping("/product-owner/applications/completion-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getPOCompletionRequests(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(appService.getPOCompletionRequests(user.getId()));
    }

    /** Product Owner підтверджує завершення */
    @PatchMapping("/product-owner/applications/{id}/approve-completion")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> approveCompletionPO(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(appService.approveCompletionPO(id, user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** Product Owner відхиляє завершення */
    @PatchMapping("/product-owner/applications/{id}/reject-completion")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> rejectCompletionPO(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(appService.rejectCompletionPO(id, user.getId()));
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

