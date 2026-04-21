package com.nti.nti_backend.call;

import com.nti.nti_backend.program.Program;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "calls")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Call {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CallStatus status = CallStatus.OPEN;

    @Column(columnDefinition = "TEXT")
    private String evaluationCriteria;

    @CreationTimestamp
    private LocalDateTime createdAt;
}