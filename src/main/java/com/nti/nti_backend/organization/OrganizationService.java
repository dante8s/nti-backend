package com.nti.nti_backend.organization;

import com.nti.nti_backend.auth.InviteOrgMemberRequest;
import com.nti.nti_backend.email.EmailService;
import com.nti.nti_backend.file.FileServeService;
import com.nti.nti_backend.organization.dto.*;
import com.nti.nti_backend.organization.entity.OrgMember;
import com.nti.nti_backend.organization.entity.OrgMemberRole;
import com.nti.nti_backend.organization.entity.OrgStatus;
import com.nti.nti_backend.organization.entity.Organization;
import com.nti.nti_backend.exception.AppException;
import com.nti.nti_backend.organization.exception.ConflictException;
import com.nti.nti_backend.organization.exception.ResourceNotFoundException;
import com.nti.nti_backend.organization.repository.OrgMemberRepository;
import com.nti.nti_backend.organization.repository.OrganizationRepository;
import com.nti.nti_backend.program.*;
import com.nti.nti_backend.user.AccountStatus;
import com.nti.nti_backend.user.Role;
import com.nti.nti_backend.user.User;
import com.nti.nti_backend.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

import static com.nti.nti_backend.config.CacheNames.*;
import org.springframework.cache.annotation.*;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository orgRepository;
    private final OrgMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ProgramBRequirementsRepository requirementsRepository;
    private final EmailService emailService;
    private final ProgramRepository programRepository;
    private final FileServeService fileServeService;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    // get authenticated user from JWT
    private User getCurrentUser() {

        return (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    private boolean isAdminOrSuperAdmin(User user) {
        return user.hasRole(Role.ADMIN) || user.hasRole(Role.SUPER_ADMIN);
    }

    private boolean isSuperAdmin(User user) {
        return user.hasRole(Role.SUPER_ADMIN);
    }

    // CREATE
    @Caching(evict = {
            @CacheEvict(value = ORGANIZATIONS, allEntries = true),
            @CacheEvict(value = ORGANIZATIONS_PUBLIC, allEntries = true)
    })
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
                .user(currentUser)
                .role(OrgMemberRole.OWNER)
                .build();

        memberRepository.save(owner);

        return toResponseDTO(orgRepository.findByIdWithMembers(org.getId()).orElseThrow());
    }

    // Read All
    @Cacheable(value = ORGANIZATIONS, unless = "#result == null || #result.isEmpty()")
    @Transactional(readOnly = true)
    public List<OrganizationResponseDTO> findAll() {
        return orgRepository.findAll().stream()
                .map(this::toResponseDTOSlim)
                .collect(Collectors.toList());
    }

    //Read One
    @Cacheable(value = ORGANIZATION, key = "#id")
    @Transactional(readOnly = true)
    public OrganizationResponseDTO findById(UUID id) {
        Organization org = orgRepository.findByIdWithMembers(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));
        return toResponseDTO(org);
    }

    // Update
    @Caching(evict = {
            @CacheEvict(value = ORGANIZATIONS, allEntries = true),
            @CacheEvict(value = ORGANIZATION, key = "#id"),
            @CacheEvict(value = ORGANIZATIONS_PUBLIC, allEntries = true)
    })
    @Transactional
    public OrganizationResponseDTO update(UUID id, OrganizationRequestDTO dto) {
        Organization org = orgRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));

        User currentUser = getCurrentUser();
        boolean canEdit = isAdminOrSuperAdmin(currentUser);

        if (!canEdit) {
            OrgMember membership = memberRepository
                    .findByOrganizationIdAndUserId(id, currentUser.getId())
                    .orElseThrow(() -> new ConflictException("You are not a member of this organization"));

            if (membership.getRole() != OrgMemberRole.OWNER) {
                throw new ConflictException("Only OWNER can edit this organization");
            }
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
    @Caching(evict = {
            @CacheEvict(value = ORGANIZATIONS, allEntries = true),
            @CacheEvict(value = ORGANIZATION, key = "#id"),
            @CacheEvict(value = ORGANIZATIONS_PUBLIC, allEntries = true),
            @CacheEvict(value = ORG_MEMBERS, key = "#id")
    })
    @Transactional
    public void delete(UUID id) {
        Organization org = orgRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));
        User currentUser = getCurrentUser();

        if (!isSuperAdmin(currentUser)) {
            OrgMember membership = memberRepository
                    .findByOrganizationIdAndUserId(id, currentUser.getId())
                    .orElseThrow(() -> new ConflictException("You are not a member of this organization"));

            if (membership.getRole() != OrgMemberRole.OWNER) {
                throw new ConflictException("Only OWNER can remove this organization");
            }
        }

        orgRepository.delete(org);

    }

    // GET members
    @Cacheable(value = ORG_MEMBERS, key = "#orgId")
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
                ).collect(Collectors.toList());
    }

    // ADD Member
    @CacheEvict(value = ORG_MEMBERS, key = "#orgId")
    @Transactional
    public OrgMemberDTO addMember(UUID orgId, AddMemberRequestDTO dto) {
        Organization org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + orgId));

        User currentUser = getCurrentUser();
        if (!isAdminOrSuperAdmin(currentUser)) {
            OrgMember requestingMember = memberRepository
                    .findByOrganizationIdAndUserId(orgId,currentUser.getId())
                    .orElseThrow(() -> new ConflictException("You are not a member of this organization"));
            if (requestingMember.getRole() != OrgMemberRole.OWNER) {
                throw new ConflictException("Only OWNER or ADMIN can add members to this organization");
            }
        }

        User userToAdd = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No user found with email: " + dto.getEmail()
                ));

        if (!memberRepository.findAllByUserId(userToAdd.getId()).isEmpty()) {
            throw new ConflictException(
                    "User " + dto.getEmail()
                            + " is already a member of an organization"
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

    @CacheEvict(value = ORG_MEMBERS, key = "#orgId")
    @Transactional
    public void inviteMember(UUID orgId, InviteOrgMemberRequest request) {
        Organization org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization not found with id: " + orgId
                ));

        User currentUser = getCurrentUser();

        if (!isAdminOrSuperAdmin(currentUser)) {
            OrgMember membership = memberRepository
                    .findByOrganizationIdAndUserId(orgId, currentUser.getId())
                    .orElseThrow(() -> new ConflictException(
                            "You are not a member of this organization"
                    ));
            if (membership.getRole() != OrgMemberRole.OWNER) {
                throw new ConflictException(
                        "Only OWNER or ADMIN can invite members"
                );
            }
        }
        String email = request.email();
        // Deny if user already exists and is a member
        userRepository.findByEmail(email).ifPresent(existingUser -> {
            if (!memberRepository.findAllByUserId(existingUser.getId()).isEmpty()) {
                throw new ConflictException(
                        "User " + email
                                + " is already a member of an organization"
                );
            }
        });

        User invitedUser;
        if (userRepository.existsByEmail(email)) {
            invitedUser = userRepository.findByEmail(email).orElseThrow();
        } else {
            String inviteToken = UUID.randomUUID().toString();

            invitedUser = User.builder()
                    .email(email)
                    .name("")
                    .password("")
                    .roles(Set.of(Role.FIRM))
                    .emailVerified(true)
                    .enabled(false)
                    .accountStatus(AccountStatus.PENDING)
                    .inviteToken(inviteToken)
                    .build();
            invitedUser = userRepository.save(invitedUser);
            emailService.sendOrgMemberInvite(email, org.getName(), inviteToken);
        }

        // Create org member
        OrgMember member = OrgMember.builder()
                .organization(org)
                .user(invitedUser)
                .role(OrgMemberRole.MEMBER)
                .build();
        member = memberRepository.save(member);
    }

    // GET my organization
    @Transactional(readOnly = true)
    public List<OrganizationResponseDTO> getMyOrganizations() {
        Long currentUserId = getCurrentUser().getId();
        return memberRepository.findAllByUserId(currentUserId)
                .stream()
                .map(m -> toResponseDTOSlim(m.getOrganization()))
                .collect(Collectors.toList());
    }

    // REMOVE Member
    @CacheEvict(value = ORG_MEMBERS, key = "#orgId")
    public void removeMember(UUID orgId, UUID memberId) {
        if (!orgRepository.existsById(orgId)) {
            throw new ResourceNotFoundException("Organization not found with id: " + orgId);
        }

        User currentUser = getCurrentUser();
        boolean canEdit = isAdminOrSuperAdmin(currentUser);

        if (!canEdit) {
            OrgMember requestingMember = memberRepository
                    .findByOrganizationIdAndUserId(orgId, currentUser.getId())
                    .orElseThrow(() -> new ConflictException("You are not a member of this organization"));

            if (requestingMember.getRole() != OrgMemberRole.OWNER && !canEdit) {
                throw new ConflictException("Only the OWNER or ADMIN can remove members from this organization");
            }
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

    @CacheEvict(value = ORG_MEMBERS, key = "#orgId")
    @Transactional
    public OrgMemberDTO transferOwnership(UUID orgId, UUID newOwnerMemberId) {
        if (!orgRepository.existsById(orgId)) {
            throw new ResourceNotFoundException("Organization not found with id: " + orgId);
        }

        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.hasRole(Role.ADMIN);

        if (!isAdminOrSuperAdmin(currentUser)) {
            throw new ConflictException("Only ADMIN or SUPER_ADMIN can transfer organization ownership");
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

    @Cacheable(value = ORGANIZATIONS_PUBLIC, unless = "#result == null || #result.isEmpty()")
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
                ).collect(Collectors.toList());
    }

    @Caching(evict = {
            @CacheEvict(value = ORGANIZATION, key = "#id"),
            @CacheEvict(value = ORGANIZATIONS, allEntries = true),
            @CacheEvict(value = ORGANIZATIONS_PUBLIC, allEntries = true)
    })
    @Transactional
    public OrganizationResponseDTO changeStatus(UUID id, OrgStatus newStatus) {
        User currentUser = getCurrentUser();
        if (!isAdminOrSuperAdmin(currentUser)) {
            throw new ConflictException("Only ADMIN can change organization status");
        }

        Organization org = orgRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization not found with id: " + id
                ));
        org.setStatus(newStatus);
        return toResponseDTOSlim(orgRepository.save(org));
    }

    @Transactional
    public ProgramBRequirementsDTO uploadSpecification(
            Long programId, MultipartFile file, User currentUser
    ) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Program not found with id: " + programId));
        if (program.getType() != ProgramType.PROGRAM_B) {
            throw new ConflictException(
                    "Specifications only apply to Program B"
            );
        }

        boolean ownsProgram = memberRepository
                .findAllByUserId(currentUser.getId())
                .stream()
                .filter(m -> m.getRole() == OrgMemberRole.OWNER)
                .anyMatch(m -> program.getOrganization() != null
                && m.getOrganization().getId().equals(program.getOrganization().getId()));

        if (!ownsProgram && !currentUser.hasRole(Role.ADMIN)) {
            throw new ConflictException(
                    "You do not own this Program B"
            );
        }


        ProgramBRequirements req = requirementsRepository.
                findByProgramId(programId)
                .orElse(ProgramBRequirements.builder()
                        .program(program)
                        .build());
        deleteFileIfExists(req.getSpecificationPath());
        String path = saveFile(file, programId, "spec");

        req.setSpecificationName(file.getOriginalFilename());
        req.setSpecificationPath(path);

        return toProgramBRequirementsDTO(requirementsRepository.save(req));
    }

    @Transactional
    public ProgramBRequirementsDTO uploadBudget(
            Long programId, MultipartFile file, User currentUser
    ) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Program not found with id: " + programId
                ));
        if (program.getType() !=  ProgramType.PROGRAM_B) {
            throw new ConflictException("Budget only applies to Program B");
        }

        boolean ownsProgram = memberRepository
                .findAllByUserId(currentUser.getId())
                .stream()
                .filter(m -> m.getRole() == OrgMemberRole.OWNER)
                .anyMatch(m -> program.getOrganization() != null
                && m.getOrganization().getId()
                        .equals(program.getOrganization().getId()));

        if (!ownsProgram && !currentUser.hasRole(Role.ADMIN)) {
            throw new ConflictException("You do not own this Program B");
        }

        ProgramBRequirements req = requirementsRepository
                .findByProgramId(programId)
                .orElse(ProgramBRequirements.builder()
                        .program(program)
                        .build()
                );
        deleteFileIfExists(req.getBudgetPath());
        String path = saveFile(file, programId, "budget");


        req.setBudgetName(file.getOriginalFilename());
        req.setBudgetPath(path);

        return toProgramBRequirementsDTO(requirementsRepository.save(req));
    }

    @Transactional(readOnly = true)
    public Optional<ProgramBRequirementsDTO> getByProgram(Long programId) {
        return requirementsRepository.findByProgramId(programId)
                .map(this::toProgramBRequirementsDTO);
//                .orElseThrow(() -> new ResourceNotFoundException(
//                        "No requirements found for application: " + programId
//                ));
    }

    private String saveFile(MultipartFile file, Long applicationId, String type) {
        try {
            String filename = applicationId  + "_" + type + "_"
                    + System.currentTimeMillis() + "_"
                    + file.getOriginalFilename();
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath,
                    StandardCopyOption.REPLACE_EXISTING);
            return filePath.toString();

        } catch (IOException e) {
            throw AppException.serverError("Failed to store file: " + e.getMessage(), e);
        }
    }

    public ResponseEntity<Resource> serveSpecification(
            Long programId, boolean inline, User currentUser
    ) {
        ProgramBRequirements req = requirementsRepository
                .findByProgramId(programId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No requirements found for program: "  + programId
                ));
        checkFileAccess(req, currentUser);

        if (req.getSpecificationPath() == null) {
            throw new ResourceNotFoundException(
                    "No specification uploaded yet"
            );
        }

        Resource resource = fileServeService.load(req.getSpecificationPath());
        String contentType = fileServeService.detectContentType(req.getSpecificationName());
        String disposition = fileServeService.contentDisposition(
                inline, req.getSpecificationName()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    public ResponseEntity<Resource> serveBudget(
            Long programId, boolean inline, User currentUser
    ) {
        ProgramBRequirements req = requirementsRepository
                .findByProgramId(programId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No requirements found for program: " + programId
                ));
        checkFileAccess(req, currentUser);

        if (req.getBudgetPath() == null) {
            throw new ResourceNotFoundException("No budget uploaded yet");
        }

        Resource resource = fileServeService.load(req.getBudgetPath());
        String contentType = fileServeService.detectContentType(req.getBudgetName());
        String disposition = fileServeService.contentDisposition(
                inline, req.getBudgetName()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    // Role check
    private void checkFileAccess(ProgramBRequirements req, User currentUser) {
        if (currentUser.hasRole(Role.ADMIN)) return;
        if (currentUser.hasRole(Role.FIRM)
        ) {
            boolean ownsOrg = memberRepository
                    .findAllByUserId(currentUser.getId())
                    .stream()
                    .anyMatch(m -> req.getProgram().getOrganization() != null
                    && m.getOrganization().getId()
                            .equals(req.getProgram().getOrganization().getId()));
            if (ownsOrg) return;
        }

        if (currentUser.hasRole(Role.MENTOR)
        || currentUser.hasRole(Role.STUDENT)) return;

        throw new ConflictException("You do not have access to this file");
    }

    private void deleteFileIfExists(String filePath) {
        if (filePath != null && !filePath.isEmpty()) {
            try {
                Path path = Paths.get(filePath);
                Files.deleteIfExists(path);
            } catch (IOException e) {
                System.err.println("Could not delete file: " + filePath + " - " + e.getMessage());
            }
        }
    }

    private ProgramBRequirementsDTO toProgramBRequirementsDTO(ProgramBRequirements r) {
        return ProgramBRequirementsDTO.builder()
                .id(r.getId())
                .programId(r.getProgram().getId())
                .specificationName(r.getSpecificationName())
                .specificationPath(r.getSpecificationPath())
                .budgetName(r.getBudgetName())
                .budgetPath(r.getBudgetPath())
                .updatedAt(r.getUpdatedAt())
                .build();
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
                ).collect(Collectors.toCollection(ArrayList::new));
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
