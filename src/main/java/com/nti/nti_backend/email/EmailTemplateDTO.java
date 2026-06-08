package com.nti.nti_backend.email;

import java.time.LocalDateTime;
import java.util.List;

public record EmailTemplateDTO(
        Long id,
        String type,
        String subject,
        String body,
        List<String> variables,
        LocalDateTime updatedAt
) {}
