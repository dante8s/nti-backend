package com.nti.nti_backend.team;

import com.nti.nti_backend.teamMember.TeamMember;
import com.nti.nti_backend.user.Role;
import com.nti.nti_backend.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<TeamResponse> createTeam(
            @AuthenticationPrincipal User authUser,
            @RequestBody CreateTeamRequest request) {
        if (!canActForUser(authUser, request.leaderId())) {
            return ResponseEntity.status(403).build();
        }
        try {
            Team team = new Team();
            team.setName(request.name());
            team.setMaxCapacity(request.maxCapacity() == null ? 5 : request.maxCapacity());
            team.setDesciption(request.description());
            team.setCompetencies(request.competencies());

            User leader = new User();
            leader.setId(request.leaderId());
            team.setLeader(leader);

            Team saved = teamService.createTeam(team);
            Team full = teamService.getTeamWithMembers(saved.getId());
            return ResponseEntity.ok(toResponse(full));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<TeamResponse> getTeamForUser(
            @AuthenticationPrincipal User authUser,
            @PathVariable Long userId) {
        if (!canActForUser(authUser, userId)) {
            return ResponseEntity.status(403).build();
        }
        try {
            Team team = teamService.getTeamForUser(userId);
            Team full = teamService.getTeamWithMembers(team.getId());
            return ResponseEntity.ok(toResponse(full));
        } catch (IllegalStateException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{teamId}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<TeamResponse> getTeam(
            @AuthenticationPrincipal User authUser,
            @PathVariable Long teamId) {
        try {
            Team team = teamService.getTeamWithMembers(teamId);
            if (!isAdmin(authUser) && !isMemberOfTeam(team, authUser.getId())) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.ok(toResponse(team));
        } catch (IllegalStateException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{teamId}/invite")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<TeamMemberResponse> inviteMember(
            @AuthenticationPrincipal User authUser,
            @PathVariable Long teamId,
            @RequestParam Long userId
    ) {
        try {
            Team team = teamService.getTeamWithMembers(teamId);
            if (!isAdmin(authUser) && !team.getLeader().getId().equals(authUser.getId())) {
                return ResponseEntity.status(403).build();
            }
            TeamMember member = teamService.inviteMember(teamId, userId);
            return ResponseEntity.ok(toMemberResponse(member));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{teamId}/respond")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<TeamMemberResponse> respondInvitation(
            @AuthenticationPrincipal User authUser,
            @PathVariable Long teamId,
            @RequestParam Long userId,
            @RequestParam boolean accepted
    ) {
        if (!canActForUser(authUser, userId)) {
            return ResponseEntity.status(403).build();
        }
        try {
            TeamMember member = teamService.respondToInvitation(teamId, userId, accepted);
            return ResponseEntity.ok(toMemberResponse(member));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/invites/{userId}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<TeamMemberResponse>> getPendingInvites(
            @AuthenticationPrincipal User authUser,
            @PathVariable Long userId) {
        if (!canActForUser(authUser, userId)) {
            return ResponseEntity.status(403).build();
        }
        List<TeamMemberResponse> response = teamService.getPendingInvitesForUser(userId)
                .stream()
                .map(this::toMemberResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    private TeamResponse toResponse(Team team) {
        List<TeamMemberResponse> members = team.getMembers()
                .stream()
                .map(this::toMemberResponse)
                .toList();

        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getLeader().getId(),
                team.getMaxCapacity(),
                team.getDesciption(),
                team.getCompetencies(),
                members
        );
    }

    private TeamMemberResponse toMemberResponse(TeamMember member) {
        return new TeamMemberResponse(
                member.getId(),
                member.getTeam().getId(),
                member.getUser().getId(),
                member.getRole().name(),
                member.getInviteStatus().name()
        );
    }

    public record CreateTeamRequest(
            String name,
            Long leaderId,
            Integer maxCapacity,
            String description,
            String competencies
    ) {
    }

    public record TeamResponse(
            Long id,
            String name,
            Long leaderId,
            Integer maxCapacity,
            String description,
            String competencies,
            List<TeamMemberResponse> members
    ) {
    }

    public record TeamMemberResponse(
            Long id,
            Long teamId,
            Long userId,
            String role,
            String inviteStatus
    ) {
    }

    private boolean canActForUser(User authUser, Long userId) {
        return authUser != null && userId != null && (
                userId.equals(authUser.getId()) || isAdmin(authUser)
        );
    }

    private boolean isAdmin(User authUser) {
        return authUser != null && (
                authUser.hasRole(Role.ADMIN)
                        || authUser.hasRole(Role.SUPER_ADMIN)
        );
    }

    private boolean isMemberOfTeam(Team team, Long userId) {
        return team.getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(userId)
                        && m.getInviteStatus() == TeamMember.InviteStatus.ACCEPTED);
    }

}
