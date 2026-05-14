package com.nti.nti_backend.organization;

import com.nti.nti_backend.auth.InviteOrgMemberRequest;
import com.nti.nti_backend.organization.dto.*;
import com.nti.nti_backend.organization.entity.OrgStatus;
import com.nti.nti_backend.organization.repository.OrganizationRepository;
import com.nti.nti_backend.program.ProgramBRequirementsDTO;
import com.nti.nti_backend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService orgService;
    private final OrganizationRepository organizationRepository;

    @PostMapping("/organizations")
    public ResponseEntity<OrganizationResponseDTO> create(
            @Valid @RequestBody OrganizationRequestDTO dto
    ) {
        OrganizationResponseDTO created = orgService.create(dto);
        return ResponseEntity
                .created(URI.create("/api/organizations/" + created.getId()))
                .body(created);
    }

    // Get /api/organizations
    @GetMapping("/organizations")
    public ResponseEntity<List<OrganizationResponseDTO>> findAll() {
        return ResponseEntity.ok(orgService.findAll());
    }

    @GetMapping("/organizations/my")
    public ResponseEntity<List<OrganizationResponseDTO>> getMyOrganizations() {
        return ResponseEntity.ok(orgService.getMyOrganizations());
    }

    // Get /api/organizations/{id}
    @GetMapping("/organizations/{id}")
    public ResponseEntity<OrganizationResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(orgService.findById(id));
    }

    // PUT /api/organizations/{id}
    @PutMapping("/organizations/{id}")
    public ResponseEntity<OrganizationResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody OrganizationRequestDTO dto
    ) {
        return ResponseEntity.ok(orgService.update(id, dto));
    }

    // DELETE /api/organizations/{id}
    @DeleteMapping("/organizations/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        orgService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/organizations/{id}/members
    @GetMapping("/organizations/{id}/members")
    public ResponseEntity<List<OrgMemberDTO>> getMembers(@PathVariable UUID id) {
        return ResponseEntity.ok(orgService.getMembers(id));
    }

    // POST /api/organizations/{id}/members
    @PostMapping("/organizations/{id}/members")
    public ResponseEntity<OrgMemberDTO> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequestDTO dto
    ) {
        OrgMemberDTO added = orgService.addMember(id, dto);
        return ResponseEntity.status(201).body(added);
    }

    // POST /api/organizations/{id}/invite-member
    @PostMapping("/organizations/{id}/invite-member")
    public ResponseEntity<Void> inviteMember(
            @PathVariable UUID id,
            @RequestBody InviteOrgMemberRequest request
            ) {
        orgService.inviteMember(id, request);
        return ResponseEntity.ok().build();
    }

    // PATCH /api/organizations/{id}/transfer-ownership/{memberId}
    @PatchMapping("/organizations/{id}/transfer-ownership/{memberId}")
    public ResponseEntity<OrgMemberDTO> transferOwnership(
            @PathVariable UUID id,
            @PathVariable UUID memberId
    ) {
        return ResponseEntity.ok(orgService.transferOwnership(id, memberId));
    }

    // DELETE /api/organizations/{id}/members/{memberId}
    @DeleteMapping("/organizations/{id}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID memberId
    ) {
        orgService.removeMember(id, memberId);
        return ResponseEntity.noContent().build();
    }

    // GET /api/public/organizations
    @GetMapping("/public/organizations")
    public ResponseEntity<List<PublicOrganizationDTO>> getPublicOrganizations() {
        return ResponseEntity.ok(orgService.getPublicOrganizations());
    }

    // PATCH /api/organizations/{id}/status
    @PatchMapping("organizations/{id}/status")
    public ResponseEntity<OrganizationResponseDTO> changeStatus(
            @PathVariable UUID id,
            @RequestParam OrgStatus status
    ) {
        return ResponseEntity.ok(orgService.changeStatus(id, status));
    }

    @PostMapping("/program-b-requirements/{programId}/specification")
    @PreAuthorize("hasAnyRole('FIRM', 'FIRM_USER')")
    public ResponseEntity<ProgramBRequirementsDTO> uploadSpec(
            @PathVariable Long programId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(orgService.uploadSpecification(programId, file, user));
    }

    @PostMapping("/program-b-requirements/{programId}/budget")
    @PreAuthorize("hasAnyRole('FIRM', 'FIRM_USER')")
    public ResponseEntity<ProgramBRequirementsDTO> uploadBudget(
            @PathVariable Long programId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(orgService.uploadBudget(programId, file, user));
    }

    @GetMapping("/program-b-requirements/{programId}")
    public ResponseEntity<ProgramBRequirementsDTO> getProgramBRequirements(
            @PathVariable Long programId
    ) {
        return ResponseEntity.ok(orgService.getByProgram(programId));
    }

    // GET /api/organizations/requirements/{programId}/specification?inline=true
    @GetMapping("/requirements/{programId}/specification")
    public ResponseEntity<Resource> serveSpecification(
            @PathVariable Long programId,
            @RequestParam(defaultValue = "false") boolean inline,
            @AuthenticationPrincipal User user
    ) {
        return orgService.serveSpecification(programId, inline, user);
    }

    // GET /api/organizations/requirements/{programId}/budget?inline=true
    @GetMapping("/requirements/{programId}/budget")
    public ResponseEntity<Resource> serveBudget(
            @PathVariable Long programId,
            @RequestParam(defaultValue = "false") boolean inline,
            @AuthenticationPrincipal User user
    ) {
        return orgService.serveBudget(programId, inline, user);
    }
}
