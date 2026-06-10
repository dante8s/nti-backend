package com.nti.nti_backend.call;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateCallRequest(

        @NotBlank(message = "Title is required")
        String title,

        @NotNull(message = "Deadline is required")
        @Future(message = "Deadline must be in the future")
        LocalDateTime deadline,

        String evaluationCriteria
) {}