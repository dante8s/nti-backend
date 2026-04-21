package com.nti.nti_backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(

        @Email(message = "Невірний формат email")
        @NotBlank(message = "Email обов'язковий")
        String email
) {}