package com.nti.nti_backend.application;

/**
 * Запит на оновлення чернетки заявки.
 * formData — JSON-рядок з довільними полями форми.
 */
public record UpdateApplicationRequest(
        String formData
) {}