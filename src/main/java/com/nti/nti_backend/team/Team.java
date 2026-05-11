package com.nti.nti_backend.team;

import com.nti.nti_backend.teamMember.TeamMember;
import com.nti.nti_backend.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name" , nullable = false , length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY , optional = false)
    @JoinColumn(name = "leader_id" , nullable = false)
    private User leader;

    @Column(name = "max_capacity")
    private Integer maxCapacity = 5;

    @Column(name = "description" , columnDefinition = "TEXT")
    private String desciption;

    @Column(name = "competencies" , columnDefinition = "TEXT")
    private String competencies;

    @Column(name = "created_at" , nullable = false , updatable = false)
    private LocalDateTime created_at;

    @Column(name = "updated_at")
    private LocalDateTime updated_at;

    @OneToMany(mappedBy = "team" , cascade = CascadeType.ALL , orphanRemoval = true)
    private List<TeamMember> members = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        created_at = LocalDateTime.now();
        updated_at = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updated_at = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public User getLeader() {
        return leader;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public String getDesciption() {
        return desciption;
    }

    public String getCompetencies() {
        return competencies;
    }

    public LocalDateTime getCreated_at() {
        return created_at;
    }

    public LocalDateTime getUpdated_at() {
        return updated_at;
    }

    public List<TeamMember> getMembers() {
        return members;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLeader(User leader) {
        this.leader = leader;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public void setDesciption(String desciption) {
        this.desciption = desciption;
    }

    public void setCompetencies(String competencies) {
        this.competencies = competencies;
    }

    public void setCreated_at(LocalDateTime created_at) {
        this.created_at = created_at;
    }

    public void setUpdated_at(LocalDateTime updated_at) {
        this.updated_at = updated_at;
    }

    public void setMembers(List<TeamMember> members) {
        this.members = members;
    }
}
