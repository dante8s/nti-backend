package com.nti.nti_backend.milestone;

import com.nti.nti_backend.milestone.dto.ChangeStatusRequestDTO;
import com.nti.nti_backend.milestone.dto.MilestoneRequestDTO;
import com.nti.nti_backend.milestone.dto.MilestoneResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/milestones")
@RequiredArgsConstructor
public class MilestoneController {
    private final MilestoneService milestoneService;

    // POST /api/milestones
    @PostMapping
    public ResponseEntity<MilestoneResponseDTO> create(
            @Valid @RequestBody MilestoneRequestDTO dto
            ) {
        MilestoneResponseDTO created = milestoneService.create(dto);
        return ResponseEntity
                .created(URI.create("/api/milestones/" + created.getId()))
                .body(created);
    }

    // GET /api/milestones
    @GetMapping
    public ResponseEntity<List<MilestoneResponseDTO>> findAll(
            @RequestParam(required = false) Long applicationId,
            @RequestParam(required = false) UUID mentorshipId,
            @RequestParam(required = false) Long createdById
    ) {
        return ResponseEntity.ok(
                milestoneService.findAll(applicationId, mentorshipId, createdById)
        );
    }

    // GET /api/milestones/{id}
    @GetMapping("/{id}")
    public ResponseEntity<MilestoneResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(milestoneService.findById(id));
    }

    // PUT /api/milestones/{id} - title, description, dueDate
    @PutMapping("/{id}")
    public ResponseEntity<MilestoneResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody MilestoneRequestDTO dto
    ) {
        return ResponseEntity.ok(milestoneService.update(id, dto));
    }

    // PATCH /api/milestones/{id}/status - status
    @PatchMapping("/{id}/status")
    public ResponseEntity<MilestoneResponseDTO> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeStatusRequestDTO dto
    ) {
        return ResponseEntity.ok(milestoneService.changeStatus(id, dto));
    }

    // GET /api/milestones/pending-approval
    @GetMapping("/pending-approval")
    public ResponseEntity<List<MilestoneResponseDTO>> getPendingApproval() {
        return ResponseEntity.ok(milestoneService.getPendingApproval());
    }
}
