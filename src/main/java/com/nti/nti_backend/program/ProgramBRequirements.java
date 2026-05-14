package com.nti.nti_backend.program;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "program_b_requirements")
@Getter
@Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProgramBRequirements {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional =  false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @Column(name = "specification_name")
    private String specificationName;

    @Column(name = "specification_path")
    private String specificationPath;

    @Column(name = "budget_name")
    private String budgetName;

    @Column(name = "budget_path")
    private String budgetPath;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
