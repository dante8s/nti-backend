package com.nti.nti_backend.milestone;

import com.nti.nti_backend.application.Application;
import com.nti.nti_backend.application.ApplicationRepository;
import com.nti.nti_backend.file.FileServeService;
import com.nti.nti_backend.mentorship.dto.MentorshipResponseDTO;
import com.nti.nti_backend.mentorship.entity.MentorshipStatus;
import com.nti.nti_backend.mentorship.repository.MentorshipRepository;
import com.nti.nti_backend.milestone.dto.*;
import com.nti.nti_backend.milestone.entity.Milestone;
import com.nti.nti_backend.milestone.entity.MilestoneAttachment;
import com.nti.nti_backend.milestone.entity.MilestoneComment;
import com.nti.nti_backend.milestone.entity.MilestoneStatus;
import com.nti.nti_backend.organization.entity.OrgMemberRole;
import com.nti.nti_backend.organization.entity.Organization;
import com.nti.nti_backend.organization.exception.ConflictException;
import com.nti.nti_backend.organization.exception.ResourceNotFoundException;
import com.nti.nti_backend.organization.repository.OrgMemberRepository;
import com.nti.nti_backend.program.ProgramType;
import com.nti.nti_backend.user.Role;
import com.nti.nti_backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.nti.nti_backend.config.CacheNames.*;
import org.springframework.cache.annotation.*;
@Service
@RequiredArgsConstructor
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    //private final MentorshipRepository mentorshipRepository;
    private final ApplicationRepository applicationRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final MentorshipRepository mentorshipRepository;
    private final MilestoneAttachmentRepository attachmentRepository;
    private final FileServeService fileServeService;

    private static final int MAX_ATTACHMENTS = 10;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

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

    private final MilestoneCommentRepository milestoneCommentRepository;

    private boolean isAdminOrSuperAdmin(User user) {
        return user.hasRole(Role.ADMIN) || user.hasRole(Role.SUPER_ADMIN);
    }

    private boolean isSuperAdmin(User user) {
        return user.hasRole(Role.SUPER_ADMIN);
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
    @CacheEvict(value = MILESTONES_PENDING, allEntries = true)
    @Transactional
    public MilestoneResponseDTO create(MilestoneRequestDTO dto) {
        User currentUser = getCurrentUser();

        boolean isAdminOrSuper = isAdminOrSuperAdmin(currentUser);
        boolean isStudent = currentUser.hasRole(Role.STUDENT);
        boolean isFirm = currentUser.hasRole(Role.FIRM);

        Application application = null;
        MilestoneStatus initialStatus;

        if (dto.getApplicationId() != null) {
            application = applicationRepository.findById(dto.getApplicationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Application not found" + dto.getApplicationId()));
            ProgramType programType = application.getCall()
                    .getProgram().getType();
            if (isAdminOrSuper) {
                initialStatus = MilestoneStatus.PLANNED;
            } else if (isStudent && programType == ProgramType.PROGRAM_A) {
                boolean isTeamLeader = application.getApplicant().getId()
                        .equals(currentUser.getId());
                if (!isTeamLeader) {
                    throw new ConflictException(
                            "Only the team leader can create milestoens for Program A"
                    );
                }
                initialStatus = MilestoneStatus.PENDING_APPROVAL;
            } else if (isFirm && programType == ProgramType.PROGRAM_B) {
                Organization programOrg = application.getCall()
                        .getProgram().getOrganization();
                if (programOrg == null) {
                    throw new ConflictException(
                            "This Program B has no associated organization"
                    );
                }
                boolean ownsOrg = orgMemberRepository
                        .findAllByUserId(currentUser.getId())
                        .stream()
                        .filter(m -> m.getRole() == OrgMemberRole.OWNER)
                        .anyMatch(m -> m.getOrganization().getId()
                                .equals(programOrg.getId()));
                if (!ownsOrg) {
                    throw new ConflictException(
                            "Only the organization owner can create milestones for Program B"
                    );
                }
                initialStatus = MilestoneStatus.PENDING_APPROVAL;
            } else {
                throw new ConflictException(
                        "You do not have permission to create milestones for this application"
                );
            }
        } else {
            if (!isAdminOrSuper) {
                throw new ConflictException(
                        "Only ADMIN or SUPER_ADMIN can create milestones without an application"
                );
            }
            initialStatus = MilestoneStatus.PLANNED;
        }

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

        return results.stream().map(this::toResponseDTO).collect(Collectors.toList());

    }

    // FIND BY ID

    @Cacheable(value = MILESTONE, key = "#id")
    @Transactional(readOnly = true)
    public MilestoneResponseDTO findById(UUID id) {
        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Milestone not foujnd: " + id
                ));
        return toResponseDTO(milestone);
    }

    // UPDATE content
    @Caching(evict = {
            @CacheEvict(value = MILESTONE, key = "#id"),
            @CacheEvict(value = MILESTONES_PENDING, allEntries = true)
    })
    @Transactional
    public MilestoneResponseDTO update(UUID id, MilestoneRequestDTO dto) {
        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Milestone not found: " + id
                ));

        User currentUser = getCurrentUser();
        boolean isAdminOrSuper = isAdminOrSuperAdmin(currentUser);

        if (milestone.getStatus() == MilestoneStatus.COMPLETED) {
            throw new ConflictException("Connect edit a COMPLETED milestone");
        }

        if (isAdminOrSuper) {

        } else {
            boolean isTeamLeader = milestone.getApplication() != null
                    && milestone.getApplication().getApplicant().getId()
                    .equals(currentUser.getId());
            if (!isTeamLeader) {
                throw new ConflictException(
                        "You do not have permission to edit this milestone"
                );
            }
            if (milestone.getStatus() != MilestoneStatus.PENDING_APPROVAL) {
                throw new ConflictException(
                        "Students can only edit milestones that are pending approval"
                );
            }
        }

        milestone.setTitle(dto.getTitle());
        milestone.setDescription(dto.getDescription());
        milestone.setDueDate(dto.getDueDate());
        return toResponseDTO(milestoneRepository.save(milestone));

    }

    @Caching(evict = {
            @CacheEvict(value = MILESTONE, key = "#id"),
            @CacheEvict(value = MILESTONES_PENDING, allEntries = true)
    })
    @Transactional
    public void deleteMilestone(UUID id) {
        if (!isAdminOrSuperAdmin(getCurrentUser())) {
            throw new ConflictException(
                    "Only ADMIN or SUPER_ADMIN can delete milestones");
        }
        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Milestone not found: " + id));
        milestoneRepository.delete(milestone);
    }

    @Transactional
    public MilestoneCommentDTO addComment(UUID milestoneId,
                                          MilestoneCommentRequest request,
                                          User currentUser) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Milestone not found: " + milestoneId
                ));
        MilestoneComment comment = MilestoneComment.builder()
                .milestone(milestone)
                .content(request.getContent())
                .createdBy(currentUser)
                .build();

        MilestoneComment saved = milestoneCommentRepository.save(comment);
        return toCommentDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<MilestoneCommentDTO> getComments(UUID milestoneId) {
        if (!milestoneRepository.existsById(milestoneId)) {
            throw new ResourceNotFoundException("Milestone not found: " + milestoneId);
        }
        return milestoneCommentRepository
                .findAllByMilestoneIdOrderByCreatedAtDesc(milestoneId)
                .stream().map(this::toCommentDTO).collect(Collectors.toList());
    }

    @Transactional
    public void deleteComment(UUID milestoneId, UUID commentId, User currentUser) {
        MilestoneComment comment = milestoneCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Comment not found: " + commentId
                ));

        if (!comment.getMilestone().getId().equals(milestoneId)) {
            throw new ConflictException("Comment does not belong to this milestone");
        }

        if (!comment.getCreatedBy().getId().equals(currentUser.getId()) && !currentUser.hasRole(Role.ADMIN)) {
            throw new ConflictException("You can only delete your own comments");
        }

        milestoneCommentRepository.delete(comment);
    }

    @Transactional
    public MilestoneAttachmentDTO addAttachment(
            UUID milestoneId, MultipartFile file, User currentUser
    ) {
        if (file == null || file.isEmpty()) {
            throw new ConflictException("No file provided for upload.");
        }

        long MAX_FILE_SIZE = 5 * 1024 * 1024;
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ConflictException("File size exceeds the 5MB limit.");
        }

    Milestone milestone = milestoneRepository.findById(milestoneId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Milestone not found: " + milestoneId
            ));

    boolean isAdminOrSuper = isAdminOrSuperAdmin(currentUser);
    boolean isTeamLeader = milestone.getApplication() != null
            && milestone.getApplication().getApplicant().getId()
            .equals(currentUser.getId());
    boolean isAssignedMentor = currentUser.hasRole(Role.MENTOR)
            && milestone.getApplication() != null
            && mentorshipRepository.existsByMentorIdAndApplication_IdAndStatus(
                    currentUser.getId(),
            milestone.getApplication().getId(),
            MentorshipStatus.ACTIVE
    );

    if (!isAdminOrSuper && !isTeamLeader && !isAssignedMentor) {
        throw new ConflictException(
                "Only the team leader, assigned mentor, or ADMIN can add attachments to this milestone"
        );
    }

    long currentCount = attachmentRepository.countByMilestoneId(milestoneId);
    if (currentCount >= MAX_ATTACHMENTS) {
        throw new ConflictException(
                "Milestone already has the maximum of " + MAX_ATTACHMENTS + " attachments"
        );
    }

    String path = saveAttachmentFile(file, milestoneId);

    MilestoneAttachment attachment = MilestoneAttachment.builder()
            .milestone(milestone)
            .fileName(file.getOriginalFilename())
            .filePath(path)
            .uploadedBy(currentUser)
            .build();

    MilestoneAttachment saved = attachmentRepository.save(attachment);
    return toAttachmentDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<MilestoneAttachmentDTO> getAttachments(UUID milestoneId) {
        if (!milestoneRepository.existsById(milestoneId)) {
            throw new ResourceNotFoundException("Milestone not found: " + milestoneId);
        }
        return attachmentRepository.findAllByMilestoneId(milestoneId)
                .stream().map(this::toAttachmentDTO).collect(Collectors.toList());
    }

    @Transactional
    public void deleteAttachment(UUID milestoneId, UUID attachmentId, User currentUser) {
        MilestoneAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found: " + attachmentId));

        if (!attachment.getMilestone().getId().equals(milestoneId)) {
            throw new ConflictException("Attachment does not belong to this milestone");
        }

        boolean isOwner = attachment.getUploadedBy().getId()
                .equals(currentUser.getId());
        boolean isAdminOrSuper = isAdminOrSuperAdmin(currentUser);

        if (!isOwner && !isAdminOrSuper) {
            throw new ConflictException(
                    "You can only delete your own attachments"
            );
        }

        attachmentRepository.delete(attachment);
    }

    public ResponseEntity<Resource> serveAttachment(
           UUID milestoneId, UUID attachmentId,
           boolean inline, User currentUser
    ) {
        MilestoneAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found: " + attachmentId));
        if (!attachment.getMilestone().getId().equals(milestoneId)) {
            throw new ConflictException(
                    "Attachment does not belong to this milestone"
            );
        }

        boolean hasAccess = currentUser.hasRole(Role.ADMIN)
                || currentUser.hasRole(Role.STUDENT)
                || currentUser.hasRole(Role.MENTOR)
                || currentUser.hasRole(Role.FIRM);

        if (!hasAccess) {
            throw new ConflictException(
                    "You do not have access to this attachment"
            );
        }

        Resource resource = fileServeService.load(attachment.getFilePath());
        String contentType = fileServeService.detectContentType(attachment.getFileName());
        String disposition = fileServeService.contentDisposition(
                inline, attachment.getFileName()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    //  GET pending approval
    @Cacheable(MILESTONES_PENDING)
    @Transactional(readOnly = true)
    public List<MilestoneResponseDTO> getPendingApproval() {
        User currentUser = getCurrentUser();
        if (!isAdminOrSuperAdmin(currentUser)) {
            throw new ConflictException("Only ADMIN can view pending mielstones");
        }
        return milestoneRepository.findAllByStatus(MilestoneStatus.PENDING_APPROVAL)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // CHANGE STATUS
    @Caching(evict = {
            @CacheEvict(value = MILESTONE, key = "#id"),
            @CacheEvict(value = MILESTONES_PENDING, allEntries = true)
    })
    @Transactional
    public MilestoneResponseDTO changeStatus(UUID id, ChangeStatusRequestDTO dto) {
        Milestone milestone = milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Milestone not found: " + id
                ));

        User currentUser = getCurrentUser();
        boolean isAdminOrSuper = isAdminOrSuperAdmin(currentUser);
        MilestoneStatus newStatus = dto.getStatus();
        MilestoneStatus effectiveCurrent = resolveStatus(milestone);

        Set<MilestoneStatus> allowed = ALLOWED_TRANSITIONS
                .getOrDefault(effectiveCurrent, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new ConflictException(
                    "Transition from " + effectiveCurrent + " to " + newStatus
                    + " is not allowed"
            );
        }

        if (effectiveCurrent == MilestoneStatus.PENDING_APPROVAL) {
            if (!isAdminOrSuper) {
                throw new ConflictException(
                        "Only ADMIN or SUPER_ADMIN can approve milestones"
                );
            }
        } else if (effectiveCurrent == MilestoneStatus.OVERDUE) {

            if (!isAdminOrSuper) {
                throw new ConflictException(
                        "Only ADMIN or SUPER_ADMIN can change the status of OVERDUE milestones"
                );
            }
        } else {
            if (!isAdminOrSuper) {
                boolean isAssignedMentor = currentUser.hasRole(Role.MENTOR)
                        && milestone.getApplication() != null
                        && mentorshipRepository
                        .existsByMentorIdAndApplication_IdAndStatus(
                                currentUser.getId(),
                                milestone.getApplication().getId(),
                                MentorshipStatus.ACTIVE
                        );
                if (!isAssignedMentor) {
                    throw new ConflictException(
                            "Only the assigned mentor, ADMIN, or SUPER_ADMIN can change milestone status"
                    );
                }
            }
        }

        if (newStatus == MilestoneStatus.COMPLETED) {
            milestone.setCompletedAt(OffsetDateTime.now());
        }
        milestone.setStatus(newStatus);
        return toResponseDTO(milestoneRepository.save(milestone));

    }

    private MilestoneCommentDTO toCommentDTO(MilestoneComment c) {
        return MilestoneCommentDTO.builder()
                .id(c.getId())
                .content(c.getContent())
                .createdById(c.getCreatedBy().getId())
                .createdByName(c.getCreatedBy().getName())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private String saveAttachmentFile(MultipartFile file, UUID milestoneId) {
        try {
            String filename = "milestone_" + milestoneId + "_"
                    + System.currentTimeMillis() + "_"
                    + file.getOriginalFilename();
            Path uploadPath = Paths.get(uploadDir, "milestones");
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath,
                    StandardCopyOption.REPLACE_EXISTING);
            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store attachment: " + e.getMessage());
        }
    }

    private MilestoneAttachmentDTO toAttachmentDTO(MilestoneAttachment a) {
        return MilestoneAttachmentDTO.builder()
                .id(a.getId())
                .fileName(a.getFileName())
                .filePath(a.getFilePath())
                .uploadedById(a.getUploadedBy().getId())
                .uploadedByName(a.getUploadedBy().getName())
                .uploadedAt(a.getUploadedAt())
                .build();
    }
}
