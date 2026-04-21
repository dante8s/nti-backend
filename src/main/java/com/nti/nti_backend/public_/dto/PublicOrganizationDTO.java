package com.nti.nti_backend.public_.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PublicOrganizationDTO {
    private UUID id;
    private String name;
    private String sector;
    private String website;
    private String description;
}
