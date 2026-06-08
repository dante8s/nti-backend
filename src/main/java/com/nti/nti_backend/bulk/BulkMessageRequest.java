package com.nti.nti_backend.bulk;

public record BulkMessageRequest(
        String targetType,        // ALL | BY_ROLE | BY_CALL | BY_APPLICATION_STATUS
        String roleFilter,        // required when targetType = BY_ROLE
        Long   callId,            // required when targetType = BY_CALL
        String applicationStatus, // required when targetType = BY_APPLICATION_STATUS
        String subject,
        String message,
        boolean sendEmail         // true = in-app + email; false = in-app only
) {}
