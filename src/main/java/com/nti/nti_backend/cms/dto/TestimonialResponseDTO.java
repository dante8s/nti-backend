package com.nti.nti_backend.cms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class TestimonialResponseDTO {
    private Long id;
    private String quote;
    private String authorName;
    private String authorRole;
    private String avatarUrl;
    private Integer sortOrder;
    private boolean published;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
