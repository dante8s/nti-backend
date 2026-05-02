package com.nti.nti_backend.evaluation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CriteriaService {

    private final CriteriaRepository criteriaRepository;

    public CriteriaService(CriteriaRepository criteriaRepository) {
        this.criteriaRepository = criteriaRepository;
    }

    @Transactional(readOnly = true)
    public List<Criteria> getCriteriaForCall(Long callId) {
        return criteriaRepository.findByCall_IdOrderBySortOrderAsc(callId);
    }

    public Criteria createCriteria(Criteria criteria) {
        if (criteriaRepository.existsByCall_IdAndName(
                criteria.getCall().getId(), criteria.getName())) {
            throw new IllegalStateException("A criterion with that name already exists for this call");
        }

        validateTotalWeightForCreate(criteria.getCall().getId(), criteria.getWeightPercent());

        return criteriaRepository.save(criteria);
    }

    public Criteria updateCriteria(Long criteriaId, Criteria updated) {
        Criteria existing = criteriaRepository.findById(criteriaId)
                .orElseThrow(() -> new IllegalStateException("Criteria not found by that id" + criteriaId));

        validateTotalWeightForUpdate(existing.getCall().getId(), updated.getWeightPercent(), criteriaId);

        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setWeightPercent(updated.getWeightPercent());
        existing.setMaxScore(updated.getMaxScore());
        existing.setSortOrder(updated.getSortOrder());

        return criteriaRepository.save(existing);
    }

    public void deleteCriteria(Long criteriaId) {
        if (!criteriaRepository.existsById(criteriaId)) {
            throw new IllegalStateException("Criteria not found by that id" + criteriaId);
        }
        criteriaRepository.deleteById(criteriaId);
    }

    @Transactional(readOnly = true)
    public boolean isRubricValid(Long callId) {
        long count = criteriaRepository.countByCall_Id(callId);
        if (count == 0) {
            return false;
        }
        Integer totalWeight = criteriaRepository.sumWeightPercentByCallId(callId);
        return totalWeight != null && totalWeight == 100;
    }

    private void validateTotalWeightForCreate(Long callId, Integer newWeight) {
        Integer currentTotal = criteriaRepository.sumWeightPercentByCallId(callId);
        int total = currentTotal == null ? 0 : currentTotal;
        int nw = newWeight == null ? 0 : newWeight;
        if (total + nw > 100) {
            throw new IllegalStateException(
                    "Total weight for this call would exceed 100%. Current total: " + total);
        }
    }

    private void validateTotalWeightForUpdate(Long callId, Integer newWeight, Long editingCriteriaId) {
        Criteria editing = criteriaRepository.findById(editingCriteriaId)
                .orElseThrow(() -> new IllegalStateException("Criteria not found"));
        Integer currentTotal = criteriaRepository.sumWeightPercentByCallId(callId);
        int total = currentTotal == null ? 0 : currentTotal;
        int oldWeight = editing.getWeightPercent() == null ? 0 : editing.getWeightPercent();
        int nw = newWeight == null ? 0 : newWeight;
        if (total - oldWeight + nw > 100) {
            throw new IllegalStateException(
                    "Total weight for this call would exceed 100%. Current total: " + total);
        }
    }
}
