package com.nti.nti_backend.application;

import com.nti.nti_backend.call.Call;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository
        extends JpaRepository<Application, Long> {

    List<Application> findByApplicantId(Long applicantId);

    @Query("""
            SELECT DISTINCT a FROM Application a
            JOIN FETCH a.call c
            JOIN FETCH c.program p
            LEFT JOIN FETCH p.organization
            LEFT JOIN FETCH a.productOwner
            WHERE a.applicant.id = :applicantId
            ORDER BY a.updatedAt DESC
            """)
    List<Application> findByApplicantIdWithDetails(
            @Param("applicantId") Long applicantId);

    List<Application> findByStatus(ApplicationStatus status);

    @Query("""
            SELECT DISTINCT a FROM Application a
            JOIN FETCH a.applicant
            JOIN FETCH a.call c
            JOIN FETCH c.program
            WHERE c.id = :callId
            """)
    List<Application> findByCallIdWithApplicant(@Param("callId") Long callId);

    List<Application> findByCallId(Long callId);

    boolean existsByApplicantIdAndCallId(
            Long applicantId, Long callId
    );

    // boolean existsByApplicantIdAndCallId(Long applicantId, Long callId);

    // Знайти конкретну заявку студента для виклику
    Optional<Application> findByApplicantIdAndCallId(Long applicantId, Long callId);

    long countByStatus(ApplicationStatus status);

    @Query("""
            SELECT COUNT(a) FROM Application a
            JOIN a.call c JOIN c.program p
            WHERE p.organization.id = :organizationId
            """)
    long countByProgramOrganizationId(@Param("organizationId") UUID organizationId);

    long countByCallId(Long callId);

    long countByCall_Program_Id(Long programId);

    @Query("""
            SELECT DISTINCT a FROM Application a
            JOIN FETCH a.call c
            JOIN FETCH c.program p
            JOIN FETCH a.applicant ap
            LEFT JOIN FETCH p.organization
            """)
    List<Application> findAllForReportingExport();

    @Query("""
            SELECT COUNT(DISTINCT a.call.id) FROM Application a
            WHERE a.status = :status
            """)
    long countDistinctCallsByStatus(@Param("status") ApplicationStatus status);

    @Query("""
            SELECT DISTINCT a FROM Application a
            JOIN FETCH a.call c
            JOIN FETCH c.program p
            LEFT JOIN FETCH p.organization
            JOIN FETCH a.applicant ap
            WHERE ap.id IN :leaderIds
            ORDER BY ap.id ASC, a.updatedAt DESC
            """)
    List<Application> findByApplicantIdInWithDetails(@Param("leaderIds") List<Long> leaderIds);
}