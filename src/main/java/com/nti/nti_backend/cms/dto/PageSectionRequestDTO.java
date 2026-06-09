package com.nti.nti_backend.cms.dto;

import lombok.Data;

@Data
public class PageSectionRequestDTO {
    private String sectionType;
    private String title;
    private String subtitle;
    private String content;
    private String icon;
    private Integer sortOrder;
    private boolean published = true;
}
