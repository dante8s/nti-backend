package com.nti.nti_backend.cms.repository;

import com.nti.nti_backend.cms.entity.CmsTestimonial;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
public interface CmsTestimonialRepository extends JpaRepository<CmsTestimonial, Long> {
    List<CmsTestimonial> findByPublishedTrueOrderBySortOrderAsc();
}
