package com.nti.nti_backend.call;

import com.nti.nti_backend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CallController {

    private final CallService callService;

    // Публічні
    @GetMapping("/api/public/calls")
    public ResponseEntity<List<CallDTO>> getAllOpen() {
        return ResponseEntity.ok(
                callService.getAllOpen()
        );
    }

    @GetMapping("/api/public/programs/{programId}/calls")
    public ResponseEntity<List<CallDTO>> getByProgram(
            @PathVariable Long programId) {
        return ResponseEntity.ok(
                callService.getOpenByProgram(programId)
        );
    }

    @GetMapping("/api/public/calls/{id}")
    public ResponseEntity<CallDTO> getOne(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                callService.getById(id)
        );
    }

    // Тільки ADMIN
    @PostMapping("/api/admin/programs/{programId}/calls")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CallDTO> create(
            @PathVariable Long programId,
            @Valid @RequestBody CreateCallRequest request,
            @AuthenticationPrincipal User actor) {
        return ResponseEntity.ok(callService.create(programId, request, actor));
    }

    @PatchMapping("/api/admin/calls/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> close(
            @PathVariable Long id,
            @AuthenticationPrincipal User actor) {
        callService.close(id, actor);
        return ResponseEntity.ok().build();
    }

    // Get calls by Program
    @GetMapping("/api/calls")
    public ResponseEntity<List<CallDTO>> getAllByProgram(
            @RequestParam Long programId
    ) {
        return ResponseEntity.ok(callService.getByProgram(programId));
    }
}