package com.nti.nti_backend.milestone.dto;

import com.nti.nti_backend.milestone.entity.MilestoneStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class MilestoneResponseDTO {

    private UUID id;
    private Long applicationId;
    private UUID mentorshipId;
    private String title;
    private String description;
    private LocalDate dueDate;
    private OffsetDateTime completedAt;

    private MilestoneStatus status;

    private Long createdById;
    private String createdByName;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
