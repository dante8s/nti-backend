package com.nti.nti_backend.mentorship.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class ConsultationRequestDTO {
    @NotNull
    private UUID mentorshipId;

    @NotNull
    private LocalDate consultationDate;

    @NotBlank
    private String topic;

    private String description;
}
