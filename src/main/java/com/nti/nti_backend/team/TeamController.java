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
//            Team full = teamService.getTeamWithMembers(saved.getId());
//            return ResponseEntity.ok(toResponse(full));
            return ResponseEntity.ok(teamService.getTeamResponseById(saved.getId()));
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
            return ResponseEntity.ok(teamService.getTeamResponseForUser(userId));
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
        return ResponseEntity.ok(teamService.getPendingInviteResponsesForUser(authUser.getId()));
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
        return ResponseEntity.ok(teamService.getPendingInviteResponsesForUser(userId));
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
            return ResponseEntity.ok(teamService.getTeamResponseById(teamId));
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
            return ResponseEntity.ok(teamService.toMemberResponse(member));
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
            return ResponseEntity.ok(teamService.toMemberResponse(member));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
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
