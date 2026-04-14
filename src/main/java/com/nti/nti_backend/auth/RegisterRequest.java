package com.nti.nti_backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record RegisterRequest(

        @NotBlank(message = "Ім'я обов'язкове")
        String name,

        @Email(message = "Невірний формат email")
        @NotBlank(message = "Email обов'язковий")
        String email,

        @Size(min = 6, message = "Мінімум 6 символів")
        @NotBlank(message = "Пароль обов'язковий")
        String password,

        @NotEmpty(message = "Оберіть хоча б одну роль")
        Set<String> roles,

        boolean gdprConsent,

        // Токен від Google reCAPTCHA
        @NotBlank(message = "Підтвердіть що ви не робот")
        String captchaToken
) {}