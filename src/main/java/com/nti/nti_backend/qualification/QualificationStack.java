package com.nti.nti_backend.qualification;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "qualification_stacks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QualificationStack {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stack_number")
    private int stackNumber;

    @Column(name = "specialization_key", unique = true)
    private String specializationKey;

    @Column(name = "specialization_name")
    private String specializationName;

    @OneToMany(mappedBy = "stack", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<QualificationSubject> subjects = new ArrayList<>();
}
