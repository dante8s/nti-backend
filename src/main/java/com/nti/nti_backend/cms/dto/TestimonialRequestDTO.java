package com.nti.nti_backend.cms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TestimonialRequestDTO {
    @NotBlank
    private String quote;
    @NotBlank private String authorName;
    private String authorRole;
    private Integer sortOrder;
    private boolean published = true;
}