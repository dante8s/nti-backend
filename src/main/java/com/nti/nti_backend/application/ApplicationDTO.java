package com.nti.nti_backend.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ApplicationDTO(
        Long id,
        Long applicantId,
        Long callId,
        String callTitle,
        String programName,
        String programType,
        String status,
        String adminComment,
        UUID organizationId,
        String organizationName,
        Long productOwnerId,
        String productOwnerName,
        String formData,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<MemberSnapshotDTO> teamMembers
) {
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
            include = JsonTypeInfo.As.PROPERTY,
            property = "@class")
    public record MemberSnapshotDTO(Long userId, String email, String role) {}
}