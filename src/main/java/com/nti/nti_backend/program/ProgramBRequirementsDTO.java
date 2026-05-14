package com.nti.nti_backend.program;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class ProgramBRequirementsDTO {
    private Long id;
    private Long programId;
    private String specificationName;
    private String specificationPath;
    private String budgetName;
    private String budgetPath;
    private OffsetDateTime updatedAt;
}
