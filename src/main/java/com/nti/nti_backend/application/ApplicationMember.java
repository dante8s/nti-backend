package com.nti.nti_backend.application;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "application_members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String role;
}
