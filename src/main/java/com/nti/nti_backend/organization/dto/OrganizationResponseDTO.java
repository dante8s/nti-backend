package com.nti.nti_backend.organization.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nti.nti_backend.organization.entity.OrgMemberRole;
import com.nti.nti_backend.organization.entity.OrgStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrganizationResponseDTO {
    private UUID id;
    private String name;
    private String ico;
    private String sector;
    private String description;
    private String contactEmail;
    private String contactPhone;
    private String website;
    private OrgStatus status;
    private List<OrgMemberDTO> members;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
