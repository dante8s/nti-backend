package com.nti.nti_backend.application;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectHistoryDTO(
        ProjectEntryDTO current,
        List<ProjectEntryDTO> history
) {
    public record ProjectEntryDTO(
            Long applicationId,
            String status,
            String programName,
            String callTitle,
            String teamName,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<ApplicationDTO.MemberSnapshotDTO> members
    ) {}
}
