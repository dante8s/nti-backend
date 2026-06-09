package com.nti.nti_backend.cms.repository;
import com.nti.nti_backend.cms.entity.CmsProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface CmsProjectRepository extends JpaRepository<CmsProject, Long> {
    List<CmsProject> findByPublishedTrueOrderBySortOrderAsc();
}
