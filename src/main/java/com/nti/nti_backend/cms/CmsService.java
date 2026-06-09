package com.nti.nti_backend.cms;

import com.nti.nti_backend.cms.dto.*;
import com.nti.nti_backend.cms.entity.CmsArticle;
import com.nti.nti_backend.cms.entity.CmsPageSection;
import com.nti.nti_backend.cms.entity.CmsProject;
import com.nti.nti_backend.cms.entity.CmsTestimonial;
import com.nti.nti_backend.cms.repository.CmsArticleRepository;
import com.nti.nti_backend.cms.repository.CmsPageSectionRepository;
import com.nti.nti_backend.cms.repository.CmsProjectRepository;
import com.nti.nti_backend.cms.repository.CmsTestimonialRepository;
import com.nti.nti_backend.file.FileServeService;
import com.nti.nti_backend.organization.exception.ConflictException;
import com.nti.nti_backend.organization.exception.ResourceNotFoundException;
import com.nti.nti_backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CmsService {

    private final CmsArticleRepository articleRepo;
    private final CmsProjectRepository projectRepo;
    private final CmsTestimonialRepository testimonialRepo;
    private final CmsPageSectionRepository pageSectionRepo;
    private final FileServeService fileServeService;

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }

    // ─────────────────────────────────────────────
    // ARTICLES
    // ─────────────────────────────────────────────

    @Transactional
    public ArticleResponseDTO createArticle(ArticleRequestDTO dto) {
        CmsArticle article = CmsArticle.builder()
                .title(dto.getTitle())
                .category(dto.getCategory())
                .excerpt(dto.getExcerpt())
                .content(dto.getContent())
                .createdBy(getCurrentUser())
                .build();
        return toArticleDTO(articleRepo.save(article));
    }

    @Transactional
    public ArticleResponseDTO updateArticle(Long id, ArticleRequestDTO dto) {
        CmsArticle article = articleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Article not found: " + id));
        article.setTitle(dto.getTitle());
        article.setCategory(dto.getCategory());
        article.setExcerpt(dto.getExcerpt());
        article.setContent(dto.getContent());
        return toArticleDTO(articleRepo.save(article));
    }

    @Transactional
    public ArticleResponseDTO publishArticle(Long id, boolean publish) {
        CmsArticle article = articleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Article not found: " + id));
        article.setPublished(publish);
        if (publish && article.getPublishedAt() == null) {
            article.setPublishedAt(OffsetDateTime.now());
        }
        return toArticleDTO(articleRepo.save(article));
    }

    @Transactional
    public void deleteArticle(Long id) {
        CmsArticle article = articleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Article not found: " + id));
        if (article.getImagePath() != null) {
            silentDelete(article.getImagePath());
        }
        articleRepo.delete(article);
    }

    @Transactional
    public ArticleResponseDTO uploadArticleImage(Long id, MultipartFile file) {
        CmsArticle article = articleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Article not found: " + id));
        fileServeService.validateImage(file);
        try {
            if (article.getImagePath() != null) {
                silentDelete(article.getImagePath());
            }
            String filename = "article_" + id + "_"
                    + System.currentTimeMillis() + "_"
                    + file.getOriginalFilename();
            String path = fileServeService.saveImage(file, "cms/articles", filename);
            article.setImagePath(path);
            article.setImageName(file.getOriginalFilename());
            return toArticleDTO(articleRepo.save(article));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save image: " + e.getMessage());
        }
    }

    // Public reads
    @Transactional(readOnly = true)
    public Page<ArticleResponseDTO> getPublishedArticles(
            String category, Pageable pageable) {
        if (category != null && !category.isBlank()) {
            return articleRepo
                    .findByPublishedTrueAndCategoryOrderByPublishedAtDesc(
                            category, pageable)
                    .map(this::toArticleDTO);
        }
        return articleRepo
                .findByPublishedTrueOrderByPublishedAtDesc(pageable)
                .map(this::toArticleDTO);
    }

    @Transactional(readOnly = true)
    public ArticleResponseDTO getPublishedArticle(Long id) {
        CmsArticle article = articleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Article not found: " + id));
        if (!article.isPublished()) {
            throw new ResourceNotFoundException("Article not found: " + id);
        }
        return toArticleDTO(article);
    }

    @Transactional(readOnly = true)
    public List<ArticleResponseDTO> getAllArticlesAdmin() {
        return articleRepo.findAll().stream()
                .map(this::toArticleDTO)
                .collect(Collectors.toList());
    }

    public ResponseEntity<Resource> serveArticleImage(Long id) {
        CmsArticle article = articleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Article not found: " + id));
        return serveImage(article.getImagePath(), article.getImageName());
    }

    // ─────────────────────────────────────────────
    // PROJECTS
    // ─────────────────────────────────────────────

    @Transactional
    public ProjectResponseDTO createProject(ProjectRequestDTO dto) {
        CmsProject project = CmsProject.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .fundingAmount(dto.getFundingAmount())
                .statusLabel(dto.getStatusLabel())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .published(dto.isPublished())
                .build();
        return toProjectDTO(projectRepo.save(project));
    }

    @Transactional
    public ProjectResponseDTO updateProject(Long id, ProjectRequestDTO dto) {
        CmsProject project = projectRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: " + id));
        project.setTitle(dto.getTitle());
        project.setDescription(dto.getDescription());
        project.setFundingAmount(dto.getFundingAmount());
        project.setStatusLabel(dto.getStatusLabel());
        if (dto.getSortOrder() != null) project.setSortOrder(dto.getSortOrder());
        project.setPublished(dto.isPublished());
        return toProjectDTO(projectRepo.save(project));
    }

    @Transactional
    public void deleteProject(Long id) {
        CmsProject project = projectRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: " + id));
        if (project.getImagePath() != null) {
            silentDelete(project.getImagePath());
        }
        projectRepo.delete(project);
    }

    @Transactional
    public ProjectResponseDTO uploadProjectImage(Long id, MultipartFile file) {
        CmsProject project = projectRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: " + id));
        fileServeService.validateImage(file);
        try {
            if (project.getImagePath() != null) silentDelete(project.getImagePath());
            String filename = "project_" + id + "_"
                    + System.currentTimeMillis() + "_"
                    + file.getOriginalFilename();
            String path = fileServeService.saveImage(file, "cms/projects", filename);
            project.setImagePath(path);
            project.setImageName(file.getOriginalFilename());
            return toProjectDTO(projectRepo.save(project));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save image: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDTO> getPublishedProjects() {
        return projectRepo.findByPublishedTrueOrderBySortOrderAsc()
                .stream().map(this::toProjectDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDTO> getAllProjectsAdmin() {
        return projectRepo.findAll().stream()
                .map(this::toProjectDTO).collect(Collectors.toList());
    }

    public ResponseEntity<Resource> serveProjectImage(Long id) {
        CmsProject project = projectRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: " + id));
        return serveImage(project.getImagePath(), project.getImageName());
    }

    // ─────────────────────────────────────────────
    // TESTIMONIALS
    // ─────────────────────────────────────────────

    @Transactional
    public TestimonialResponseDTO createTestimonial(TestimonialRequestDTO dto) {
        CmsTestimonial t = CmsTestimonial.builder()
                .quote(dto.getQuote())
                .authorName(dto.getAuthorName())
                .authorRole(dto.getAuthorRole())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .published(dto.isPublished())
                .build();
        return toTestimonialDTO(testimonialRepo.save(t));
    }

    @Transactional
    public TestimonialResponseDTO updateTestimonial(Long id, TestimonialRequestDTO dto) {
        CmsTestimonial t = testimonialRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Testimonial not found: " + id));
        t.setQuote(dto.getQuote());
        t.setAuthorName(dto.getAuthorName());
        t.setAuthorRole(dto.getAuthorRole());
        if (dto.getSortOrder() != null) t.setSortOrder(dto.getSortOrder());
        t.setPublished(dto.isPublished());
        return toTestimonialDTO(testimonialRepo.save(t));
    }

    @Transactional
    public void deleteTestimonial(Long id) {
        CmsTestimonial t = testimonialRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Testimonial not found: " + id));
        if (t.getAvatarPath() != null) silentDelete(t.getAvatarPath());
        testimonialRepo.delete(t);
    }

    @Transactional
    public TestimonialResponseDTO uploadTestimonialAvatar(Long id, MultipartFile file) {
        CmsTestimonial t = testimonialRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Testimonial not found: " + id));
        fileServeService.validateImage(file);
        try {
            if (t.getAvatarPath() != null) silentDelete(t.getAvatarPath());
            String filename = "testimonial_" + id + "_"
                    + System.currentTimeMillis() + "_"
                    + file.getOriginalFilename();
            String path = fileServeService.saveImage(file, "cms/testimonials", filename);
            t.setAvatarPath(path);
            t.setAvatarName(file.getOriginalFilename());
            return toTestimonialDTO(testimonialRepo.save(t));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save avatar: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<TestimonialResponseDTO> getPublishedTestimonials() {
        return testimonialRepo.findByPublishedTrueOrderBySortOrderAsc()
                .stream().map(this::toTestimonialDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TestimonialResponseDTO> getAllTestimonialsAdmin() {
        return testimonialRepo.findAll().stream()
                .map(this::toTestimonialDTO).collect(Collectors.toList());
    }

    public ResponseEntity<Resource> serveTestimonialAvatar(Long id) {
        CmsTestimonial t = testimonialRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Testimonial not found: " + id));
        return serveImage(t.getAvatarPath(), t.getAvatarName());
    }

    // ─────────────────────────────────────────────
    // PAGE SECTIONS
    // ─────────────────────────────────────────────

    @Transactional
    public PageSectionResponseDTO createSection(
            String pageKey, PageSectionRequestDTO dto) {
        CmsPageSection section = CmsPageSection.builder()
                .pageKey(pageKey)
                .sectionType(dto.getSectionType() != null
                        ? dto.getSectionType() : "block")
                .title(dto.getTitle())
                .subtitle(dto.getSubtitle())
                .content(dto.getContent())
                .icon(dto.getIcon())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .published(dto.isPublished())
                .build();
        return toSectionDTO(pageSectionRepo.save(section));
    }

    @Transactional
    public PageSectionResponseDTO updateSection(
            Long id, PageSectionRequestDTO dto) {
        CmsPageSection section = pageSectionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Section not found: " + id));
        section.setSectionType(dto.getSectionType() != null
                ? dto.getSectionType() : section.getSectionType());
        section.setTitle(dto.getTitle());
        section.setSubtitle(dto.getSubtitle());
        section.setContent(dto.getContent());
        section.setIcon(dto.getIcon());
        if (dto.getSortOrder() != null) section.setSortOrder(dto.getSortOrder());
        section.setPublished(dto.isPublished());
        return toSectionDTO(pageSectionRepo.save(section));
    }

    @Transactional
    public void deleteSection(Long id) {
        CmsPageSection section = pageSectionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Section not found: " + id));
        if (section.getImagePath() != null) silentDelete(section.getImagePath());
        pageSectionRepo.delete(section);
    }

    @Transactional
    public PageSectionResponseDTO uploadSectionImage(Long id, MultipartFile file) {
        CmsPageSection section = pageSectionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Section not found: " + id));
        fileServeService.validateImage(file);
        try {
            if (section.getImagePath() != null) silentDelete(section.getImagePath());
            String filename = "section_" + id + "_"
                    + System.currentTimeMillis() + "_"
                    + file.getOriginalFilename();
            String path = fileServeService.saveImage(file, "cms/sections", filename);
            section.setImagePath(path);
            section.setImageName(file.getOriginalFilename());
            return toSectionDTO(pageSectionRepo.save(section));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save image: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<PageSectionResponseDTO> getPublishedSections(String pageKey) {
        return pageSectionRepo
                .findByPageKeyAndPublishedTrueOrderBySortOrderAsc(pageKey)
                .stream().map(this::toSectionDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PageSectionResponseDTO> getAllSectionsAdmin(String pageKey) {
        return pageSectionRepo
                .findByPageKeyOrderBySortOrderAsc(pageKey)
                .stream().map(this::toSectionDTO).collect(Collectors.toList());
    }

    public ResponseEntity<Resource> serveSectionImage(Long id) {
        CmsPageSection section = pageSectionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Section not found: " + id));
        return serveImage(section.getImagePath(), section.getImageName());
    }

    // ─────────────────────────────────────────────
    // Shared helpers
    // ─────────────────────────────────────────────

    private ResponseEntity<Resource> serveImage(String path, String name) {
        if (path == null) {
            throw new ResourceNotFoundException("No image uploaded");
        }
        Resource resource = fileServeService.load(path);
        String contentType = fileServeService.detectContentType(name);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        fileServeService.contentDisposition(true, name))
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    private void silentDelete(String path) {
        try { Files.deleteIfExists(Paths.get(path)); }
        catch (IOException ignored) {}
    }

    // ─────────────────────────────────────────────
    // Mapping
    // ─────────────────────────────────────────────

    private ArticleResponseDTO toArticleDTO(CmsArticle a) {
        return ArticleResponseDTO.builder()
                .id(a.getId())
                .title(a.getTitle())
                .category(a.getCategory())
                .excerpt(a.getExcerpt())
                .content(a.getContent())
                .imageUrl(a.getImagePath() != null
                        ? "/api/public/cms/articles/" + a.getId() + "/image" : null)
                .published(a.isPublished())
                .publishedAt(a.getPublishedAt())
                .createdByName(a.getCreatedBy() != null
                        ? a.getCreatedBy().getName() : null)
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }

    private ProjectResponseDTO toProjectDTO(CmsProject p) {
        return ProjectResponseDTO.builder()
                .id(p.getId())
                .title(p.getTitle())
                .description(p.getDescription())
                .imageUrl(p.getImagePath() != null
                        ? "/api/public/cms/projects/" + p.getId() + "/image" : null)
                .fundingAmount(p.getFundingAmount())
                .statusLabel(p.getStatusLabel())
                .sortOrder(p.getSortOrder())
                .published(p.isPublished())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private TestimonialResponseDTO toTestimonialDTO(CmsTestimonial t) {
        return TestimonialResponseDTO.builder()
                .id(t.getId())
                .quote(t.getQuote())
                .authorName(t.getAuthorName())
                .authorRole(t.getAuthorRole())
                .avatarUrl(t.getAvatarPath() != null
                        ? "/api/public/cms/testimonials/" + t.getId() + "/avatar" : null)
                .sortOrder(t.getSortOrder())
                .published(t.isPublished())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private PageSectionResponseDTO toSectionDTO(CmsPageSection s) {
        return PageSectionResponseDTO.builder()
                .id(s.getId())
                .pageKey(s.getPageKey())
                .sectionType(s.getSectionType())
                .title(s.getTitle())
                .subtitle(s.getSubtitle())
                .content(s.getContent())
                .icon(s.getIcon())
                .imageUrl(s.getImagePath() != null
                        ? "/api/public/cms/sections/" + s.getId() + "/image" : null)
                .sortOrder(s.getSortOrder())
                .published(s.isPublished())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}