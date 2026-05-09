package com.nti.nti_backend.milestone.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MilestoneCommentRequest {
    @NotBlank(message = "Comment cannot be empty!")
    private String content;
}
