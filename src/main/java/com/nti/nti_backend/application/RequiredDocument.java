package com.nti.nti_backend.application;

public record RequiredDocument(
        DocumentType type,
        String label,
        String description
) {}