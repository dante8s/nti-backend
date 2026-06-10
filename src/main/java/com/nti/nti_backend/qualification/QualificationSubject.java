package com.nti.nti_backend.qualification;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "qualification_subjects")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QualificationSubject {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stack_id", nullable = false)
    private QualificationStack stack;

    @Column(name = "subject_name", nullable = false)
    private String subjectName;

    @Column(name = "position")
    private int position;
}
