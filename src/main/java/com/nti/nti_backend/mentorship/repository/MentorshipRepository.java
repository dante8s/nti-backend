package com.nti.nti_backend.mentorship.repository;

import com.nti.nti_backend.mentorship.entity.Mentorship;
import com.nti.nti_backend.mentorship.entity.MentorshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MentorshipRepository extends JpaRepository<Mentorship, UUID> {

    //All mentorships for specific mentor
    List<Mentorship> findByMentorId(Long mentorId);

    List<Mentorship> findAllByMentorIdAndStatus(Long mentorId, MentorshipStatus status);


    boolean existsByMentorIdAndApplication_IdAndStatus(
            Long mentorId, Long applicationId, MentorshipStatus status
    );

    List<Mentorship> findAllByApplication_Id(Long applicationId);

    @Query("""
            SELECT COUNT(m) FROM Mentorship m
            JOIN m.application a JOIN a.call c JOIN c.program p
            WHERE p.organization.id = :organizationId
            """)
    long countByProgramOrganizationId(@Param("organizationId") UUID organizationId);

}
