package com.nti.nti_backend.mentorship.repository;

import com.nti.nti_backend.mentorship.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsultationRepository
    extends JpaRepository<Consultation, UUID> {

    List<Consultation> findAllByMentorshipIdOrderByConsultationDateDesc(UUID mentorshipId);
    List<Consultation> findAllByMentorId(Long mentorId);

}
