package com.nti.nti_backend.application;

/**
 * Request to update an application draft.
 * formData — JSON string with arbitrary form fields.
 */
public record UpdateApplicationRequest(
        String formData
) {}