package com.nti.nti_backend.cms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectRequestDTO {
    @NotBlank
    private String title;
    private String description;
    private String fundingAmount;
    private String statusLabel;
    private Integer sortOrder;
    private boolean published = true;
}