package com.nti.nti_backend.program;

public record ProgramDTO(
        Long id,
        String name,
        String description,
        String type,
        boolean isActive
) {}