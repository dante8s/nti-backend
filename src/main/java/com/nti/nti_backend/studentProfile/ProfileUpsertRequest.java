package com.nti.nti_backend.studentProfile;

public class ProfileUpsertRequest {

    private Long  userId;
    private String studyProgram;
    private Integer yearOfStudy;
    private String skills;
    private String bio;
    private boolean hasRepeatedSubjects;
    private Double profileAverageGrade ;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getStudyProgram() {
        return studyProgram;
    }

    public void setStudyProgram(String studyProgram) {
        this.studyProgram = studyProgram;
    }

    public Integer getYearOfStudy() {
        return yearOfStudy;
    }

    public void setYearOfStudy(Integer yearOfStudy) {
        this.yearOfStudy = yearOfStudy;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public boolean isHasRepeatedSubjects() {
        return hasRepeatedSubjects;
    }

    public void setHasRepeatedSubjects(boolean hasRepeatedSubjects) {
        this.hasRepeatedSubjects = hasRepeatedSubjects;
    }

    public Double getProfileAverageGrade() {
        return profileAverageGrade;
    }

    public void setProfileAverageGrade(Double profileAverageGrade) {
        this.profileAverageGrade = profileAverageGrade;
    }
}
