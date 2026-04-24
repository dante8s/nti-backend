package com.nti.nti_backend.milestone;

import com.nti.nti_backend.application.Application;
import com.nti.nti_backend.application.ApplicationRepository;
import com.nti.nti_backend.milestone.dto.ChangeStatusRequestDTO;
import com.nti.nti_backend.milestone.dto.MilestoneRequestDTO;
import com.nti.nti_backend.milestone.dto.MilestoneResponseDTO;
import com.nti.nti_backend.milestone.entity.Milestone;
import com.nti.nti_backend.milestone.entity.MilestoneStatus;
import com.nti.nti_backend.organization.exception.ConflictException;
import com.nti.nti_backend.organization.exception.ResourceNotFoundException;
import com.nti.nti_backend.user.Role;
import com.nti.nti_backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    //private final MentorshipRepository mentorshipRepository;
    private final ApplicationRepository applicationRepository;

    private static final Map<MilestoneStatus, Set<MilestoneStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(MilestoneStatus.class);
    static {
        ALLOWED_TRANSITIONS.put(
                MilestoneStatus.PENDING_APPROVAL, Set.of(
                        MilestoneStatus.PLANNED // admin-approval
                ));
        ALLOWED_TRANSITIONS.put(MilestoneStatus.PLANNED, Set.of(
                MilestoneStatus.IN_PROGRESS,
                MilestoneStatus.BLOCKED
        ));
        ALLOWED_TRANSITIONS.put(MilestoneStatus.IN_PROGRESS, Set.of(
                MilestoneStatus.COMPLETED,
                MilestoneStatus.BLOCKED,
                MilestoneStatus.PLANNED
        ));
        ALLOWED_TRANSITIONS.put(MilestoneStatus.BLOCKED, Set.of(
                MilestoneStatus.IN_PROGRESS,
                MilestoneStatus.PLANNED
        ));
        ALLOWED_TRANSITIONS.put(MilestoneStatus.OVERDUE, Set.of(
                MilestoneStatus.IN_PROGRESS,
                MilestoneStatus.COMPLETED
        ));
        ALLOWED_TRANSITIONS.put(MilestoneStatus.COMPLETED, Set.of());

    }

    // helpers
    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    private MilestoneStatus resolveStatus(Milestone milestone) {
        if (milestone.getStatus() == MilestoneStatus.COMPLETED ||
        milestone.getStatus() == MilestoneStatus.PENDING_APPROVAL) {
            return milestone.getStatus();
        }
        if (milestone.getDueDate().isBefore(LocalDate.now())) {
            return MilestoneStatus.OVERDUE;
        }
        return milestone.getStatus();
    }

    private MilestoneResponseDTO toResponseDTO(Milestone m) {
        return MilestoneResponseDTO.builder()
                .id(m.getId())
                .applicationId(m.getApplication() != null ?  m.getApplication().getId() : null)
                .mentorshipId(m.getMentorshipId())
                .title(m.getTitle())
                .description(m.getDescription())
                .dueDate(m.getDueDate())
                .completedAt(m.getCompletedAt())
                .status(m.getStatus())
                .createdById(m.getCreatedBy().getId())
                .createdByName(m.getCreatedBy().getName())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    // CREATE
    @Transactional
    public MilestoneResponseDTO create(MilestoneRequestDTO dto) {
        User currentUser = getCurrentUser();

        boolean isAdmin = currentUser.hasRole(Role.ADMIN);
        boolean isStudent = currentUser.hasRole(Role.STUDENT);

        if (!isAdmin && !isStudent) {
            throw new ConflictException("Only STUDENT or ADMIN can create milestones");
        }

        Application application = null;
        if (dto.getApplicationId() != null) {
            application = applicationRepository.findById(dto.getApplicationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Application not found: " + dto.getApplicationId()
                    ));

        }
        MilestoneStatus initialStatus = isAdmin ?
                MilestoneStatus.PLANNED
                : MilestoneStatus.PENDING_APPROVAL;

        Milestone milestone = Milestone.builder()
                .application(application)
                .mentorshipId(dto.getMentorshipId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .dueDate(dto.getDueDate())
                .status(initialStatus)
                .createdBy(currentUser)
                .build();

        return toResponseDTO(milestoneRepository.save(milestone));
    }

    // FIND ALL
    @Transactional(readOnly = true)
    public List<MilestoneResponseDTO> findAll(
            Long applicationId,
            UUID mentorshipId,
            Long createdById
    ) {
        List<Milestone> results;

        if (applicationId != null) {
            results = milestoneRepository.findAllByApplication_Id(applicationId);
        } else if (mentorshipId != null) {
            results = milestoneRepository.findAllByMentorshipId(mentorshipId);
        } else if (createdById != null) {
            results = milestoneRepository.findAllByCreatedById(createdById);
        } else {
            results = milestoneRepository.findAll();
        }

        return results.stream().map(this::toResponseDTO).toList();

    }

    // FIND BY ID

    @Transactional(readOnly = true)
    public MilestoneResponseDTO findById(UUID id) {
        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Milestone not foujnd: " + id
                ));
        return toResponseDTO(milestone);
    }

    // UPDATE content
    @Transactional
    public MilestoneResponseDTO update(UUID id, MilestoneRequestDTO dto) {
        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Milestone not found: " + id
                ));

        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.hasRole(Role.ADMIN);
        boolean isCreator = milestone.getCreatedBy().getId().equals(currentUser.getId());

        // Can't edit COMPLETED milestone
        if (milestone.getStatus() == MilestoneStatus.COMPLETED) {
            throw new ConflictException(
                    "Cannot edit a COMPLETED milestone"
            );
        }

        if (isAdmin) {

        } else if (isCreator && milestone.getStatus() ==  MilestoneStatus.PENDING_APPROVAL) {

        } else {
            throw new ConflictException(
                    "You cannot edit this milestone. Once APPROVED, only admin can make changes"
            );
        }


        milestone.setTitle(dto.getTitle());
        milestone.setDescription(dto.getDescription());
        milestone.setDueDate(dto.getDueDate());

        return toResponseDTO(milestoneRepository.save(milestone));
    }

    //  GET pending approval
    @Transactional(readOnly = true)
    public List<MilestoneResponseDTO> getPendingApproval() {
        User currentUser = getCurrentUser();
        if (!currentUser.hasRole(Role.ADMIN)) {
            throw new ConflictException("Only ADMIN can view pending mielstones");
        }
        return milestoneRepository.findAllByStatus(MilestoneStatus.PENDING_APPROVAL)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // CHANGE STATUS
    @Transactional
    public MilestoneResponseDTO changeStatus(UUID id, ChangeStatusRequestDTO dto) {
        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Milestone not found: " + id
                ));

        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.hasRole(Role.ADMIN);
        boolean isMentor = currentUser.hasRole(Role.MENTOR);

        if (!isAdmin && !isMentor) {
            throw new ConflictException(
                    "Only ADMIN or MENTOR can change milestone status"
            );
        }

        MilestoneStatus newStatus = dto.getStatus();

        MilestoneStatus effectiveCurrentStatus = resolveStatus(milestone);

        if (effectiveCurrentStatus == MilestoneStatus.OVERDUE && !isAdmin) {
            throw new ConflictException("Only ADMIN can change OVERDUE status");
        }

        Set<MilestoneStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(
                effectiveCurrentStatus,
                Set.of()
        );

        if (!allowed.contains(newStatus)) {
            throw new ConflictException(
                    "Transition from " + effectiveCurrentStatus + " to " + newStatus +
                            " is not allowed"
            );
        }

        if (newStatus == MilestoneStatus.COMPLETED) {
            milestone.setCompletedAt(OffsetDateTime.now());
        }

        milestone.setStatus(newStatus);

        return toResponseDTO(milestoneRepository.save(milestone));
    }
}
