package com.nti.nti_backend.mentorship;

import com.nti.nti_backend.mentorship.dto.AddNoteRequestDTO;
import com.nti.nti_backend.mentorship.dto.ConsultationNoteDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/consultation-notes")
@RequiredArgsConstructor
public class ConsultationNoteController {

    private final ConsultationNoteService noteService;

    @PostMapping
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<ConsultationNoteDTO> create(
            @Valid @RequestBody AddNoteRequestDTO dto
    ) {
        return ResponseEntity.status(201).body(noteService.create(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MENTOR','ADMIN','SUPER_ADMIN','STUDENT', 'FIRM')")
    public ResponseEntity<List<ConsultationNoteDTO>> getByApplication(
            @RequestParam Long applicationId
    ) {
        return ResponseEntity.ok(noteService.getByApplication(applicationId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<ConsultationNoteDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody AddNoteRequestDTO dto
    ) {
        return ResponseEntity.ok(noteService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MENTOR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id
    ) {
        noteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
