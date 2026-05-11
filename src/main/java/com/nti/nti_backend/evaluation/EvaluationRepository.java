package com.nti.nti_backend.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    List<Evaluation> findByApplication_Id(Long applicationId);

    List<Evaluation> findByApplication_IdAndEvaluator_Id(Long applicationId, Long evaluatorId);

    Optional<Evaluation> findByApplication_IdAndEvaluator_IdAndCriteria_Id(
            Long applicationId, Long evaluatorId, Long criteriaId);

    @Query("""
           SELECT SUM(e.score * c.weightPercent) / SUM(c.weightPercent)
           FROM Evaluation e
           JOIN e.criteria c
           WHERE e.application.id = :applicationId
        """)
    Optional<Double> findWeightAverageScoreByApplicationId(@Param("applicationId") Long applicationId);

    @Query("""
        SELECT AVG(e.score)
        FROM Evaluation e
        WHERE e.application.id = :applicationId
    """)
    Optional<Double> findAverageScoreByApplicationId(@Param("applicationId") Long applicationId);

    @Query("""
        SELECT e FROM Evaluation e
        JOIN e.application a
        WHERE a.call.id = :callId
    """)
    List<Evaluation> findAllByCallId(@Param("callId") Long callId);

    long countByApplication_Id(Long applicationId);

    long countByApplication_IdAndEvaluator_Id(Long applicationId, Long evaluatorId);

    @Query("""
        SELECT COUNT(e) FROM Evaluation e
        JOIN e.application a
        WHERE a.call.id = :callId
        """)
    long countEvaluationsForCall(@Param("callId") Long callId);
}
