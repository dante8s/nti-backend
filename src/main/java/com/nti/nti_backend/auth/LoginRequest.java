package com.nti.nti_backend.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LoginRequest(

        @Email(message = "Невірний формат email")
        @NotBlank(message = "Email обов'язковий")
        String email,

        @NotBlank(message = "Пароль обов'язковий")
        String password,

        @NotBlank(message = "Підтвердіть що ви не робот")
        String captchaToken
) {}