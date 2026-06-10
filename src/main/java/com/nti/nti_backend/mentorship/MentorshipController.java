package com.nti.nti_backend.mentorship;

import com.nti.nti_backend.mentorship.dto.*;
import com.nti.nti_backend.mentorship.entity.MentorshipStatus;
import com.nti.nti_backend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MentorshipController {

    private final MentorshipService mentorshipService;

    // POST /api/mentorships — only admin assigns a mentor
    @PostMapping("/mentorships")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<MentorshipResponseDTO> assignMentor(
            @Valid @RequestBody AssignMentorRequestDTO dto
            ) {
        return ResponseEntity.status(201).body(mentorshipService.assignMentor(dto));
    }

    // GET /api/mentorships/my
    @GetMapping("/mentorships/my")
    @PreAuthorize("hasAnyRole('MENTOR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<MentorshipResponseDTO>> getMyMentorships(
            @AuthenticationPrincipal User user
    ) {
        // 1. Log the user to see if it is null
        if (user == null) {
            System.out.println("DEBUG: User is NULL in getMyMentorships");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build(); // Return 400 if user is null
        }

        // 2. Log the ID
        System.out.println("DEBUG: Fetching mentorships for User ID: " + user.getId());

        // 3. Proceed
        return ResponseEntity.ok(mentorshipService.getMyMentorships(user.getId()));
    }

    // GET /api/mentorships/{id}
    @GetMapping("/mentorships/{id}")
    @PreAuthorize("hasAnyRole('MENTOR','ADMIN','SUPER_ADMIN','STUDENT')")
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
    @PreAuthorize("hasAnyRole('MENTOR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<MentorshipResponseDTO> closeMentorship(
            @PathVariable UUID id,
            @RequestParam MentorshipStatus status
    ) {
        return ResponseEntity.ok(mentorshipService.closeMentorship(id, status));
    }

    // GET /api/public/mentors — public, no @PreAuthorize (allowed in SecurityConfig)
    @GetMapping("/public/mentors")
    public ResponseEntity<List<PublicMentorDTO>> getPublicMentors() {
        return ResponseEntity.ok(mentorshipService.getPublicMentors());
    }

    // GET /api/mentorships — all mentorships (admin only)
    @GetMapping("/mentorships")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<MentorshipResponseDTO>> getAll() {
        return ResponseEntity.ok(mentorshipService.getAll());
    }

    // GET api/mentorships/by-application/{applicationId}
    @GetMapping("/mentorships/by-application/{applicationId}")
    @PreAuthorize("hasAnyRole('MENTOR','ADMIN','SUPER_ADMIN','STUDENT','FIRM','FIRM_USER')")
    public ResponseEntity<List<MentorshipResponseDTO>> getByApplication(
            @PathVariable Long applicationId
    ) {
        return ResponseEntity.ok(mentorshipService.getByApplication(applicationId));
    }

    @PostMapping("/consultations")
    @PreAuthorize("hasAnyRole('MENTOR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ConsultationResponseDTO> createConsultation(
            @Valid @RequestBody ConsultationRequestDTO dto) {
        return ResponseEntity.status(201)
                .body(mentorshipService.createConsultation(dto));
    }

    @GetMapping("/consultations")
    @PreAuthorize("hasAnyRole('MENTOR','ADMIN','SUPER_ADMIN','STUDENT')")
    public ResponseEntity<List<ConsultationResponseDTO>> getConsultationsByMentorship(
            @RequestParam UUID mentorshipId
    ) {
        return ResponseEntity.ok(
                mentorshipService.getConsultationsByMentorshipId(mentorshipId));
    }

    @PutMapping("/consultations/{id}")
    @PreAuthorize("hasAnyRole('MENTOR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ConsultationResponseDTO> updateConsultation(
            @PathVariable UUID id,
            @Valid @RequestBody ConsultationRequestDTO dto
    ) {
        return ResponseEntity.ok(mentorshipService.updateConsultation(id, dto));
    }

    @DeleteMapping("/consultations/{id}")
    @PreAuthorize("hasAnyRole('MENTOR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> deleteConsultation(@PathVariable UUID id) {
        mentorshipService.deleteConsultation(id);
        return ResponseEntity.noContent().build();
    }
}
