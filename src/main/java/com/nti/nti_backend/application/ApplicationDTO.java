package com.nti.nti_backend.application;

import java.time.LocalDateTime;

public record ApplicationDTO(
        Long id,
        Long callId,
        String callTitle,
        String programName,
        String programType,
        String status,
        String adminComment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}