package com.nti.nti_backend.mentorship;


import com.nti.nti_backend.application.Application;
import com.nti.nti_backend.application.ApplicationRepository;
import com.nti.nti_backend.application.ApplicationStatus;
import com.nti.nti_backend.mentorship.dto.*;
import com.nti.nti_backend.mentorship.entity.Consultation;
import com.nti.nti_backend.mentorship.entity.Mentorship;
import com.nti.nti_backend.mentorship.entity.MentorshipStatus;
import com.nti.nti_backend.mentorship.repository.ConsultationRepository;
import com.nti.nti_backend.mentorship.repository.MentorshipRepository;
import com.nti.nti_backend.organization.exception.ConflictException;
import com.nti.nti_backend.organization.exception.ResourceNotFoundException;
import com.nti.nti_backend.user.Role;
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
    private final ApplicationRepository applicationRepository;
    private final ConsultationRepository consultationRepository;


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

        if (!mentor.hasRole(Role.MENTOR)) {
            throw new ConflictException(
                    "User " + mentor.getEmail() + " does not have the MENTOR role"
            );
        }

        Application application = null;
        if (dto.getApplicationId() != null) {
            application = applicationRepository.findById(dto.getApplicationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Application not found: " + dto.getApplicationId()
                    ));

            if (application.getStatus() != ApplicationStatus.APPROVED) {
                throw new ConflictException(
                        "Mentorship can only be assigned to approved application"
                );
            }

            boolean alreadyExists = mentorshipRepository
                    .existsByMentorIdAndApplication_IdAndStatus(
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
                .application(application)
                .status(MentorshipStatus.ACTIVE)
                .build();

        return toResponseDTO(mentorshipRepository.save(mentorship));
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
        boolean isAdmin = currentUser.hasRole(Role.ADMIN);
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
        boolean isAdmin = currentUser.hasRole(Role.ADMIN);


        if  (!isAdmin) {
            throw new ConflictException("You cannot close this mentorship");
        }

        mentorship.setStatus(newStatus);
        mentorship.setEndDate(OffsetDateTime.now());
        mentorshipRepository.save(mentorship);

        return toResponseDTO(mentorship);
    }

    @Transactional(readOnly = true)
    public List<MentorshipResponseDTO> getAll() {
        User currentUser = getCurrentUser();
        if (!currentUser.hasRole(Role.ADMIN)) {
            throw new ConflictException("Only ADMIN can view all mentorships");
        }
        return mentorshipRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MentorshipResponseDTO> getByApplication(Long applicationId) {
        return mentorshipRepository.findAllByApplication_Id(applicationId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional
    public ConsultationResponseDTO createConsultation(ConsultationRequestDTO dto) {
        User currentUser = getCurrentUser();

        if (!currentUser.hasRole(Role.MENTOR)) {
            throw new ConflictException("Only MENTOR can add consultations");
        }

        Mentorship mentorship = null;
        if (dto.getMentorshipId() != null) {
            mentorship = mentorshipRepository.findById(dto.getMentorshipId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Mentorship not found: " + dto.getMentorshipId()
                    ));
        }


        Consultation consultation = Consultation.builder()
                .mentorship(mentorship)
                .mentor(currentUser)
                .consultationDate(dto.getConsultationDate())
                .topic(dto.getTopic())
                .description(dto.getDescription())
                .build();
        return toConsultationDTO(consultationRepository.save(consultation));
    }

    @Transactional(readOnly = true)
    public List<ConsultationResponseDTO> getConsultationsByMentorshipId(UUID mentorshipId) {
        return consultationRepository
                .findAllByMentorshipIdOrderByConsultationDateDesc(mentorshipId)
                .stream().map(this::toConsultationDTO).toList();
    }

    @Transactional
    public ConsultationResponseDTO updateConsultation(UUID id, ConsultationRequestDTO dto) {
        User currentUser = getCurrentUser();

        Consultation consultation = consultationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Consultation not found: " + id
                ));
        if (!consultation.getMentor().getId().equals(currentUser.getId())) {
            throw new ConflictException(
                    "Only the author can edit this consultation"
            );
        }

        consultation.setConsultationDate(dto.getConsultationDate());
        consultation.setDescription(dto.getDescription());
        consultation.setTopic(dto.getTopic());

        return toConsultationDTO(consultationRepository.save(consultation));
    }

    @Transactional
    public void deleteConsultation(UUID id) {
        User currentUser = getCurrentUser();

        if (!currentUser.hasRole(Role.ADMIN)) {
            throw new ConflictException("Only ADMIN can delete consultations");
        }

        Consultation consultation = consultationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Consultation not found: " + id
                ));
        consultationRepository.delete(consultation);
    }


    private ConsultationResponseDTO toConsultationDTO(Consultation c) {
        return ConsultationResponseDTO.builder()
                .id(c.getId())
                .mentorshipId(c.getMentorship().getId())
                .mentorId(c.getMentor().getId())
                .mentorName(c.getMentor().getName())
                .consultationDate(c.getConsultationDate())
                .topic(c.getTopic())
                .description(c.getDescription())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
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
                .applicationId(m.getApplication() != null ? m.getApplication().getId() : null)
                .status(m.getStatus())
                .startDate(m.getStartDate())
                .endDate(m.getEndDate())
                .createdAt(m.getCreatedAt())
                .build();
    }

    public List<PublicMentorDTO> getPublicMentors() {
        return userRepository.findAllByRole(Role.MENTOR)
                .stream()
                .map(u -> PublicMentorDTO.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .build()
                ).toList();
    }

}
