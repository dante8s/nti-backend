package com.nti.nti_backend.call;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateCallRequest(

        @NotBlank(message = "Назва обов'язкова")
        String title,

        @NotNull(message = "Дедлайн обов'язковий")
        @Future(message = "Дедлайн має бути в майбутньому")
        LocalDateTime deadline,

        String evaluationCriteria
) {}