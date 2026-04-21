package com.nti.nti_backend.organization.dto;

import com.nti.nti_backend.organization.entity.OrgMemberRole;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class OrgMemberDTO {
    private UUID id;
    private Long userId;
    private String userName;
    private String userEmail;
    private OrgMemberRole role;
    private OffsetDateTime joinedAt;
}
