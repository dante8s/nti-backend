package com.nti.nti_backend.milestone;

import com.nti.nti_backend.milestone.entity.Milestone;
import com.nti.nti_backend.milestone.entity.MilestoneStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MilestoneRepository extends JpaRepository<Milestone, UUID> {
    List<Milestone> findAllByApplication_Id(Long applicationId);

    List<Milestone> findAllByMentorshipId(UUID mentorshipId);

    List<Milestone> findAllByCreatedById(Long createdById);

    List<Milestone> findAllByStatus(MilestoneStatus status);
}
