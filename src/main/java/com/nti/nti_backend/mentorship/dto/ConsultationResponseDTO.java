package com.nti.nti_backend.mentorship.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class ConsultationResponseDTO {
    private UUID id;
    private UUID mentorshipId;
    private Long mentorId;
    private String mentorName;
    private LocalDate consultationDate;
    private String topic;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
