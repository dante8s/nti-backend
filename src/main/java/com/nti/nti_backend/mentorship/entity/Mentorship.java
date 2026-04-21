package com.nti.nti_backend.mentorship.entity;

import com.nti.nti_backend.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "mentorships")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Mentorship {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mentor_user_id", nullable = false)
    private User mentor;

    @Column(name = "application_id")
    private Long applicationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MentorshipStatus status = MentorshipStatus.ACTIVE;

    @Column(name = "start_date", nullable = false)
    @Builder.Default
    private OffsetDateTime startDate = OffsetDateTime.now();

    @Column(name = "end_date")
    private OffsetDateTime endDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
