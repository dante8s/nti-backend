package com.nti.nti_backend.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrganizationRequestDTO {

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    @NotBlank(message = "ICO is required")
    @Size(max = 20)
    private String ico;

    @Size(max = 20)
    private String sector;

    private String description;

    @Email(message = "Must be a valid email")
    private String contactEmail;

    @Size(max = 50)
    private String contactPhone;

    private String website;
}
