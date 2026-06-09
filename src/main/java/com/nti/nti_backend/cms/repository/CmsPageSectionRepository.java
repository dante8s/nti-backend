package com.nti.nti_backend.cms.repository;
import com.nti.nti_backend.cms.entity.CmsPageSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
public interface CmsPageSectionRepository extends JpaRepository<CmsPageSection, Long> {
    List<CmsPageSection> findByPageKeyAndPublishedTrueOrderBySortOrderAsc(String pageKey);
    List<CmsPageSection> findByPageKeyOrderBySortOrderAsc(String pageKey);
}
