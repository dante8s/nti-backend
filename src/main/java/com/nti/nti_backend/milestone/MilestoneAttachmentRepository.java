package com.nti.nti_backend.milestone;

import com.nti.nti_backend.milestone.entity.MilestoneAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MilestoneAttachmentRepository
extends JpaRepository<MilestoneAttachment, UUID> {
    List<MilestoneAttachment> findAllByMilestoneId(UUID milestoneId);
    long countByMilestoneId(UUID milestoneId);
}
