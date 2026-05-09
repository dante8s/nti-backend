package com.nti.nti_backend.program;

import com.nti.nti_backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    // FIRM submits Program B project
    @PostMapping("/api/programs/submit-program-b")
    @PreAuthorize("hasRole('FIRM')")
    public ResponseEntity<ProgramDTO> submitProgramB(
            @AuthenticationPrincipal User user,
            @RequestBody SubmitProgramBRequest dto
    ) {

        return ResponseEntity.status(201)
                .body(programService.submitProgramB(dto, user));
    }

    @PutMapping("/api/programs/program-b/{id}")
    @PreAuthorize("hasRole('FIRM')")
    public ResponseEntity<ProgramDTO> updateProgramB(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody SubmitProgramBRequest dto
    ) {
        return ResponseEntity.ok(programService.updateProgramB(id, dto, user));
    }

    @PostMapping("/api/programs/program-b/{id}/submit")
    @PreAuthorize("hasRole('FIRM')")
    public ResponseEntity<ProgramDTO> submitForReview(
            @AuthenticationPrincipal User user,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(programService.submitForReview(id, user));
    }

    @GetMapping("/api/programs/my")
    @PreAuthorize("hasRole('FIRM')")
    public ResponseEntity<List<ProgramDTO>> getMyPrograms(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(programService.getMyPrograms(user));
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

    @GetMapping("/api/admin/programs/pending-review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProgramDTO>> getPendingReview() {
        return ResponseEntity.ok(programService.getPendingReview());
    }

    @PostMapping("/api/admin/programs/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProgramDTO> review(
            @PathVariable Long id,
            @RequestBody ReviewProgramRequest dto
    ) {
        return ResponseEntity.ok(programService.review(id, dto));
    }

    @DeleteMapping("/api/admin/programs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(
            @PathVariable Long id) {
        programService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}