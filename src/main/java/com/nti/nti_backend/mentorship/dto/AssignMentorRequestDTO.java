package com.nti.nti_backend.mentorship.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignMentorRequestDTO {

    @NotNull(message = "Mentor user ID is required")
    private Long mentorUserId;


    private Long applicationId;
}
