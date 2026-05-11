package com.nti.nti_backend.studentProfile;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;

    private final String uploadDir = "uploads/cv/";

    public StudentProfileService(StudentProfileRepository studentProfileRepository) {
        this.studentProfileRepository = studentProfileRepository;
    }

    @Transactional(readOnly = true)
    public StudentProfile getProfileById(Long userId) {
        return studentProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new IllegalStateException("User not found by that id"));
    }

    public StudentProfile createStudentProfile(StudentProfile profile) {
        if (studentProfileRepository.existsByUser_Id(profile.getUser().getId())) {
            throw new IllegalStateException("Profile already exists for this user");
        }
        profile.setProfileComplete(false);
        return studentProfileRepository.save(profile);
    }

    public StudentProfile editProfile(Long userId, StudentProfile updated) {
        StudentProfile existing = getProfileById(userId);

        existing.setStudyProgram(updated.getStudyProgram());
        existing.setSkills(updated.getSkills());
        existing.setBio(updated.getBio());
        existing.setProfileAverageGrade(updated.getProfileAverageGrade());
        existing.setHasRepeatedSubjects(updated.isHasRepeatedSubjects());
        existing.setYearOfStudy(updated.getYearOfStudy());
        existing.setProfileComplete(isProfileComplete(existing));

        return studentProfileRepository.save(existing);
    }

    public StudentProfile clearCv(Long userId) {
        StudentProfile profile = getProfileById(userId);
        profile.setCvFilePath(null);
        profile.setCvOriginalName(null);
        profile.setCvUploadedAt(null);
        profile.setProfileComplete(isProfileComplete(profile));
        return studentProfileRepository.save(profile);
    }

    public StudentProfile uploadCv(Long userId, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalStateException("Cannot upload an empty file");
        }

        String contentType = file.getContentType();
        String originalFileName = file.getOriginalFilename();
        boolean hasPdfExtension =
                originalFileName != null && originalFileName.toLowerCase().endsWith(".pdf");
        boolean hasPdfMime =
                "application/pdf".equalsIgnoreCase(contentType)
                        || "application/octet-stream".equalsIgnoreCase(contentType);
        if (!hasPdfExtension || !hasPdfMime) {
            throw new IllegalStateException("Only PDF files are accepted for CV");
        }

        StudentProfile profile = getProfileById(userId);

        String filename = userId + "_" + System.currentTimeMillis() + "_" + originalFileName;
        Path targetPath = Paths.get(uploadDir).resolve(filename);
        Files.createDirectories(targetPath.getParent());
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        if (profile.getCvFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(profile.getCvFilePath()));
            } catch (IOException ignored) {
            }
        }

        profile.setCvFilePath(targetPath.toString());
        profile.setCvOriginalName(file.getOriginalFilename());
        profile.setCvUploadedAt(LocalDateTime.now());
        profile.setProfileComplete(isProfileComplete(profile));

        return studentProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public List<StudentProfile> getProfileWithCv() {
        return studentProfileRepository.findByCvFilePathIsNotNull();
    }

    @Transactional(readOnly = true)
    public List<StudentProfile> getCompleteProfile() {
        return studentProfileRepository.findByProfileCompleteTrue();
    }

    @Transactional(readOnly = true)
    public long countProfileWithCv() {
        return studentProfileRepository.countByCvFilePathIsNotNull();
    }

    @Transactional(readOnly = true)
    public long countCompleteProfile() {
        return studentProfileRepository.countByProfileCompleteTrue();
    }

    @Transactional(readOnly = true)
    public Double getAverageProfileGrade() {
        return studentProfileRepository.findAverageProfileGrade();
    }

    private boolean isProfileComplete(StudentProfile profile) {
        return profile.getStudyProgram() != null
                && profile.getYearOfStudy() != null
                && profile.getSkills() != null && !profile.getSkills().isBlank()
                && profile.getCvFilePath() != null;
    }
}
