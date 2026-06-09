package com.nti.nti_backend.cms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "cms_page_sections")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CmsPageSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // which page: 'about', 'home', 'programs-a', 'programs-b', etc.
    @Column(name = "page_key", nullable = false, length = 100)
    private String pageKey;

    // what kind: 'hero', 'card', 'text-block', 'header', 'pillar'
    @Builder.Default
    @Column(name = "section_type", nullable = false, length = 50)
    private String sectionType = "block";

    @Column(length = 500)
    private String title;

    @Column(length = 500)
    private String subtitle;

    @Column(columnDefinition = "TEXT")
    private String content;  // HTML

    @Column(length = 100)
    private String icon;  // emoji or icon identifier

    @Column(name = "image_path", length = 512)
    private String imagePath;

    @Column(name = "image_name", length = 255)
    private String imageName;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Builder.Default
    private boolean published = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
