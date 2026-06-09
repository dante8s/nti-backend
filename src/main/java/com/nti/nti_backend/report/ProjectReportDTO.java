package com.nti.nti_backend.report;

import java.time.LocalDateTime;

public record ProjectReportDTO(
        Long id,
        Long applicationId,
        String projectName,
        String programType,
        String teamLeaderName,
        String teamMembers,
        String productOwnerName,
        LocalDateTime completedAt,
        Double kpiScore,
        String kpiDetails,
        String resultDocuments,
        Integer milestonesTotal,
        Integer milestonesDone,
        LocalDateTime createdAt
) {}
