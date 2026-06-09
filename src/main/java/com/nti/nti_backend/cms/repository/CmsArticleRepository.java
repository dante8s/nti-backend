package com.nti.nti_backend.cms.repository;

import com.nti.nti_backend.cms.entity.CmsArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface CmsArticleRepository extends JpaRepository<CmsArticle, Long> {
    Page<CmsArticle> findByPublishedTrueOrderByPublishedAtDesc(Pageable pageable);
    Page<CmsArticle> findByPublishedTrueAndCategoryOrderByPublishedAtDesc(
            String category, Pageable pageable);
    List<String> findDistinctCategoryByPublishedTrue();
}
