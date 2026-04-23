package com.nti.nti_backend.controller;

import com.nti.nti_backend.studentProfile.ProfileUpsertRequest;
import com.nti.nti_backend.studentProfile.StudentProfile;
import com.nti.nti_backend.studentProfile.StudentProfileService;
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

    public CvUploadController (StudentProfileService studentProfileService) {
        this.studentProfileService = studentProfileService;
    }

    @GetMapping("/{userId}")
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

    @PutMapping("/{userId}")
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

    // ── POST /api/profile/{userId}/cv ─────────────────────────────────────────
    // Accepts a multipart PDF file.
    // Vue "Upload CV" button sends: Content-Type: multipart/form-data, field name: "file"

    @PostMapping(value = "/{userId}/cv" , consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
    @GetMapping("/{userId}/cv")
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
    @DeleteMapping("/{userId}/cv")
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

    private StudentProfile mapRequestToProfile(ProfileUpsertRequest request) {
        StudentProfile profile = new StudentProfile();

        User user = new User();
        user.setId(request.getUserId());
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
        return authUser != null && targetUserId != null && (
                targetUserId.equals(authUser.getId())
                        || authUser.hasRole(Role.ADMIN)
                        || authUser.hasRole(Role.SUPER_ADMIN)
        );
    }
}
