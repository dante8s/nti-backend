package com.nti.nti_backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(

        @NotBlank(message = "Token is required")
        String token,

        @Size(min = 6, message = "Minimum 6 characters")
        @NotBlank(message = "Password is required")
        String newPassword
) {}