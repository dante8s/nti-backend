package com.nti.nti_backend.evaluation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CriteriaRepository extends JpaRepository<Criteria, Long> {

    List<Criteria> findByCall_IdOrderBySortOrderAsc(Long callId);

    long countByCall_Id(Long callId);

    boolean existsByCall_IdAndName(Long callId, String name);

    @Query("""
        SELECT SUM(c.weightPercent)
        FROM Criteria c
        WHERE c.call.id = :callId
        """)
    Integer sumWeightPercentByCallId(@Param("callId") Long callId);
}
