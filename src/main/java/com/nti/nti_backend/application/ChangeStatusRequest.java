package com.nti.nti_backend.application;

import jakarta.validation.constraints.NotBlank;

public record ChangeStatusRequest(
        @NotBlank
        String status,
        String comment
) {}