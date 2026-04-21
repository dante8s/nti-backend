package com.nti.nti_backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @Email(message = "Невірний формат email")
        @NotBlank(message = "Email обов'язковий")
        String email,

        @NotBlank(message = "Пароль обов'язковий")
        String password,

        @NotBlank(message = "Підтвердіть що ви не робот")
        String captchaToken
) {}