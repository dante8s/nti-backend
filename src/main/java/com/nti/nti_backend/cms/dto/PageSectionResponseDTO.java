package com.nti.nti_backend.cms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class PageSectionResponseDTO {
    private Long id;
    private String pageKey;
    private String sectionType;
    private String title;
    private String subtitle;
    private String content;
    private String icon;
    private String imageUrl;
    private Integer sortOrder;
    private boolean published;
    private OffsetDateTime updatedAt;
}
