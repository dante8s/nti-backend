package com.nti.nti_backend.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicOrganizationDTO {
    private UUID id;
    private String name;
    private String sector;
    private String website;
    private String description;
}
