package com.nti.nti_backend.teamMember;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    List<TeamMember> findByUser_IdAndInviteStatus(Long userId, TeamMember.InviteStatus status);

    @Query("""
            SELECT DISTINCT m FROM TeamMember m
            JOIN FETCH m.team
            JOIN FETCH m.user
            WHERE m.user.id = :userId AND m.inviteStatus = :status
            """)
    List<TeamMember> findByUser_IdAndInviteStatusJoinTeamAndUser(
            @Param("userId") Long userId,
            @Param("status") TeamMember.InviteStatus status);

    List<TeamMember> findByTeam_IdAndInviteStatus(Long teamId, TeamMember.InviteStatus status);

    List<TeamMember> findByTeam_Id(Long teamId);

    boolean existsByTeam_IdAndUser_Id(Long teamId, Long userId);

    Optional<TeamMember> findByTeam_IdAndUser_Id(Long teamId, Long userId);

    boolean existsByUser_IdAndInviteStatus(Long userId, TeamMember.InviteStatus status);

    @Query("""
        SELECT COUNT(m) FROM TeamMember m
        WHERE m.team.id = :teamId
        AND m.inviteStatus = 'ACCEPTED'
        """)
    long countAcceptedMembers(@Param("teamId") Long teamId);

    long countByInviteStatus(TeamMember.InviteStatus status);

    /**
     * True when both users have ACCEPTED membership on the same team (teammates).
     */
    @Query("""
            SELECT COUNT(m1) FROM TeamMember m1, TeamMember m2
            WHERE m1.team.id = m2.team.id
              AND m1.user.id = :userA AND m2.user.id = :userB
              AND m1.inviteStatus = 'ACCEPTED' AND m2.inviteStatus = 'ACCEPTED'
            """)
    long countAcceptedCoMembership(
            @Param("userA") Long userA,
            @Param("userB") Long userB);
}
