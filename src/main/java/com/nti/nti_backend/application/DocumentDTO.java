package com.nti.nti_backend.application;

import java.time.LocalDateTime;

public record DocumentDTO(
        Long id,
        String fileName,
        String fileType,
        String documentType,
        String documentLabel,
        LocalDateTime uploadedAt
) {}