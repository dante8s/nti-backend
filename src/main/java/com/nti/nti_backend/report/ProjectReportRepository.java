package com.nti.nti_backend.report;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectReportRepository extends JpaRepository<ProjectReport, Long> {
    Optional<ProjectReport> findByApplication_Id(Long applicationId);
}
