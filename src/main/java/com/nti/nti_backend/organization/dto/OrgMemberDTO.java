package com.nti.nti_backend.organization.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nti.nti_backend.organization.entity.OrgMemberRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrgMemberDTO {
    private UUID id;
    private Long userId;
    private String userName;
    private String userEmail;
    private OrgMemberRole role;
    private OffsetDateTime joinedAt;
}
