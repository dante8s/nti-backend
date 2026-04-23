package com.nti.nti_backend.criteria;

import com.nti.nti_backend.call.Call;
import jakarta.persistence.*;

@Entity
@Table(name = "criteria")
public class Criteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "call_id", nullable = false)
    private Call call;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "weight_percent")
    private Integer weightPercent;

    @Column(name = "max_score")
    private Double maxScore = 10.0;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    public Long getId() {
        return id;
    }

    public Call getCall() {
        return call;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getWeightPercent() {
        return weightPercent;
    }

    public Double getMaxScore() {
        return maxScore;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCall(Call call) {
        this.call = call;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setWeightPercent(Integer weightPercent) {
        this.weightPercent = weightPercent;
    }

    public void setMaxScore(Double maxScore) {
        this.maxScore = maxScore;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
