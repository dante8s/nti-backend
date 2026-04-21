package com.nti.nti_backend.mentorship;


import com.nti.nti_backend.mentorship.dto.AssignMentorRequestDTO;
import com.nti.nti_backend.mentorship.dto.MentorshipResponseDTO;
import com.nti.nti_backend.mentorship.entity.Mentorship;
import com.nti.nti_backend.mentorship.entity.MentorshipStatus;
import com.nti.nti_backend.mentorship.repository.MentorshipRepository;
import com.nti.nti_backend.organization.exception.ConflictException;
import com.nti.nti_backend.organization.exception.ResourceNotFoundException;
import com.nti.nti_backend.user.User;
import com.nti.nti_backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class MentorshipService {

    private final MentorshipRepository mentorshipRepository;
    private final UserRepository userRepository;


    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    // ASSIGN MENTOR
    @Transactional
    public MentorshipResponseDTO assignMentor(AssignMentorRequestDTO dto) {

        User mentor = userRepository.findById(
                dto.getMentorUserId()
        ).orElseThrow(() -> new ResourceNotFoundException(
                "User not found with id: " + dto.getMentorUserId()
        ));

        if (!mentor.getRole().name().equals("MENTOR")) {
            throw new ConflictException(
                    "User " + mentor.getEmail() + " does not have the MENTOR role"
            );
        }

        if (dto.getApplicationId() != null) {
            boolean alreadyExists = mentorshipRepository
                    .existsByMentorIdAndApplicationIdAndStatus(
                            dto.getMentorUserId(),
                            dto.getApplicationId(),
                            MentorshipStatus.ACTIVE
                    );
            if (alreadyExists) {
                throw new ConflictException(
                        "This mentor already has an active mentorship on that application"
                );
            }
        }

        Mentorship mentorship = Mentorship.builder()
                .mentor(mentor)
                .applicationId(dto.getApplicationId())
                .status(MentorshipStatus.ACTIVE)
                .build();

        mentorship = mentorshipRepository.save(mentorship);
        return toResponseDTO(mentorship);
    }

    // GET my mentorships (mentor)
    @Transactional(readOnly = true)
    public List<MentorshipResponseDTO> getMyMentorships() {
        Long currentUserId = getCurrentUser().getId();
        return mentorshipRepository
                .findAllByMentorIdAndStatus(currentUserId, MentorshipStatus.ACTIVE)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // Get by id
    @Transactional(readOnly = true)
    public MentorshipResponseDTO getById(UUID id) {
        Mentorship mentorship = mentorshipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mentorship with id: " + id + " not found"
                ));

        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
        boolean isMentor = mentorship.getMentor().getId().equals(currentUser.getId());

        if (!isAdmin && !isMentor) {
            throw new ConflictException("You do not have access to this mentorship");
        }

        return toResponseDTO(mentorship);

    }

    // Close Mentorship
    @Transactional
    public MentorshipResponseDTO closeMentorship(UUID id, MentorshipStatus newStatus) {
        Mentorship mentorship = mentorshipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mentorship not found: " + id));
        if (mentorship.getStatus() != MentorshipStatus.ACTIVE) {
            throw new ConflictException(
                    "Mentorship has status " + mentorship.getStatus()
            );
        }

        if (newStatus == MentorshipStatus.ACTIVE) {
            throw new ConflictException("Cannot transition back to ACTIVE");
        }

        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");


        if  (!isAdmin) {
            throw new ConflictException("You cannot close this mentorship");
        }

        mentorship.setStatus(newStatus);
        mentorship.setEndDate(OffsetDateTime.now());
        mentorshipRepository.save(mentorship);

        return toResponseDTO(mentorship);
    }

    // Mapping
    private MentorshipResponseDTO toResponseDTO(
            Mentorship m
    ) {
        return MentorshipResponseDTO.builder()
                .id(m.getId())
                .mentorUserId(m.getMentor().getId())
                .mentorName(m.getMentor().getName())
                .mentorEmail(m.getMentor().getEmail())
                .applicationId(m.getApplicationId())
                .status(m.getStatus())
                .startDate(m.getStartDate())
                .endDate(m.getEndDate())
                .createdAt(m.getCreatedAt())
                .build();
    }

}
