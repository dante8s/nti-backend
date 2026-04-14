package com.nti.nti_backend.program;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProgramController {

    private final ProgramService programService;

    // Публічні — без токена
    @GetMapping("/api/public/programs")
    public ResponseEntity<List<ProgramDTO>> getAll() {
        return ResponseEntity.ok(
                programService.getAllActive()
        );
    }

    @GetMapping("/api/public/programs/{id}")
    public ResponseEntity<ProgramDTO> getOne(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                programService.getById(id)
        );
    }

    // Тільки ADMIN
    @PostMapping("/api/admin/programs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProgramDTO> create(
            @RequestBody ProgramDTO dto) {
        return ResponseEntity.ok(
                programService.create(dto)
        );
    }

    @PutMapping("/api/admin/programs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProgramDTO> update(
            @PathVariable Long id,
            @RequestBody ProgramDTO dto) {
        return ResponseEntity.ok(
                programService.update(id, dto)
        );
    }

    @DeleteMapping("/api/admin/programs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(
            @PathVariable Long id) {
        programService.deactivate(id);
        return ResponseEntity.ok().build();
    }
}