package com.nti.nti_backend.organization;

import com.nti.nti_backend.organization.dto.*;
import com.nti.nti_backend.organization.entity.OrgMember;
import com.nti.nti_backend.organization.entity.OrgMemberRole;
import com.nti.nti_backend.organization.entity.OrgStatus;
import com.nti.nti_backend.organization.entity.Organization;
import com.nti.nti_backend.organization.exception.ConflictException;
import com.nti.nti_backend.organization.exception.ResourceNotFoundException;
import com.nti.nti_backend.organization.repository.OrgMemberRepository;
import com.nti.nti_backend.organization.repository.OrganizationRepository;
import com.nti.nti_backend.user.Role;
import com.nti.nti_backend.user.User;
import com.nti.nti_backend.user.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository orgRepository;
    private final OrgMemberRepository memberRepository;
    private final UserRepository userRepository;

    // get authenticated user from JWT
    private User getCurrentUser() {

        return (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    // CREATE
    @Transactional
    public OrganizationResponseDTO create(OrganizationRequestDTO dto) {
        if (orgRepository.existsByIco(dto.getIco())) {
            throw new ConflictException(
              "Organization with ICO '" + dto.getIco() + "' already exists"
            );
        }

        User currentUser = getCurrentUser();
        if (currentUser.hasRole(Role.FIRM)) {
            boolean alreadyOwnsOne = memberRepository
                    .findAllByUserId(currentUser.getId())
                    .stream()
                    .anyMatch(m -> m.getRole() == OrgMemberRole.OWNER);
            if (alreadyOwnsOne) {
                throw new ConflictException("A FIRM account can only register one organization");
            }
        }

        Organization org = Organization.builder()
                .name(dto.getName())
                .ico(dto.getIco())
                .sector(dto.getSector())
                .description(dto.getDescription())
                .contactEmail(dto.getContactEmail())
                .contactPhone(dto.getContactPhone())
                .website(dto.getWebsite())
                .status(OrgStatus.PENDING)
                .build();

        org = orgRepository.save(org);

        OrgMember owner = OrgMember.builder()
                .organization(org)
                .user(getCurrentUser())
                .role(OrgMemberRole.OWNER)
                .build();

        memberRepository.save(owner);

        return toResponseDTO(orgRepository.findByIdWithMembers(org.getId()).orElseThrow());
    }

    // Read All
    @Transactional(readOnly = true)
    public List<OrganizationResponseDTO> findAll() {
        return orgRepository.findAll().stream()
                .map(this::toResponseDTOSlim)
                .toList();
    }

    //Read One
    @Transactional(readOnly = true)
    public OrganizationResponseDTO findById(UUID id) {
        Organization org = orgRepository.findByIdWithMembers(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));
        return toResponseDTO(org);
    }

    // Update
    @Transactional
    public OrganizationResponseDTO update(UUID id, OrganizationRequestDTO dto) {
        Organization org = orgRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));
        Long currentUserId = getCurrentUser().getId();

        OrgMember membership = memberRepository
                .findByOrganizationIdAndUserId(id, currentUserId)
                .orElseThrow(() -> new ConflictException("You are not a member of this organization"));

        if (membership.getRole() != OrgMemberRole.OWNER) {
            throw new ConflictException("Only OWNER can edit this organization");
        }

        if (!org.getIco().equals(dto.getIco()) && orgRepository.existsByIco(dto.getIco())) {
            throw new ConflictException("ICO '" + dto.getIco() + "' already exists");
        }

        org.setName(dto.getName());
        org.setIco(dto.getIco());
        org.setSector(dto.getSector());
        org.setDescription(dto.getDescription());
        org.setContactEmail(dto.getContactEmail());
        org.setContactPhone(dto.getContactPhone());
        org.setWebsite(dto.getWebsite());

        orgRepository.save(org);
        return toResponseDTO(orgRepository.findByIdWithMembers(org.getId()).orElseThrow());
    }

    // Delete
    @Transactional
    public void delete(UUID id) {
        Organization org = orgRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));

        Long currentUserId = getCurrentUser().getId();

        OrgMember membership = memberRepository
                .findByOrganizationIdAndUserId(id, currentUserId)
                .orElseThrow(() -> new ConflictException("You are not a member of this organization"));

        if (membership.getRole() != OrgMemberRole.OWNER) {
            throw new ConflictException("Only OWNER can remove this organization");
        }

        orgRepository.delete(org);

    }

    // GET members
    @Transactional(readOnly = true)
    public List<OrgMemberDTO> getMembers(UUID orgId) {
        if (!orgRepository.existsById(orgId)) {
            throw new ResourceNotFoundException("Organization not found: " + orgId);
        }
        return memberRepository.findAllByOrganizationId(orgId)
                .stream()
                .map(m -> OrgMemberDTO.builder()
                        .id(m.getId())
                        .userId(m.getUser().getId())
                        .userName(m.getUser().getName())
                        .userEmail(m.getUser().getEmail())
                        .role(m.getRole())
                        .joinedAt(m.getJoinedAt())
                        .build()
                ).toList();
    }

    // ADD Member
    @Transactional
    public OrgMemberDTO addMember(UUID orgId, AddMemberRequestDTO dto) {
        Organization org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + orgId));

        Long currentUserId = getCurrentUser().getId();
        boolean isAdmin = getCurrentUser().hasRole(Role.ADMIN);
        if (!isAdmin) {
            OrgMember requestingMember = memberRepository
                    .findByOrganizationIdAndUserId(orgId,currentUserId)
                    .orElseThrow(() -> new ConflictException("You are not a member of this organization"));
            if (requestingMember.getRole() != OrgMemberRole.OWNER) {
                throw new ConflictException("Only OWNER or ADMIN can add members to this organization");
            }
        }

        User userToAdd = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No user found with email: " + dto.getEmail()
                ));

        if (memberRepository.existsByOrganizationIdAndUserId(orgId, userToAdd.getId())) {
            throw new ConflictException(
                    "User " + dto.getEmail() + " already exists"
            );
        }

        OrgMemberRole roleToAssign = dto.getRole() == OrgMemberRole.OWNER
                ? OrgMemberRole.MEMBER : dto.getRole();
        OrgMember newMember = OrgMember.builder()
                .organization(org)
                .user(userToAdd)
                .role(roleToAssign)
                .build();

        newMember = memberRepository.save(newMember);

        return OrgMemberDTO.builder()
                .id(newMember.getId())
                .userId(userToAdd.getId())
                .userName(userToAdd.getName())
                .userEmail(userToAdd.getEmail())
                .role(newMember.getRole())
                .joinedAt(newMember.getJoinedAt())
                .build();
    }

    // GET my organization
    @Transactional(readOnly = true)
    public List<OrganizationResponseDTO> getMyOrganizations() {
        Long currentUserId = getCurrentUser().getId();
        return memberRepository.findAllByUserId(currentUserId)
                .stream()
                .map(m -> toResponseDTOSlim(m.getOrganization()))
                .toList();
    }

    // REMOVE Member
    public void removeMember(UUID orgId, UUID memberId) {
        if (!orgRepository.existsById(orgId)) {
            throw new ResourceNotFoundException("Organization not found with id: " + orgId);
        }

        Long currentUserId = getCurrentUser().getId();
        boolean isAdmin = getCurrentUser().hasRole(Role.ADMIN);
        OrgMember requestingMember = memberRepository
                .findByOrganizationIdAndUserId(orgId, currentUserId)
                .orElseThrow(() -> new ConflictException("You are not a member of this organization"));

        if (requestingMember.getRole() != OrgMemberRole.OWNER || !isAdmin) {
            throw new ConflictException("Only the OWNER or ADMIN can remove members from this organization");
        }

        OrgMember memberToRemove = memberRepository.findById(memberId).orElseThrow(
                () -> new ResourceNotFoundException("Member not found with id: " + memberId)
        );

        if (memberToRemove.getRole() == OrgMemberRole.OWNER) {
            throw new ConflictException(
                    "Owner cannot be removed"
            );
        }

        if (!memberToRemove.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Member not found in this organization");
        }

        memberRepository.delete(memberToRemove);
    }

    @Transactional
    public OrgMemberDTO transferOwnership(UUID orgId, UUID newOwnerMemberId) {
        if (!orgRepository.existsById(orgId)) {
            throw new ResourceNotFoundException("Organization not found with id: " + orgId);
        }

        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.hasRole(Role.ADMIN);

        if (!isAdmin) {
            throw new ConflictException("Only ADMIN can transfer organization ownership");
        }

        OrgMember newOwner = memberRepository.findById(newOwnerMemberId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member not found: " + newOwnerMemberId
                ));
        if (!newOwner.getOrganization().getId().equals(orgId)) {
            throw new ResourceNotFoundException("Member not found in this organization");
        }

        if (newOwner.getRole() == OrgMemberRole.OWNER) {
            throw new ConflictException("This member is already the OWNER");
        }

        OrgMember currentOwner = memberRepository.findAllByOrganizationId(orgId)
                .stream()
                .filter(m -> m.getRole() == OrgMemberRole.OWNER)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No current OWNER found for this organization"
                ));
        currentOwner.setRole(OrgMemberRole.MEMBER);
        memberRepository.save(currentOwner);

        // Promote new Owner
        newOwner.setRole(OrgMemberRole.OWNER);
        memberRepository.save(newOwner);

        return OrgMemberDTO.builder()
                .id(newOwner.getId())
                .userId(newOwner.getUser().getId())
                .userName(newOwner.getUser().getName())
                .userEmail(newOwner.getUser().getEmail())
                .role(newOwner.getRole())
                .joinedAt(newOwner.getJoinedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<PublicOrganizationDTO> getPublicOrganizations() {
        return orgRepository.findAllByStatus(OrgStatus.ACTIVE)
                .stream()
                .map(org -> PublicOrganizationDTO.builder()
                        .id(org.getId())
                        .name(org.getName())
                        .sector(org.getSector())
                        .website(org.getWebsite())
                        .description(org.getDescription())
                        .build()
                ).toList();
    }

    @Transactional
    public OrganizationResponseDTO changeStatus(UUID id, OrgStatus newStatus) {
        User currentUser = getCurrentUser();
        if (!currentUser.hasRole(Role.ADMIN)) {
            throw new ConflictException("Only ADMIN can change organization status");
        }

        Organization org = orgRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization not found with id: " + id
                ));
        org.setStatus(newStatus);
        return toResponseDTOSlim(orgRepository.save(org));
    }

    // Mapping
    private OrganizationResponseDTO toResponseDTO(Organization org) {
        List<OrgMemberDTO> memberDTOs = org.getMembers().stream()
                .map(m -> OrgMemberDTO.builder()
                        .id(m.getId())
                        .userId(m.getUser().getId())
                        .userName(m.getUser().getName())
                        .userEmail(m.getUser().getEmail())
                        .role(m.getRole())
                        .joinedAt(m.getJoinedAt())
                        .build()
                ).toList();
        return buildDTO(org, memberDTOs);
    }

    private OrganizationResponseDTO toResponseDTOSlim(Organization org) {
        return buildDTO(org, null);
    }

    private OrganizationResponseDTO buildDTO(Organization org, List<OrgMemberDTO> members) {
        return OrganizationResponseDTO.builder()
                .id(org.getId())
                .name(org.getName())
                .ico(org.getIco())
                .sector(org.getSector())
                .description(org.getDescription())
                .contactEmail(org.getContactEmail())
                .contactPhone(org.getContactPhone())
                .website(org.getWebsite())
                .status(org.getStatus())
                .members(members)
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .build();
    }

}
