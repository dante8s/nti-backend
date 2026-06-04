package com.nti.nti_backend.team;

import com.nti.nti_backend.teamMember.TeamMember;
import com.nti.nti_backend.user.Role;
import com.nti.nti_backend.user.User;
import com.nti.nti_backend.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;
    private final UserRepository userRepository;

    public TeamController(TeamService teamService, UserRepository userRepository) {
        this.teamService = teamService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> createTeam(
            @AuthenticationPrincipal User authUser,
            @RequestBody CreateTeamRequest request) {
        if (!canActForUser(authUser, request.leaderId())) {
            return ResponseEntity.status(403)
                    .body(Map.of("message", "Не можна створити команду від імені іншого користувача або без авторизації."));
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
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }


    @GetMapping("/user/{userId}/removal-notice")
    @PreAuthorize("hasAnyRole('STUDENT' , 'ADMIN' , 'SUPER_ADMIN')")
    public ResponseEntity<TeamRemovalNotice> getRemovalNotice(
        @AuthenticationPrincipal User authUser , 
        @PathVariable Long userId ) {
            if  (!canActForUser(authUser , userId)) {
                return ResponseEntity.status(403).build();
            }
            return teamService.getRemovalNoticeForUser(userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
        }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN','EVALUATOR','SUPER_EVALUATOR')")
    public ResponseEntity<TeamResponse> getTeamForUser(
            @AuthenticationPrincipal User authUser,
            @PathVariable Long userId) {
        if (!canActForUser(authUser, userId) && !isCommissionViewer(authUser)) {
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

    /** Вхідні запрошення поточного користувача (id з JWT, без помилок localStorage). */
    @GetMapping("/me/invites")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<TeamMemberResponse>> getMyPendingInvites(
            @AuthenticationPrincipal User authUser) {
        if (authUser == null || authUser.getId() == null) {
            return ResponseEntity.status(401).build();
        }
        List<TeamMemberResponse> response = teamService.getPendingInvitesForUser(authUser.getId())
                .stream()
                .map(this::toMemberResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    /** Declare before `/{teamId}` so `/invites/...` is not mistaken for team id (some setups). */
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

    @GetMapping("/{teamId}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN','EVALUATOR','SUPER_EVALUATOR')")
    public ResponseEntity<TeamResponse> getTeam(
            @AuthenticationPrincipal User authUser,
            @PathVariable Long teamId) {
        try {
            Team team = teamService.getTeamWithMembers(teamId);
            if (!isAdmin(authUser) && !isMemberOfTeam(team, authUser.getId())
                    && !isCommissionViewer(authUser)) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.ok(toResponse(team));
        } catch (IllegalStateException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{teamId}/invite")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> inviteMember(
            @AuthenticationPrincipal User authUser,
            @PathVariable Long teamId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String email
    ) {
        try {
            Team team = teamService.getTeamWithMembers(teamId);
            if (!isAdmin(authUser) && !team.getLeader().getId().equals(authUser.getId())) {
                return ResponseEntity.status(403).build();
            }
            Long targetUserId = resolveInviteTargetUserId(userId, email);
            TeamMember member = teamService.inviteMember(teamId, targetUserId);
            return ResponseEntity.ok(toMemberResponse(member));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }


    @DeleteMapping("/{teamId}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> deleteTeam(
            @AuthenticationPrincipal User authUser,
            @PathVariable Long teamId) {
        try {
            Team team = teamService.getTeamWithMembers(teamId);
            if (!isAdmin(authUser) && !team.getLeader().getId().equals(authUser.getId())) {
                return ResponseEntity.status(403)
                        .body(Map.of("message", "Лише лідер команди може видалити команду."));
            }
            teamService.deleteTeam(teamId, authUser.getId(), isAdmin(authUser));
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{teamId}/members/{memberUserId}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> removeMember(
        @AuthenticationPrincipal User authUser , 
        @PathVariable Long teamId , 
        @PathVariable Long memberUserId
    ){
        try {
            teamService.removeMember(
                    teamId, memberUserId, authUser.getId(), isAdmin(authUser));
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/{teamId}/respond")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> respondInvitation(
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
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private TeamResponse toResponse(Team team) {
        List<TeamMemberResponse> members = team.getMembers()
                .stream()
                .filter(m -> m.getInviteStatus() == TeamMember.InviteStatus.PENDING
                        || m.getInviteStatus() == TeamMember.InviteStatus.ACCEPTED)
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
        var user = member.getUser();
        String displayName = "—";
        String email = null;
        if (user != null) {
            email = user.getEmail();
            String name = user.getName();
            displayName = (name != null && !name.isBlank())
                    ? name
                    : ("Користувач #" + user.getId());
        }
        String teamName = member.getTeam() != null ? member.getTeam().getName() : null;
        return new TeamMemberResponse(
                member.getId(),
                member.getTeam().getId(),
                member.getUser().getId(),
                member.getRole().name(),
                member.getInviteStatus().name(),
                displayName,
                email,
                teamName
        );
    }

    private Long resolveInviteTargetUserId(Long userId, String email) {
        if (userId != null && userId > 0) {
            return userId;
        }
        if (email != null && !email.isBlank()) {
            return userRepository.findByEmail(email.trim().toLowerCase())
                    .or(() -> userRepository.findByEmail(email.trim()))
                    .orElseThrow(() -> new IllegalStateException(
                            "Користувача з email «" + email.trim() + "» не знайдено"))
                    .getId();
        }
        throw new IllegalStateException("Вкажіть ID користувача або email");
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
            String inviteStatus,
            String memberDisplayName,
            String memberEmail,
            String teamName
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

    private boolean isCommissionViewer(User authUser) {
        return authUser != null && (
                authUser.hasRole(Role.EVALUATOR)
                        || authUser.hasRole(Role.SUPER_EVALUATOR)
        );
    }

    private boolean isMemberOfTeam(Team team, Long userId) {
        return team.getMembers().stream()
                .anyMatch(m -> m.getUser().getId().equals(userId)
                        && m.getInviteStatus() == TeamMember.InviteStatus.ACCEPTED);
    }

}
