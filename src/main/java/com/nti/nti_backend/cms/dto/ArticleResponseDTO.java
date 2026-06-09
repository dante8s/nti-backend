package com.nti.nti_backend.cms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class ArticleResponseDTO {
    private Long id;
    private String title;
    private String category;
    private String excerpt;
    private String content;
    private String imageUrl;
    private boolean published;
    private OffsetDateTime publishedAt;
    private String createdByName;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
