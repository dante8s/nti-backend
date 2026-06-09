package com.nti.nti_backend.cms.controller;

import com.nti.nti_backend.cms.CmsService;
import com.nti.nti_backend.cms.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/cms")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'CONTENT_EDITOR')")
@RequiredArgsConstructor
public class CmsAdminController {

    private final CmsService cmsService;

    // ── Articles ──────────────────────────────────
    @PostMapping("/articles")
    public ResponseEntity<ArticleResponseDTO> createArticle(
            @Valid @RequestBody ArticleRequestDTO dto) {
        return ResponseEntity.status(201).body(cmsService.createArticle(dto));
    }

    @PutMapping("/articles/{id}")
    public ResponseEntity<ArticleResponseDTO> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody ArticleRequestDTO dto) {
        return ResponseEntity.ok(cmsService.updateArticle(id, dto));
    }

    @PatchMapping("/articles/{id}/publish")
    public ResponseEntity<ArticleResponseDTO> publishArticle(
            @PathVariable Long id,
            @RequestParam boolean publish) {
        return ResponseEntity.ok(cmsService.publishArticle(id, publish));
    }

    @DeleteMapping("/articles/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        cmsService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/articles/{id}/image")
    public ResponseEntity<ArticleResponseDTO> uploadArticleImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(cmsService.uploadArticleImage(id, file));
    }

    @GetMapping("/articles")
    public ResponseEntity<List<ArticleResponseDTO>> getAllArticles() {
        return ResponseEntity.ok(cmsService.getAllArticlesAdmin());
    }

    // ── Projects ──────────────────────────────────
    @PostMapping("/projects")
    public ResponseEntity<ProjectResponseDTO> createProject(
            @Valid @RequestBody ProjectRequestDTO dto) {
        return ResponseEntity.status(201).body(cmsService.createProject(dto));
    }

    @PutMapping("/projects/{id}")
    public ResponseEntity<ProjectResponseDTO> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequestDTO dto) {
        return ResponseEntity.ok(cmsService.updateProject(id, dto));
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        cmsService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/projects/{id}/image")
    public ResponseEntity<ProjectResponseDTO> uploadProjectImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(cmsService.uploadProjectImage(id, file));
    }

    @GetMapping("/projects")
    public ResponseEntity<List<ProjectResponseDTO>> getAllProjects() {
        return ResponseEntity.ok(cmsService.getAllProjectsAdmin());
    }

    // ── Testimonials ──────────────────────────────
    @PostMapping("/testimonials")
    public ResponseEntity<TestimonialResponseDTO> createTestimonial(
            @Valid @RequestBody TestimonialRequestDTO dto) {
        return ResponseEntity.status(201).body(cmsService.createTestimonial(dto));
    }

    @PutMapping("/testimonials/{id}")
    public ResponseEntity<TestimonialResponseDTO> updateTestimonial(
            @PathVariable Long id,
            @Valid @RequestBody TestimonialRequestDTO dto) {
        return ResponseEntity.ok(cmsService.updateTestimonial(id, dto));
    }

    @DeleteMapping("/testimonials/{id}")
    public ResponseEntity<Void> deleteTestimonial(@PathVariable Long id) {
        cmsService.deleteTestimonial(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/testimonials/{id}/avatar")
    public ResponseEntity<TestimonialResponseDTO> uploadTestimonialAvatar(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(cmsService.uploadTestimonialAvatar(id, file));
    }

    @GetMapping("/testimonials")
    public ResponseEntity<List<TestimonialResponseDTO>> getAllTestimonials() {
        return ResponseEntity.ok(cmsService.getAllTestimonialsAdmin());
    }

    // ── Page Sections ─────────────────────────────
    @PostMapping("/pages/{pageKey}/sections")
    public ResponseEntity<PageSectionResponseDTO> createSection(
            @PathVariable String pageKey,
            @RequestBody PageSectionRequestDTO dto) {
        return ResponseEntity.status(201)
                .body(cmsService.createSection(pageKey, dto));
    }

    @PutMapping("/pages/sections/{id}")
    public ResponseEntity<PageSectionResponseDTO> updateSection(
            @PathVariable Long id,
            @RequestBody PageSectionRequestDTO dto) {
        return ResponseEntity.ok(cmsService.updateSection(id, dto));
    }

    @DeleteMapping("/pages/sections/{id}")
    public ResponseEntity<Void> deleteSection(@PathVariable Long id) {
        cmsService.deleteSection(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/pages/sections/{id}/image")
    public ResponseEntity<PageSectionResponseDTO> uploadSectionImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(cmsService.uploadSectionImage(id, file));
    }

    @GetMapping("/pages/{pageKey}/sections")
    public ResponseEntity<List<PageSectionResponseDTO>> getAllSections(
            @PathVariable String pageKey) {
        return ResponseEntity.ok(cmsService.getAllSectionsAdmin(pageKey));
    }
}