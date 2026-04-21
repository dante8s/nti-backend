package com.nti.nti_backend.mentorship.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddNoteRequestDTO {
    @NotNull(message = "Application ID is required")
    private Long applicationId;

    @NotBlank(message = "Note content cannot be empty")
    private String content;
}
