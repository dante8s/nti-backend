package com.nti.nti_backend.cms.controller;

import com.nti.nti_backend.cms.CmsService;
import com.nti.nti_backend.cms.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/cms")
@RequiredArgsConstructor
public class CmsPublicController {

    private final CmsService cmsService;

    // Articles
    @GetMapping("/articles")
    public ResponseEntity<Page<ArticleResponseDTO>> getArticles(
            @RequestParam(required = false) String category,
            @PageableDefault(size = 9, sort = "publishedAt") Pageable pageable) {
        return ResponseEntity.ok(
                cmsService.getPublishedArticles(category, pageable));
    }

    @GetMapping("/articles/{id}")
    public ResponseEntity<ArticleResponseDTO> getArticle(@PathVariable Long id) {
        return ResponseEntity.ok(cmsService.getPublishedArticle(id));
    }

    @GetMapping("/articles/{id}/image")
    public ResponseEntity<Resource> getArticleImage(@PathVariable Long id) {
        return cmsService.serveArticleImage(id);
    }

    // Projects
    @GetMapping("/projects")
    public ResponseEntity<List<ProjectResponseDTO>> getProjects() {
        return ResponseEntity.ok(cmsService.getPublishedProjects());
    }

    @GetMapping("/projects/{id}/image")
    public ResponseEntity<Resource> getProjectImage(@PathVariable Long id) {
        return cmsService.serveProjectImage(id);
    }

    // Testimonials
    @GetMapping("/testimonials")
    public ResponseEntity<List<TestimonialResponseDTO>> getTestimonials() {
        return ResponseEntity.ok(cmsService.getPublishedTestimonials());
    }

    @GetMapping("/testimonials/{id}/avatar")
    public ResponseEntity<Resource> getTestimonialAvatar(@PathVariable Long id) {
        return cmsService.serveTestimonialAvatar(id);
    }

    // Page sections
    @GetMapping("/pages/{pageKey}")
    public ResponseEntity<List<PageSectionResponseDTO>> getPageSections(
            @PathVariable String pageKey) {
        return ResponseEntity.ok(cmsService.getPublishedSections(pageKey));
    }

    @GetMapping("/sections/{id}/image")
    public ResponseEntity<Resource> getSectionImage(@PathVariable Long id) {
        return cmsService.serveSectionImage(id);
    }
}