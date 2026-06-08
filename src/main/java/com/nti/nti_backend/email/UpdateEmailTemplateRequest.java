package com.nti.nti_backend.email;

import jakarta.validation.constraints.NotBlank;

public record UpdateEmailTemplateRequest(
        @NotBlank String subject,
        @NotBlank String body
) {}
