package com.nti.nti_backend.studentProfile;

import com.nti.nti_backend.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_profiles")
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ removed duplicate @Column user_id — the @JoinColumn already creates user_id in DB
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "study_program", length = 150)
    private String studyProgram;               // ✅ camelCase, not study_program

    @Column(name = "year_of_study")
    private Integer yearOfStudy;

    @Column(name = "skills", columnDefinition = "TEXT")
    private String skills;

    @Column(name = "cv_file_path", length = 512)
    private String cvFilePath;                 // ✅ was cv_path

    @Column(name = "cv_original_name", length = 255)
    private String cvOriginalName;

    @Column(name = "cv_uploaded_at", updatable = false)
    private LocalDateTime cvUploadedAt;

    @Column(name = "has_repeated_subjects")
    private boolean hasRepeatedSubjects = false;

    @Column(name = "profile_average_grade")
    private Double profileAverageGrade;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "profile_complete")
    private boolean profileComplete = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;           // ✅ was updated_at (snake_case in Java field)

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getStudyProgram() { return studyProgram; }
    public void setStudyProgram(String studyProgram) { this.studyProgram = studyProgram; }

    public Integer getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(Integer yearOfStudy) { this.yearOfStudy = yearOfStudy; }

    public String getSkills() { return skills; }

    public void setSkills(String skills) { this.skills = skills; }

    public String getCvFilePath() { return cvFilePath; }
    public void setCvFilePath(String cvFilePath) { this.cvFilePath = cvFilePath; }

    public String getCvOriginalName() { return cvOriginalName; }

    public void setCvOriginalName(String cvOriginalName) {
        this.cvOriginalName = cvOriginalName;
    }

    public LocalDateTime getCvUploadedAt() { return cvUploadedAt; }

    public void setCvUploadedAt(LocalDateTime cvUploadedAt) {
        this.cvUploadedAt = cvUploadedAt;
    }

    public boolean isHasRepeatedSubjects() { return hasRepeatedSubjects; }
    public void setHasRepeatedSubjects(boolean hasRepeatedSubjects) { this.hasRepeatedSubjects = hasRepeatedSubjects; }

    public Double getProfileAverageGrade() { return profileAverageGrade; }
    public void setProfileAverageGrade(Double profileAverageGrade) { this.profileAverageGrade = profileAverageGrade; }

    public String getBio() { return bio; }

    public void setBio(String bio) { this.bio = bio; }

    public boolean isProfileComplete() { return profileComplete; }

    public void setProfileComplete(boolean profileComplete) {
        this.profileComplete = profileComplete;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}