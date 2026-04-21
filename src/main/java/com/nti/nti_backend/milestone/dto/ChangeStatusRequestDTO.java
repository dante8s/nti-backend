package com.nti.nti_backend.milestone.dto;

import com.nti.nti_backend.milestone.entity.MilestoneStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeStatusRequestDTO {

    @NotNull(message = "Status is required")
    private MilestoneStatus status;
}
