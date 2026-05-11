package com.nti.nti_backend.evaluation;

import com.nti.nti_backend.application.Application;
import com.nti.nti_backend.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "evaluations")
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluator_id", nullable = false)
    private User evaluator;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criteria_id", nullable = false)
    private Criteria criteria;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation", length = 20)
    private Recommendation recommendation;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        evaluatedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Recommendation {
        APPROVE,
        REJECT,
        REQUEST_CHANGES,
        ABSTAIN
    }

    public Long getId() {
        return id;
    }

    public Application getApplication() {
        return application;
    }

    public User getEvaluator() {
        return evaluator;
    }

    public Criteria getCriteria() {
        return criteria;
    }

    public Double getScore() {
        return score;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Recommendation getRecommendation() {
        return recommendation;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public void setEvaluator(User evaluator) {
        this.evaluator = evaluator;
    }

    public void setCriteria(Criteria criteria) {
        this.criteria = criteria;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setRecommendation(Recommendation recommendation) {
        this.recommendation = recommendation;
    }

    public void setEvaluatedAt(LocalDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
