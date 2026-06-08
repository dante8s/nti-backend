package com.nti.nti_backend.application;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApplicationMemberRepository extends JpaRepository<ApplicationMember, Long> {

    List<ApplicationMember> findByApplicationId(Long applicationId);

    @Query("SELECT m.application.id FROM ApplicationMember m WHERE m.userId = :userId")
    List<Long> findApplicationIdsByUserId(@Param("userId") Long userId);
}
