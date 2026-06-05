package com.nti.nti_backend.milestone;

import com.nti.nti_backend.application.Application;
import com.nti.nti_backend.application.ApplicationRepository;
import com.nti.nti_backend.file.FileServeService;
import com.nti.nti_backend.milestone.dto.*;
import com.nti.nti_backend.milestone.entity.Milestone;
import com.nti.nti_backend.milestone.entity.MilestoneAttachment;
import com.nti.nti_backend.milestone.entity.MilestoneComment;
import com.nti.nti_backend.milestone.entity.MilestoneStatus;
import com.nti.nti_backend.organization.exception.ConflictException;
import com.nti.nti_backend.organization.exception.ResourceNotFoundException;
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

@Service
@RequiredArgsConstructor
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    //private final MentorshipRepository mentorshipRepository;
    private final ApplicationRepository applicationRepository;
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
        boolean isFirm = currentUser.hasRole(Role.FIRM);

        if (!isAdmin && !isStudent && !isFirm) {
            throw new ConflictException("Only STUDENT or ADMIN or ORGANIZATION can create milestones");
        }

        Application application = null;
        if (dto.getApplicationId() != null) {
            application = applicationRepository.findById(dto.getApplicationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Application not found: " + dto.getApplicationId()
                    ));

        }
        if (isFirm) {
            ProgramType type = application.getCall()
                    .getProgram().getType();
            if (type != ProgramType.PROGRAM_B) {
                throw new ConflictException(
                        "Organizations can only add milestones to Program B"
                );
            }
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
                .stream().map(this::toCommentDTO).toList();
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
    boolean canAttach = currentUser.hasRole(Role.ADMIN)
            || currentUser.hasRole(Role.STUDENT)
            || currentUser.hasRole(Role.MENTOR);
    if (!canAttach) {
        throw new ConflictException(
                "Only MENTOR, STUDENT or ADMIN can add attachments"
        );
    }

    Milestone milestone = milestoneRepository.findById(milestoneId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Milestone not found: " + milestoneId
            ));
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
                .stream().map(this::toAttachmentDTO).toList();
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
        boolean isAdmin = currentUser.hasRole(Role.ADMIN);

        if (!isOwner && !isAdmin) {
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
