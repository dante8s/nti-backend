package com.nti.nti_backend.evaluation;

import com.nti.nti_backend.application.Application;
import com.nti.nti_backend.application.ApplicationRepository;
import com.nti.nti_backend.criteria.Criteria;
import com.nti.nti_backend.criteria.CriteriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final CriteriaRepository criteriaRepository;
    private final ApplicationRepository applicationRepository;

    public EvaluationService(EvaluationRepository evaluationRepository,
                             CriteriaRepository criteriaRepository,
                             ApplicationRepository applicationRepository) {
        this.evaluationRepository = evaluationRepository;
        this.criteriaRepository = criteriaRepository;
        this.applicationRepository = applicationRepository;
    }

    public Evaluation submitScore(Evaluation evaluation) {
        Long appId = evaluation.getApplication().getId();
        Long evaluatorId = evaluation.getEvaluator().getId();
        Long criteriaId = evaluation.getCriteria().getId();
        if (appId == null || evaluatorId == null || criteriaId == null) {
            throw new IllegalStateException("Application, evaluator and criteria IDs are required");
        }

        Application application = applicationRepository.findById(appId)
                .orElseThrow(() -> new IllegalStateException("Application not found " + appId));

        Criteria criteria = criteriaRepository.findById(criteriaId)
                .orElseThrow(() -> new IllegalStateException("Criteria not found " + criteriaId));

        Long appCallId = application.getCall().getId();
        Long criteriaCallId = criteria.getCall().getId();
        if (!appCallId.equals(criteriaCallId)) {
            throw new IllegalStateException(
                    "Criteria does not belong to application's call");
        }

        if (evaluation.getScore() < 0 || evaluation.getScore() > criteria.getMaxScore()) {
            throw new IllegalStateException("The score must be between 0 to " + criteria.getMaxScore());
        }

        return evaluationRepository
                .findByApplication_IdAndEvaluator_IdAndCriteria_Id(appId, evaluatorId, criteriaId)
                .map(existing -> {
                    existing.setScore(evaluation.getScore());
                    existing.setComment(evaluation.getComment());
                    existing.setRecommendation(evaluation.getRecommendation());
                    return evaluationRepository.save(existing);
                })
                .orElseGet(() -> evaluationRepository.save(evaluation));
    }

    @Transactional(readOnly = true)
    public List<Evaluation> getEvaluatorScoresForApplication(Long applicationId, Long evaluatorId) {
        return evaluationRepository.findByApplication_IdAndEvaluator_Id(applicationId, evaluatorId);
    }

    @Transactional(readOnly = true)
    public List<Evaluation> getAllScoresForApplication(Long applicationId) {
        return evaluationRepository.findByApplication_Id(applicationId);
    }

    @Transactional(readOnly = true)
    public Double getWeightedAverageScore(Long applicationId) {
        return evaluationRepository
                .findWeightAverageScoreByApplicationId(applicationId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Double getAverageScore(Long applicationId) {
        return evaluationRepository
                .findAverageScoreByApplicationId(applicationId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<Evaluation>> getEvaluationGroupByApplication(Long callId) {
        return evaluationRepository.findAllByCallId(callId)
                .stream()
                .collect(Collectors.groupingBy(e -> e.getApplication().getId()));
    }

    @Transactional(readOnly = true)
    public Map<Long, Double> getSummaryScoresForCall(Long callId) {
        List<Criteria> criteria = criteriaRepository.findByCall_IdOrderBySortOrderAsc(callId);
        Map<Long, Integer> weightByCriteriaId = criteria.stream()
                .collect(Collectors.toMap(Criteria::getId, c -> c.getWeightPercent() == null ? 0 : c.getWeightPercent()));

        return evaluationRepository.findAllByCallId(callId)
                .stream()
                .collect(Collectors.groupingBy(
                        e -> e.getApplication().getId(),
                        Collectors.collectingAndThen(Collectors.toList(),
                                appEvaluations -> {
                                    Map<Long, Double> avgByCriteria = appEvaluations.stream()
                                            .collect(Collectors.groupingBy(
                                                    e -> e.getCriteria().getId(),
                                                    Collectors.averagingDouble(Evaluation::getScore)
                                            ));
                                    double weightedSum = 0;
                                    int totalWeight = 0;
                                    for (Map.Entry<Long, Double> entry : avgByCriteria.entrySet()) {
                                        int weight = weightByCriteriaId.getOrDefault(entry.getKey(), 0);
                                        if (weight > 0) {
                                            weightedSum += entry.getValue() * weight;
                                            totalWeight += weight;
                                        }
                                    }
                                    return totalWeight > 0 ? weightedSum / totalWeight : null;
                                })
                ));
    }

    @Transactional(readOnly = true)
    public boolean isEvaluationComplete(Long appId, Long evaluatorId, Long callId) {
        Application application = applicationRepository.findById(appId)
                .orElseThrow(() -> new IllegalStateException("Application not found " + appId));
        Long actualCallId = application.getCall().getId();
        if (callId != null && !actualCallId.equals(callId)) {
            throw new IllegalStateException("Call does not match application");
        }

        long totalCriteria = criteriaRepository.countByCall_Id(actualCallId);
        long scoredCriteria = evaluationRepository.countByApplication_IdAndEvaluator_Id(appId, evaluatorId);

        return totalCriteria > 0 && scoredCriteria >= totalCriteria;
    }
}
