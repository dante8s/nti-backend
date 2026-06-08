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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import static com.nti.nti_backend.config.CacheNames.*;
import org.springframework.cache.annotation.*;

@Service
@Transactional
public class StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;

    private final String uploadDir = "uploads/cv/";

    private final String avatarUploadDir = "uploads/profile-photos/";

    private static final long MAX_AVATAR_BYTES = 10L * 1024 * 1024;

    private static final List<String> DANGEROUS_SUFFIXES = List.of(
            ".exe", ".bat", ".cmd", ".sh", ".dll", ".jar", ".php", ".html", ".htm"
    );

    private static final String[] IMAGE_SUFFIXES = {
            ".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp", ".tif", ".tiff",
            ".heic", ".heif", ".avif", ".ico", ".jfif", ".pjpeg", ".pjp", ".svg",
    };

    public StudentProfileService(StudentProfileRepository studentProfileRepository) {
        this.studentProfileRepository = studentProfileRepository;
    }

    @Cacheable(value = STUDENT_PROFILE, key = "#userId")
    @Transactional(readOnly = true)
    public StudentProfile getProfileById(Long userId) {
        return studentProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new IllegalStateException("User not found by that id"));
    }

    @CacheEvict(value = STUDENT_PROFILE, key = "#profile.user.id")
    public StudentProfile createStudentProfile(StudentProfile profile) {
        if (studentProfileRepository.existsByUser_Id(profile.getUser().getId())) {
            throw new IllegalStateException("Profile already exists for this user");
        }
        profile.setProfileComplete(false);
        return studentProfileRepository.save(profile);
    }

    @CacheEvict(value = STUDENT_PROFILE, key = "#userId")
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

    @CacheEvict(value = STUDENT_PROFILE, key = "#userId")
    public StudentProfile clearCv(Long userId) {
        StudentProfile profile = getProfileById(userId);
        profile.setCvFilePath(null);
        profile.setCvOriginalName(null);
        profile.setCvUploadedAt(null);
        profile.setProfileComplete(isProfileComplete(profile));
        return studentProfileRepository.save(profile);
    }

    @CacheEvict(value = STUDENT_PROFILE, key = "#userId")
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

    @CacheEvict(value = STUDENT_PROFILE, key = "#userId")
    public StudentProfile clearProfilePhoto(Long userId) {
        StudentProfile profile = getProfileById(userId);
        if (profile.getAvatarFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(profile.getAvatarFilePath()));
            } catch (IOException ignored) {
            }
        }
        profile.setAvatarFilePath(null);
        profile.setAvatarOriginalName(null);
        profile.setAvatarUploadedAt(null);
        return studentProfileRepository.save(profile);
    }

    @CacheEvict(value = STUDENT_PROFILE, key = "#userId")
    public StudentProfile uploadProfilePhoto(Long userId, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalStateException("Cannot upload an empty file");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new IllegalStateException("Photo must be at most 10 MB");
        }

        String contentType = file.getContentType();
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalStateException("File name is required");
        }
        if (!isAllowedProfileImage(originalFileName, contentType)) {
            throw new IllegalStateException("Дозволено лише зображення (типові формати фото)");
        }

        StudentProfile profile = getProfileById(userId);
        String filename = userId + "_" + System.currentTimeMillis() + "_"
                + originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path targetPath = Paths.get(avatarUploadDir).resolve(filename);
        Files.createDirectories(targetPath.getParent());
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        if (profile.getAvatarFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(profile.getAvatarFilePath()));
            } catch (IOException ignored) {
            }
        }

        profile.setAvatarFilePath(targetPath.toString());
        profile.setAvatarOriginalName(file.getOriginalFilename());
        profile.setAvatarUploadedAt(LocalDateTime.now());

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

    private boolean isAllowedProfileImage(String filename, String contentType) {
        if (filename == null || filename.isBlank()) {
            return false;
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        for (String bad : DANGEROUS_SUFFIXES) {
            if (lower.endsWith(bad)) {
                return false;
            }
        }
        String ct = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT).trim();
        if (!ct.isEmpty() && !ct.startsWith("image/") && !"application/octet-stream".equals(ct)) {
            return false;
        }
        if (ct.startsWith("image/")) {
            return true;
        }
        return Arrays.stream(IMAGE_SUFFIXES).anyMatch(lower::endsWith);
    }
}
