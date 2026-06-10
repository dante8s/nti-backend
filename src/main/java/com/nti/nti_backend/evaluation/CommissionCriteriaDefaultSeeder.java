package com.nti.nti_backend.evaluation;

import com.nti.nti_backend.call.Call;
import com.nti.nti_backend.call.CallRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * If there are no criteria for a call in the DB, creates a default set for commission evaluation.
 * Aligned with the screen template (scores 1–100 and comment).
 */
@Service
public class CommissionCriteriaDefaultSeeder {

    private record DefaultCriterion(String name, String description, int weightPercent, int sortOrder) {}

    private static final List<DefaultCriterion> DEFAULT_SET = List.of(
            new DefaultCriterion(
                    "Originality of the idea",
                    "How much the project differs from existing solutions, the freshness "
                            + "of the problem statement and approaches.",
                    25,
                    1),
            new DefaultCriterion(
                    "Technical feasibility",
                    "Realism of the technical plan, architecture, and execution roadmap.",
                    25,
                    2),
            new DefaultCriterion(
                    "Social and market impact",
                    "Benefit to the audience, social, environmental, or economic impact, "
                            + "and scaling potential.",
                    25,
                    3),
            new DefaultCriterion(
                    "Quality and completeness of materials",
                    "Structure of submitted documentation: budget, risk analysis, "
                            + "monetization, and compliance with call requirements.",
                    25,
                    4)
    );

    private final CriteriaRepository criteriaRepository;
    private final CallRepository callRepository;

    /** Protection against double creation under parallel initial requests. */
    private final ConcurrentHashMap<Long, Object> localLocks = new ConcurrentHashMap<>();

    public CommissionCriteriaDefaultSeeder(
            CriteriaRepository criteriaRepository,
            CallRepository callRepository) {
        this.criteriaRepository = criteriaRepository;
        this.callRepository = callRepository;
    }

    @Transactional
    public List<Criteria> listForCallEnsuringDefaults(Long callId) {
        if (callId == null) {
            return List.of();
        }
        List<Criteria> first = criteriaRepository.findByCall_IdOrderBySortOrderAsc(callId);
        if (!first.isEmpty()) {
            return first;
        }

        Long key = Objects.requireNonNull(callId);
        synchronized (localLocks.computeIfAbsent(key, k -> new Object())) {
            List<Criteria> again = criteriaRepository.findByCall_IdOrderBySortOrderAsc(callId);
            if (!again.isEmpty()) {
                return again;
            }

            Call call = callRepository.findById(callId).orElse(null);
            if (call == null) {
                return List.of();
            }

            for (DefaultCriterion d : DEFAULT_SET) {
                Criteria c = new Criteria();
                c.setCall(call);
                c.setName(d.name());
                c.setDescription(d.description());
                c.setWeightPercent(d.weightPercent());
                c.setSortOrder(d.sortOrder());
                c.setMaxScore(100.0);
                criteriaRepository.save(c);
            }
            return criteriaRepository.findByCall_IdOrderBySortOrderAsc(callId);
        }
    }
}
