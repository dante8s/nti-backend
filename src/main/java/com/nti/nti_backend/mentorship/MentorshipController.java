package com.nti.nti_backend.mentorship;

import com.nti.nti_backend.mentorship.dto.AssignMentorRequestDTO;
import com.nti.nti_backend.mentorship.dto.MentorshipResponseDTO;
import com.nti.nti_backend.mentorship.entity.MentorshipStatus;
import com.nti.nti_backend.mentorship.dto.PublicMentorDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<MentorshipResponseDTO>> getMyMentorships() {
        return ResponseEntity.ok(mentorshipService.getMyMentorships());
    }

    // GET /api/mentorships/{id}
    @GetMapping("/mentorships/{id}")
    public ResponseEntity<MentorshipResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(mentorshipService.getById(id));
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
}
