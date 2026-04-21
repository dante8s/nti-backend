package com.nti.nti_backend.mentorship;

import com.nti.nti_backend.mentorship.dto.AddNoteRequestDTO;
import com.nti.nti_backend.mentorship.dto.ConsultationNoteDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/consultation-notes")
@RequiredArgsConstructor
public class ConsultationNoteController {

    private final ConsultationNoteService noteService;

    // POST /api/consultation-notes
    @PostMapping
    public ResponseEntity<ConsultationNoteDTO> create(
            @Valid @RequestBody AddNoteRequestDTO dto
    ) {
        return ResponseEntity.status(201).body(noteService.create(dto));
    }

    // GET /api/consultation-notes?applicationId=
    @GetMapping
    public ResponseEntity<List<ConsultationNoteDTO>> getByApplication(
            @RequestParam Long applicationId
    ) {
        return ResponseEntity.ok(
                noteService.getByApplication(applicationId)
        );
    }

    // PUT /api/consultation-notes/{id}
    @PutMapping("/{id}")
    public ResponseEntity<ConsultationNoteDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody AddNoteRequestDTO dto
    ) {
        return ResponseEntity.ok(noteService.update(id, dto));
    }

    // DELETE /api/consultation-notes/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id
    ) {
        noteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
