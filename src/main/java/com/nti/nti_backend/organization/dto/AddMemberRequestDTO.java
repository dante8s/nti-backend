package com.nti.nti_backend.organization.dto;

import com.nti.nti_backend.organization.entity.OrgMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddMemberRequestDTO {
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email")
    private String email;

    private OrgMemberRole role = OrgMemberRole.MEMBER;
}
