package com.nti.nti_backend.reporting;

import com.nti.nti_backend.evaluation.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportingRepository extends JpaRepository<Evaluation, Long> {

    @Query("""
        SELECT e FROM Evaluation e
        JOIN FETCH e.criteria c
        JOIN FETCH e.application a
        WHERE a.call.id = :callId
        ORDER BY a.id ASC, c.sortOrder ASC
        """)
    List<Evaluation> findAllCallIdWithDetails(@Param("callId") Long callId);

    @Query("""
        SELECT COUNT(e) FROM Evaluation e
        JOIN e.application a
        WHERE a.call.id = :callId
        """)
    Long countEvaluationByCallId(@Param("callId") Long callId);

    @Query("""
        SELECT SUM(e.score * c.weightPercent) / SUM(c.weightPercent)
        FROM Evaluation e
        JOIN e.criteria c
        JOIN e.application a
        WHERE a.call.id = :callId
        """)
    Double findAverageWeightedScore(@Param("callId") Long callId);

    @Query("""
        SELECT COUNT(DISTINCT e.application.id) FROM Evaluation e
        JOIN e.application a
        WHERE a.call.id = :callId
        """)
    long countEvaluationApplicationByCallId(@Param("callId") Long callId);
}
