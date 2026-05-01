package com.nti.nti_backend.team;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByLeader_Id(Long userId);

    boolean existsByName(String name);

    @Query("""
        SELECT t FROM Team t
        JOIN t.members m
        WHERE m.user.id = :userId
        AND m.inviteStatus = 'ACCEPTED'
        """)
    List<Team> findAcceptedTeamsByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT DISTINCT t FROM Team t
            LEFT JOIN FETCH t.members m
            LEFT JOIN FETCH m.user
            LEFT JOIN FETCH t.leader
            WHERE t.id = :id
            """)
    Optional<Team> findByIdWithMembers(@Param("id") Long id);

    @Query("""
        SELECT t FROM Team t
        WHERE (SELECT COUNT(m) FROM TeamMember m
               WHERE m.team = t AND m.inviteStatus = 'ACCEPTED') >= :minSize
        """)
    List<Team> findTeamsWithMinimumSize(@Param("minSize") int minSize);
}
