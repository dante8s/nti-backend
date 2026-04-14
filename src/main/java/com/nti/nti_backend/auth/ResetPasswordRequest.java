package com.nti.nti_backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(

        @NotBlank(message = "Токен обов'язковий")
        String token,

        @Size(min = 6, message = "Мінімум 6 символів")
        @NotBlank(message = "Пароль обов'язковий")
        String newPassword
) {}