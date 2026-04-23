package com.nti.nti_backend.teamMember;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    List<TeamMember> findByUser_IdAndInviteStatus(Long userId, TeamMember.InviteStatus status);

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
}
