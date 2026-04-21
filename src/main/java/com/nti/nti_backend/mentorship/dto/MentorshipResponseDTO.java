package com.nti.nti_backend.mentorship.dto;

import com.nti.nti_backend.mentorship.entity.MentorshipStatus;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class MentorshipResponseDTO {
    private UUID id;
    private Long mentorUserId;
    private String mentorName;
    private String mentorEmail;
    private Long applicationId;
    private MentorshipStatus status;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private OffsetDateTime createdAt;
}
