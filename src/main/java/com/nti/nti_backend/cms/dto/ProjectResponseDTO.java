package com.nti.nti_backend.cms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class ProjectResponseDTO {
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private String fundingAmount;
    private String statusLabel;
    private Integer sortOrder;
    private boolean published;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
