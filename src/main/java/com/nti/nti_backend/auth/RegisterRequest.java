package com.nti.nti_backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record RegisterRequest(

        @NotBlank(message = "Name is required")
        String name,

        @Email(message = "Invalid email format")
        @NotBlank(message = "Email is required")
        String email,

        @Size(min = 6, message = "Minimum 6 characters")
        @NotBlank(message = "Password is required")
        String password,

        @NotEmpty(message = "Select at least one role")
        Set<String> roles,

        boolean gdprConsent,

        String captchaToken
) {}