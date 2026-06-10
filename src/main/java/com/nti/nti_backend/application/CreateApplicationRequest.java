package com.nti.nti_backend.application;

import jakarta.validation.constraints.NotNull;

public record CreateApplicationRequest(

        @NotNull(message = "Call is required")
        Long callId
) {}