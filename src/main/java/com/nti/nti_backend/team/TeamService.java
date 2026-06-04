package com.nti.nti_backend.team;

import com.nti.nti_backend.teamMember.TeamMember;
import com.nti.nti_backend.teamMember.TeamMemberRepository;
import com.nti.nti_backend.user.User;
import com.nti.nti_backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    public TeamService(TeamRepository teamRepository,
                       TeamMemberRepository teamMemberRepository,
                       UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
    }

    public Team createTeam(Team team) {
        if (team.getLeader() == null || team.getLeader().getId() == null) {
            throw new IllegalStateException("Team leader is required");
        }
        if (teamRepository.existsByName(team.getName())) {
            throw new IllegalStateException("Team already exists: " + team.getName());
        }
        User leader = userRepository.findById(team.getLeader().getId())
                .orElseThrow(() -> new IllegalStateException("Leader user not found"));
        if (teamRepository.findByLeader_Id(team.getLeader().getId()).isPresent()) {
            throw new IllegalStateException("User is already leader of a team");
        }
        if (team.getMaxCapacity() == null || team.getMaxCapacity() < 1) {
            throw new IllegalStateException("Team max capacity must be at least 1");
        }
        if (teamMemberRepository.existsByUser_IdAndInviteStatus(leader.getId(), TeamMember.InviteStatus.ACCEPTED)) {
            throw new IllegalStateException("User already belongs to another team");
        }
        team.setLeader(leader);

        Team saved = teamRepository.save(team);

        TeamMember leaderMember = new TeamMember();
        leaderMember.setTeam(saved);
        leaderMember.setUser(saved.getLeader());
        leaderMember.setRole(TeamMember.TeamRole.LEADER);
        leaderMember.setInviteStatus(TeamMember.InviteStatus.ACCEPTED);
        leaderMember.setInvitedAt(LocalDateTime.now());
        teamMemberRepository.save(leaderMember);

        return saved;
    }

    @Transactional(readOnly = true)
    public Team getTeamForUser(Long userId) {
        return teamRepository.findByLeader_Id(userId)
                .orElseGet(() -> {
                    List<Team> teams = teamRepository.findAcceptedTeamsByUserId(userId);
                    if (teams.isEmpty()) {
                        throw new IllegalStateException("User is not a member of any team");
                    }
                    return teams.get(0);
                });
    }

    @Transactional(readOnly = true)
    public Team getTeamWithMembers(Long teamId) {
        return teamRepository.findByIdWithMembers(teamId)
                .orElseThrow(() -> new IllegalStateException("Team not found: " + teamId));
    }

    public TeamMember inviteMember(Long teamId, Long invitedUserId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalStateException("Команду не знайдено"));
        if (team.getLeader().getId().equals(invitedUserId)) {
            throw new IllegalStateException("Лідер не може запросити сам себе");
        }
        User invitedUser = userRepository.findById(invitedUserId)
                .orElseThrow(() -> new IllegalStateException(
                        "Користувача з id " + invitedUserId + " не існує в системі."));

        if (teamMemberRepository.countAcceptedInOtherTeam(invitedUserId, teamId) > 0) {
            throw new IllegalStateException(
                    "Користувач уже підтверджений учасником іншої команди");
        }

        long currentSize = teamMemberRepository.countAcceptedMembers(teamId);
        if (currentSize >= team.getMaxCapacity()) {
            throw new IllegalStateException("У команді вже максимум учасників");
        }

        Optional<TeamMember> existing =
                teamMemberRepository.findByTeam_IdAndUser_Id(teamId, invitedUserId);
        if (existing.isPresent()) {
            TeamMember row = existing.get();
            if (row.getInviteStatus() == TeamMember.InviteStatus.ACCEPTED) {
                throw new IllegalStateException("Користувач уже в складі команди");
            }
            // PENDING, REMOVED, DECLINED — повторне запрошення (необмежена кількість разів)
            row.setInviteStatus(TeamMember.InviteStatus.PENDING);
            row.setRole(TeamMember.TeamRole.MEMBER);
            row.setInvitedAt(LocalDateTime.now());
            row.setRespondedAt(null);
            row.setJoinedAt(null);
            return teamMemberRepository.save(row);
        }

        TeamMember invite = new TeamMember();
        invite.setTeam(team);
        invite.setUser(invitedUser);
        invite.setRole(TeamMember.TeamRole.MEMBER);
        invite.setInviteStatus(TeamMember.InviteStatus.PENDING);
        invite.setInvitedAt(LocalDateTime.now());

        return teamMemberRepository.save(invite);
    }

    public TeamMember respondToInvitation(Long teamId, Long userId, boolean accepted) {
        TeamMember membership = teamMemberRepository.findByTeam_IdAndUser_Id(teamId, userId)
                .orElseThrow(() -> new IllegalStateException("Запрошення не знайдено"));

        if (membership.getInviteStatus() != TeamMember.InviteStatus.PENDING) {
            throw new IllegalStateException("На це запрошення вже відповіли");
        }

        // Перевірки тільки поки в БД ще PENDING — інакше flush може бачити цей рядок як ACCEPTED
        // і existsByUser_IdAndInviteStatus хибно спрацює.
        if (accepted) {
            if (teamMemberRepository.countAcceptedInOtherTeam(userId, teamId) > 0) {
                throw new IllegalStateException(
                        "Ви вже підтверджені учасником іншої команди. Спочатку вийдіть з неї, щоб приєднатися до цієї.");
            }
            long currentSize = teamMemberRepository.countAcceptedMembers(teamId);
            if (currentSize >= membership.getTeam().getMaxCapacity()) {
                throw new IllegalStateException("У команді вже максимум учасників");
            }
        }

        membership.setInviteStatus(
                accepted ? TeamMember.InviteStatus.ACCEPTED : TeamMember.InviteStatus.DECLINED);
        membership.setRespondedAt(LocalDateTime.now());

        if (accepted) {
            membership.setJoinedAt(LocalDateTime.now());
        }

        return teamMemberRepository.save(membership);
    }

    @Transactional(readOnly = true)
    public List<TeamMember> getPendingInvitesForUser(Long userId) {
        return teamMemberRepository.findByUser_IdAndInviteStatusJoinTeamAndUser(
                userId, TeamMember.InviteStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public long countAllTeams() {
        return teamRepository.count();
    }

    @Transactional(readOnly = true)
    public List<Team> getEligibleTeams() {
        return teamRepository.findTeamsWithMinimumSize(3);
    }

    /**
     * Лідер скасовує запрошення (PENDING) або виключає учасника (ACCEPTED).
     * Статус REMOVED — для повідомлення на сторінці «Моя команда».
     */
    public void removeMember(Long teamId , Long memberUserId , Long requesterUserId , boolean admin) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalStateException("Team not found"));
        if (!admin && !team.getLeader().getId().equals(requesterUserId)) {
            throw  new IllegalStateException("Only leader can remove members");
        }
        if (team.getLeader().getId().equals(memberUserId)) {
            throw new IllegalStateException("Leader cannot be removed from the team");
        }
        TeamMember membership = teamMemberRepository.findByTeam_IdAndUser_Id(teamId, memberUserId)
            .orElseThrow(() -> new IllegalStateException("Учасника не знайдено в цій команді"));

        TeamMember.InviteStatus st = membership.getInviteStatus();

        if(st != TeamMember.InviteStatus.PENDING && st != TeamMember.InviteStatus.ACCEPTED) {
            throw new IllegalStateException("Цього учасника вже немає в активному складі");
        }

        membership.setInviteStatus(TeamMember.InviteStatus.REMOVED);
        membership.setRespondedAt(LocalDateTime.now());
        teamMemberRepository.save(membership);
    }

    @Transactional(readOnly = true)
    public Optional<TeamRemovalNotice> getRemovalNoticeForUser(Long userId) {
        if(teamRepository.findByLeader_Id(userId).isPresent()) {
            return Optional.empty();
        }

        if (!teamRepository.findAcceptedTeamsByUserId(userId).isEmpty()) {
            return Optional.empty();
        }
        if (!teamMemberRepository
                .findByUser_IdAndInviteStatus(userId, TeamMember.InviteStatus.PENDING)
                .isEmpty()) {
            return Optional.empty();
        }

        List<TeamMember> removed =
                teamMemberRepository.findRemovedMembershipsForUser(userId);

        if (removed.isEmpty()) {
            return Optional.empty();
        }

        TeamMember lastRemoved = removed.get(0);
        return Optional.of(new TeamRemovalNotice(
            lastRemoved.getTeam().getId(),
            lastRemoved.getTeam().getName(),
            lastRemoved.getRespondedAt()
        ));
    }


    public void deleteTeam(Long teamId, Long requesterUserId, boolean admin) {
        Team team = teamRepository.findByIdWithMembers(teamId)
                .orElseThrow(() -> new IllegalStateException("Команду не знайдено"));

        Long leaderId = team.getLeader().getId();
        if (!admin && !leaderId.equals(requesterUserId)) {
            throw new IllegalStateException("Лише лідер команди може її видалити");
        }

        teamMemberRepository.deleteByTeam_Id(teamId);
        teamRepository.delete(team);
        teamRepository.flush();
    }
}
