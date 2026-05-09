package com.nti.nti_backend.mentorship.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PublicMentorDTO {
    private Long id;
    private String name;
}
