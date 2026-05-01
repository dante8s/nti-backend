package com.nti.nti_backend.controller;

import com.nti.nti_backend.studentProfile.CallApplicationEligibility;
import com.nti.nti_backend.studentProfile.ProfileSessionBrief;
import com.nti.nti_backend.studentProfile.ProfileUpsertRequest;
import com.nti.nti_backend.studentProfile.StudentProfile;
import com.nti.nti_backend.studentProfile.StudentProfileRepository;
import com.nti.nti_backend.studentProfile.StudentProfileService;
import com.nti.nti_backend.team.TeamRepository;
import com.nti.nti_backend.teamMember.TeamMemberRepository;
import com.nti.nti_backend.user.Role;
import com.nti.nti_backend.user.User;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CV Upload API — Week 3
 * Handles PDF upload, download, and deletion for StudentProfile.
 *
 * POST   /api/profile/{userId}/cv         — upload CV PDF
 * GET    /api/profile/{userId}/cv         — download / preview CV
 * DELETE /api/profile/{userId}/cv         — remove CV
 */

@RestController
@RequestMapping("/api/profile")
public class CvUploadController {
    private final StudentProfileService studentProfileService;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final StudentProfileRepository studentProfileRepository;

    public CvUploadController(
            StudentProfileService studentProfileService,
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            StudentProfileRepository studentProfileRepository) {
        this.studentProfileService = studentProfileService;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.studentProfileRepository = studentProfileRepository;
    }

    @GetMapping("/{userId:\\d+}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<StudentProfile> getProfile(
            @AuthenticationPrincipal User authUser,
            @PathVariable Long userId) {
        if (!canAccessUser(authUser, userId)) {
            return ResponseEntity.status(403).build();
        }
        try {
            return ResponseEntity.ok(studentProfileService.getProfileById(userId));
        } catch (IllegalStateException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<StudentProfile> getMyProfile(
            @AuthenticationPrincipal User authUser) {
        if (authUser == null || authUser.getId() == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            return ResponseEntity.ok(studentProfileService.getProfileById(authUser.getId()));
        } catch (IllegalStateException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Current user id and roles from JWT. */
    @GetMapping("/me/session")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ProfileSessionBrief> getMySessionBrief(
            @AuthenticationPrincipal User authUser) {
        if (authUser == null || authUser.getId() == null) {
            return ResponseEntity.status(401).build();
        }
        Set<String> roleNames = authUser.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(new ProfileSessionBrief(
                authUser.getId(),
                authUser.getName(),
                authUser.getEmail(),
                roleNames,
                authUser.getAccountStatus().name(),
                authUser.isEmailVerified(),
                authUser.isOnboardingCompleted()
        ));
    }

    /**
     * Чи користувач на своєму боці готовий переходити до заявки на виклик (профіль + команда).
     * Не змінює модуль applications — лише підказка для UI.
     */
    @GetMapping("/me/call-application-eligibility")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<CallApplicationEligibility> getCallApplicationEligibility(
            @AuthenticationPrincipal User authUser) {
        if (authUser == null || authUser.getId() == null) {
            return ResponseEntity.status(401).build();
        }
        boolean privileged = authUser.hasRole(Role.SUPER_ADMIN)
                || authUser.hasRole(Role.ADMIN);
        boolean teamLeader =
                teamRepository.findByLeader_Id(authUser.getId()).isPresent();
        boolean profileComplete =
                studentProfileRepository.findByUser_Id(authUser.getId())
                        .map(StudentProfile::isProfileComplete)
                        .orElse(false);
        boolean suggestsReady =
                privileged || (teamLeader && profileComplete);
        List<String> reminders = new ArrayList<>();
        if (!privileged) {
            if (!teamLeader) {
                reminders.add(
                        "Створіть команду й будьте її лідером (сторінка «Моя команда»).");
            }
            if (!profileComplete) {
                reminders.add(
                        "Завершіть студентський профіль і завантажте CV.");
            }
        }
        return ResponseEntity.ok(new CallApplicationEligibility(
                profileComplete,
                teamLeader,
                suggestsReady,
                List.copyOf(reminders)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<StudentProfile> createProfile(
            @AuthenticationPrincipal User authUser,
            @RequestBody ProfileUpsertRequest request) {
        if (!canAccessUser(authUser, request.getUserId())) {
            return ResponseEntity.status(403).build();
        }
        try {
            StudentProfile profile = mapRequestToProfile(request);
            return ResponseEntity.ok(studentProfileService.createStudentProfile(profile));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/me")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<StudentProfile> createMyProfile(
            @AuthenticationPrincipal User authUser,
            @RequestBody ProfileUpsertRequest request) {
        if (authUser == null || authUser.getId() == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            StudentProfile profile = mapRequestToProfile(request, authUser.getId());
            return ResponseEntity.ok(studentProfileService.createStudentProfile(profile));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{userId:\\d+}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<StudentProfile> updateProfile(
            @AuthenticationPrincipal User authUser,
            @PathVariable Long userId,
            @RequestBody ProfileUpsertRequest request
    ) {
        if (!canAccessUser(authUser, userId) || !userId.equals(request.getUserId())) {
            return ResponseEntity.status(403).build();
        }
        try {
            StudentProfile updated = mapRequestToProfile(request);
            return ResponseEntity.ok(studentProfileService.editProfile(userId, updated));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<StudentProfile> updateMyProfile(
            @AuthenticationPrincipal User authUser,
            @RequestBody ProfileUpsertRequest request
    ) {
        if (authUser == null || authUser.getId() == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            StudentProfile updated = mapRequestToProfile(request, authUser.getId());
            return ResponseEntity.ok(studentProfileService.editProfile(authUser.getId(), updated));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ── POST /api/profile/{userId}/cv ─────────────────────────────────────────
    // Accepts a multipart PDF file.
    // Vue "Upload CV" button sends: Content-Type: multipart/form-data, field name: "file"

    @PostMapping(value = "/{userId:\\d+}/cv" , consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<StudentProfile> uploadCv(
            @AuthenticationPrincipal User authUser,
            @PathVariable Long userId ,
            @RequestPart("file")MultipartFile file) {
        if (!canAccessUser(authUser, userId)) {
            return ResponseEntity.status(403).build();
        }

        if (file == null || file.isEmpty()){
            return ResponseEntity.badRequest().build();
        }

        // Validate it is a PDF by both MIME type and file extension
        // — double check because MIME type alone can be spoofed

        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null || !originalFileName.toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().build();
        }

        try {
            StudentProfile updated = studentProfileService.uploadCv(userId , file);
            return ResponseEntity.ok(updated);
        }catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ── GET /api/profile/{userId}/cv ──────────────────────────────────────────
    // Returns the PDF file as a downloadable/previewable response.
    // Vue profile page uses this to render the PDF preview iframe.
    @GetMapping("/{userId:\\d+}/cv")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Resource> downloadCv(
            @AuthenticationPrincipal User authUser,
            @PathVariable Long userId) {
        if (!canAccessUser(authUser, userId)) {
            return ResponseEntity.status(403).build();
        }
        StudentProfile profile = studentProfileService.getProfileById(userId);

        if (profile.getCvFilePath() == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path filePath = Paths.get(profile.getCvFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // Content-Disposition: inline — opens in browser PDF viewer
            // Change to "attachment" if you want to force download instead
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION ,
                            "inline; filename=\"" + profile.getCvOriginalName() + "\"")
                    .body(resource);
        }catch  (MalformedURLException e ) {
            return ResponseEntity.internalServerError().build();
        }
    }


    // ── DELETE /api/profile/{userId}/cv ──────────────────────────────────────
    // Removes the CV file from disk and clears the path in the profile.
    // Called when the student clicks "Remove CV" on their profile page.
    @DeleteMapping("/{userId:\\d+}/cv")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<StudentProfile> deleteCv(
            @AuthenticationPrincipal User authUser,
            @PathVariable Long userId) {
        if (!canAccessUser(authUser, userId)) {
            return ResponseEntity.status(403).build();
        }
        StudentProfile profile = studentProfileService.getProfileById(userId);

        if (profile.getCvFilePath() == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            java.nio.file.Files.deleteIfExists(Paths.get(profile.getCvFilePath()));
        }catch (IOException e ) {
            // File already gone from disk — still clear the DB record
        }
        StudentProfile saved = studentProfileService.clearCv(userId);
        return ResponseEntity.ok(saved);
    }

    @PostMapping(value = "/me/cv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<StudentProfile> uploadMyCv(
            @AuthenticationPrincipal User authUser,
            @RequestPart("file") MultipartFile file) {
        if (authUser == null || authUser.getId() == null) {
            return ResponseEntity.status(401).build();
        }
        return uploadCv(authUser, authUser.getId(), file);
    }

    @GetMapping("/me/cv")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Resource> downloadMyCv(
            @AuthenticationPrincipal User authUser) {
        if (authUser == null || authUser.getId() == null) {
            return ResponseEntity.status(401).build();
        }
        return downloadCv(authUser, authUser.getId());
    }

    @DeleteMapping("/me/cv")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<StudentProfile> deleteMyCv(
            @AuthenticationPrincipal User authUser) {
        if (authUser == null || authUser.getId() == null) {
            return ResponseEntity.status(401).build();
        }
        return deleteCv(authUser, authUser.getId());
    }

    private StudentProfile mapRequestToProfile(ProfileUpsertRequest request) {
        return mapRequestToProfile(request, request.getUserId());
    }

    private StudentProfile mapRequestToProfile(ProfileUpsertRequest request, Long userId) {
        StudentProfile profile = new StudentProfile();

        User user = new User();
        user.setId(userId);
        profile.setUser(user);

        profile.setStudyProgram(request.getStudyProgram());
        profile.setYearOfStudy(request.getYearOfStudy());
        profile.setSkills(request.getSkills());
        profile.setBio(request.getBio());
        profile.setHasRepeatedSubjects(request.isHasRepeatedSubjects());
        profile.setProfileAverageGrade(request.getProfileAverageGrade());

        return profile;
    }

    private boolean canAccessUser(User authUser, Long targetUserId) {
        if (authUser == null || targetUserId == null) {
            return false;
        }
        if (targetUserId.equals(authUser.getId())) {
            return true;
        }
        if (authUser.hasRole(Role.ADMIN) || authUser.hasRole(Role.SUPER_ADMIN)) {
            return true;
        }
        return teamMemberRepository.countAcceptedCoMembership(authUser.getId(), targetUserId) > 0;
    }
}
