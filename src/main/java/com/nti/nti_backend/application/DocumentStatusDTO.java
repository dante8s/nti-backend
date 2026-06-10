package com.nti.nti_backend.application;

public record DocumentStatusDTO(
        String documentType,
        String label,
        String description,
        boolean uploaded,
        String fileName,    // null if not uploaded
        Long documentId     // null if not uploaded
) {}