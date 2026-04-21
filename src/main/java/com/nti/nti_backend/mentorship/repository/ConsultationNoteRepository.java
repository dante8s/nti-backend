package com.nti.nti_backend.mentorship.repository;

import com.nti.nti_backend.Application;
import com.nti.nti_backend.mentorship.entity.ConsultationNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsultationNoteRepository extends JpaRepository<ConsultationNote, UUID> {

    List<ConsultationNote> findAllByApplicationIdOrderByCreatedAtDesc(Long applicationId);
}
