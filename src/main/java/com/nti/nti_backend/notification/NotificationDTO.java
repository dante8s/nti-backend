package com.nti.nti_backend.notification;

import java.time.LocalDateTime;

public record NotificationDTO(
        Long id,
        String type,
        String title,
        String message,
        String link,
        boolean read,
        LocalDateTime createdAt
) {}
