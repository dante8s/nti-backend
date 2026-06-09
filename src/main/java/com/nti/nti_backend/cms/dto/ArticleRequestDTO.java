package com.nti.nti_backend.cms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ArticleRequestDTO {
    @NotBlank
    private String title;
    private String category;
    private String excerpt;
    @NotBlank
    private String content;
}
