package com.nti.nti_backend.auth;

import com.nti.nti_backend.user.Role;
import com.nti.nti_backend.user.UserDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verifyEmail(
            @RequestParam String token) {
        return ResponseEntity.ok(authService.verifyEmail(token));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req);
        // Завжди однакова відповідь — щоб не розкривати чи існує email
        return ResponseEntity.ok("Якщо email існує — лист надіслано");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok("Пароль змінено");
    }

    // --- Тільки SUPER_ADMIN ---

    @PostMapping("/admin/users/{id}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<String> approveUser(@PathVariable Long id) {
        authService.approveUser(id);
        return ResponseEntity.ok("Акаунт схвалено");
    }

    @PostMapping("/admin/users/{id}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<String> rejectUser(
            @PathVariable Long id,
            @RequestParam String reason) {
        authService.rejectUser(id, reason);
        return ResponseEntity.ok("Акаунт відхилено");
    }

    @PostMapping("/admin/users/{id}/suspend")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<String> suspendUser(
            @PathVariable Long id,
            @RequestParam String reason) {
        authService.suspendUser(id, reason);
        return ResponseEntity.ok("Акаунт заблоковано");
    }

    @PostMapping("/admin/users/{id}/roles/add")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<String> addRole(
            @PathVariable Long id,
            @RequestParam String role) {
        authService.addRole(id, Role.valueOf(role));
        return ResponseEntity.ok("Роль додано");
    }

    @PostMapping("/admin/users/{id}/roles/remove")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<String> removeRole(
            @PathVariable Long id,
            @RequestParam String role) {
        authService.removeRole(id, Role.valueOf(role));
        return ResponseEntity.ok("Роль видалено");
    }

    @GetMapping("/admin/users/pending")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UserDTO>> getPendingUsers() {
        return ResponseEntity.ok(
                authService.getPendingUsers()
        );
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(
                authService.getAllUsers()
        );
    }

}