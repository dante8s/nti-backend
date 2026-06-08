package com.nti.nti_backend.mentorship;

import com.nti.nti_backend.mentorship.dto.*;
import com.nti.nti_backend.mentorship.entity.MentorshipStatus;
import com.nti.nti_backend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MentorshipController {

    private final MentorshipService mentorshipService;

    // POST /api/mentorships
    @PostMapping("/mentorships")
    public ResponseEntity<MentorshipResponseDTO> assignMentor(
            @Valid @RequestBody AssignMentorRequestDTO dto
            ) {
        return ResponseEntity.status(201).body(mentorshipService.assignMentor(dto));
    }

    // GET /api/mentorships/my
    @GetMapping("/mentorships/my")
    public ResponseEntity<List<MentorshipResponseDTO>> getMyMentorships(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(mentorshipService.getMyMentorships(user.getId()));
    }

    // GET /api/mentorships/{id}
    @GetMapping("/mentorships/{id}")
    public ResponseEntity<MentorshipResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(mentorshipService.getById(id));
    }

    // DELETE api/mentorships/{id}
    @DeleteMapping("/mentorships/{id}")
    public ResponseEntity<Void> deleteMentorship(@PathVariable UUID id) {
        mentorshipService.deleteMentorship(id);
        return ResponseEntity.noContent().build();
    }

    // PATCH /api/mentorships/{id}/status
    @PatchMapping("/mentorships/{id}/status")
    public ResponseEntity<MentorshipResponseDTO> closeMentorship(
            @PathVariable UUID id,
            @RequestParam MentorshipStatus status
    ) {
        return ResponseEntity.ok(mentorshipService.closeMentorship(id, status));
    }

    // GET /api/public/mentors
    @GetMapping("/public/mentors")
    public ResponseEntity<List<PublicMentorDTO>> getPublicMentors() {
        return ResponseEntity.ok(mentorshipService.getPublicMentors());
    }

    // GET /api/mentorships
    @GetMapping("/mentorships")
    public ResponseEntity<List<MentorshipResponseDTO>> getAll() {
        return ResponseEntity.ok(mentorshipService.getAll());
    }

    // GET api/mentorships/by-application/{applicationId}
    @GetMapping("/mentorships/by-application/{applicationId}")
    public ResponseEntity<List<MentorshipResponseDTO>> getByApplication(
            @PathVariable Long applicationId
    ) {
        return ResponseEntity.ok(mentorshipService.getByApplication(applicationId));
    }

    @PostMapping("/consultations")
    public ResponseEntity<ConsultationResponseDTO> createConsultation(
            @Valid @RequestBody ConsultationRequestDTO dto) {
        return ResponseEntity.status(201)
                .body(mentorshipService.createConsultation(dto));
    }

    @GetMapping("/consultations")
    public ResponseEntity<List<ConsultationResponseDTO>> getConsultationsByMentorship(
            @RequestParam UUID mentorshipId
    ) {
        return ResponseEntity.ok(
                mentorshipService.getConsultationsByMentorshipId(mentorshipId));
    }

    @PutMapping("/consultations/{id}")
    public ResponseEntity<ConsultationResponseDTO> updateConsultation(
            @PathVariable UUID id,
            @Valid @RequestBody ConsultationRequestDTO dto
    ) {
        return ResponseEntity.ok(mentorshipService.updateConsultation(id, dto));
    }

    @DeleteMapping("/consultations/{id}")
    public ResponseEntity<Void> deleteConsultation(@PathVariable UUID id) {
        mentorshipService.deleteConsultation(id);
        return ResponseEntity.noContent().build();
    }
}
