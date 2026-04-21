package com.nti.nti_backend.call;

import java.time.LocalDateTime;

public record CallDTO(
        Long id,
        String title,
        Long programId,
        String programName,
        LocalDateTime deadline,
        String status,
        String evaluationCriteria
) {}