package com.nti.nti_backend.application;

public record DocumentStatusDTO(
        String documentType,
        String label,
        String description,
        boolean uploaded,
        String fileName,    // null якщо не завантажено
        Long documentId     // null якщо не завантажено
) {}