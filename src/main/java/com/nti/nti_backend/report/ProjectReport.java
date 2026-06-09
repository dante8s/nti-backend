package com.nti.nti_backend.report;

import com.nti.nti_backend.application.Application;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    private String projectName;

    @Column(nullable = false, length = 50)
    private String programType;

    private String teamLeaderName;

    @Column(columnDefinition = "TEXT")
    private String teamMembers;

    private String productOwnerName;

    @Column(nullable = false)
    private LocalDateTime completedAt;

    private Double kpiScore;

    @Column(columnDefinition = "TEXT")
    private String kpiDetails;

    @Column(columnDefinition = "TEXT")
    private String resultDocuments;

    @Column(nullable = false)
    @Builder.Default
    private Integer milestonesTotal = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer milestonesDone = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
