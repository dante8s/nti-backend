package com.nti.nti_backend.milestone.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class MilestoneCommentDTO {
    private UUID id;
    private String content;
    private Long createdById;
    private String createdByName;
    private OffsetDateTime createdAt;
}
