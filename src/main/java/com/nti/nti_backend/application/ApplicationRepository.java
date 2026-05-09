package com.nti.nti_backend.application;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository
        extends JpaRepository<Application, Long> {

    List<Application> findByApplicantId(Long applicantId);

    List<Application> findByStatus(ApplicationStatus status);

    List<Application> findByCallId(Long callId);

    boolean existsByApplicantIdAndCallId(Long applicantId, Long callId);

    // Знайти конкретну заявку студента для виклику
    Optional<Application> findByApplicantIdAndCallId(Long applicantId, Long callId);
}