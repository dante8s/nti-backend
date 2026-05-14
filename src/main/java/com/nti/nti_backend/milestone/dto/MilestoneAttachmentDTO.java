package com.nti.nti_backend.milestone.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class MilestoneAttachmentDTO {
    private UUID id;
    private String fileName;
    private String filePath;
    private Long uploadedById;
    private String uploadedByName;
    private OffsetDateTime uploadedAt;
}
