package com.nti.nti_backend.milestone;

import com.nti.nti_backend.milestone.entity.MilestoneComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MilestoneCommentRepository
    extends JpaRepository<MilestoneComment, UUID> {
    List<MilestoneComment> findAllByMilestoneIdOrderByCreatedAtDesc(UUID milestoneId);
}
