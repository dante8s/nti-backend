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

    // Program A
    @GetMapping("/api/public/programs-a")
    public ResponseEntity<List<ProgramDTO>> getAllProgramA() {
        return ResponseEntity.ok(programService.getByType(ProgramType.PROGRAM_A));
    }

    // Program B
    @GetMapping("/api/public/programs-b")
    public ResponseEntity<List<ProgramDTO>> getAllProgramB() {
        return ResponseEntity.ok(programService.getByType(ProgramType.PROGRAM_B));
    }

    @GetMapping("/api/public/programs-a/{id}")
    public ResponseEntity<ProgramDTO> getProgramA(@PathVariable Long id) {
        return ResponseEntity.ok(
                programService.getByIdAndType(id, ProgramType.PROGRAM_A)
        );
    }

    @GetMapping("/api/public/programs-b/{id}")
    public ResponseEntity<ProgramDTO> getProgramB(@PathVariable Long id) {
        return ResponseEntity.ok(
                programService.getByIdAndType(id, ProgramType.PROGRAM_B)
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