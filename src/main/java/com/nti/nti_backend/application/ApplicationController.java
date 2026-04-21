package com.nti.nti_backend.application;

import com.nti.nti_backend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService appService;

    // Створити draft
    @PostMapping("/applications")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApplicationDTO> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody
            CreateApplicationRequest request) {
        return ResponseEntity.ok(
                appService.createDraft(user, request)
        );
    }

    // Відправити заявку
    @PatchMapping("/applications/{id}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApplicationDTO> submit(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(
                appService.submit(id, user.getId())
        );
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

    // Одна заявка
    @GetMapping("/applications/{id}")
    public ResponseEntity<ApplicationDTO> getOne(
            @PathVariable Long id) {
        return ResponseEntity.ok(appService.getById(id));
    }

    // Всі заявки — тільки ADMIN
    @GetMapping("/admin/applications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ApplicationDTO>> getAll() {
        return ResponseEntity.ok(appService.getAll());
    }

    // Змінити статус — тільки ADMIN
    @PatchMapping("/admin/applications/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApplicationDTO> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest req) {
        return ResponseEntity.ok(
                appService.changeStatus(
                        id,
                        ApplicationStatus.valueOf(req.status()),
                        req.comment()
                )
        );
    }
}