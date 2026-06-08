package com.nti.nti_backend.milestone;

import com.nti.nti_backend.milestone.dto.*;
import com.nti.nti_backend.milestone.entity.MilestoneAttachment;
import com.nti.nti_backend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    // DELETE /api/milestones/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMilestone(@PathVariable UUID id) {
        milestoneService.deleteMilestone(id);
        return ResponseEntity.noContent().build();
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

    @PostMapping("/{id}/comments")
    public ResponseEntity<MilestoneCommentDTO> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody MilestoneCommentRequest dto,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.status(201)
                .body(milestoneService.addComment(id, dto, user));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<MilestoneCommentDTO>> getComments(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(milestoneService.getComments(id));
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID id,
            @PathVariable UUID commentId,
            @AuthenticationPrincipal User user
    ) {
        milestoneService.deleteComment(id, commentId, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/attachments")
    public ResponseEntity<MilestoneAttachmentDTO> addAttachment(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user
            ) {
        return ResponseEntity.status(201)
                .body(milestoneService.addAttachment(id, file, user));
    }

    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<MilestoneAttachmentDTO>> getAttachments(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(milestoneService.getAttachments(id));
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable UUID id,
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal User user
    ) {
        milestoneService.deleteAttachment(id, attachmentId, user);
        return ResponseEntity.noContent().build();
    }

    // GET /api/milestones/{id}/attachments/{attachmentId}/file?inline=true
    @GetMapping("/{id}/attachments/{attachmentId}/file")
    public ResponseEntity<Resource> serveAttachment(
            @PathVariable UUID id,
            @PathVariable UUID attachmentId,
            @RequestParam(defaultValue = "false") boolean inline,
            @AuthenticationPrincipal User user
    ) {
        return milestoneService.serveAttachment(id, attachmentId, inline, user);
    }
}
