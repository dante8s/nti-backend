package com.nti.nti_backend.program;

public record ReviewProgramRequest(
        String status,
        String adminComment
) {
}
