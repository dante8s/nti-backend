package com.nti.nti_backend.program;

import com.nti.nti_backend.organization.entity.OrgMemberRole;
import com.nti.nti_backend.organization.entity.Organization;
import com.nti.nti_backend.organization.repository.OrgMemberRepository;
import com.nti.nti_backend.organization.repository.OrganizationRepository;
import com.nti.nti_backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgramService {

    private final ProgramRepository programRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final OrganizationRepository organizationRepository;

    // Public - approved programs by type
    public List<ProgramDTO> getByType(ProgramType type) {
        return programRepository.findByTypeAndStatus(type, ProgramStatus.APPROVED)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public ProgramDTO getByIdAndType(Long id, ProgramType type) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Program Not Found"));
        if (program.getType() != type) {
            throw new RuntimeException("Program Not Found");
        }
        return toDTO(program);
    }

    // Create Program A
    @Transactional
    public ProgramDTO create(ProgramDTO dto) {
        Program program = Program.builder()
                .name(dto.name())
                .description(dto.description())
                .type(ProgramType.valueOf(dto.type()))
                .status(ProgramStatus.APPROVED)
                .build();
        return toDTO(programRepository.save(program));
    }

    // FIRM submits Program B - start as DRAFT
    @Transactional
    public ProgramDTO submitProgramB(SubmitProgramBRequest dto, User currentUser) {
        UUID orgId = orgMemberRepository.findAllByUserId(currentUser.getId())
                .stream()
                .filter(m -> m.getRole() == OrgMemberRole.OWNER)
                .map(m -> m.getOrganization().getId())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Organization Not Found"));

        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization Not Found"));

        Program program = Program.builder()
                .name(dto.name())
                .description(dto.description())
                .type(ProgramType.PROGRAM_B)
                .organization(org)
                .status(ProgramStatus.DRAFT)
                .build();
        return toDTO(programRepository.save(program));
    }

    // FIRM submits draft for review
    @Transactional
    public ProgramDTO submitForReview(Long id, User currentUser) {
        Program program = getProgramOwnedByUser(id, currentUser);

        if (program.getStatus() != ProgramStatus.DRAFT
        && program.getStatus() != ProgramStatus.NEEDS_REVISION) {
            throw new RuntimeException("Only DRAFT or NEEDS_REVISION programs can be submitted for review");
        }

        program.setStatus(ProgramStatus.PENDING_REVIEW);
        program.setAdminComment(null);
        return toDTO(programRepository.save(program));
    }

    // FIRM edits their Program B when DRAFT or NEEDS_REVISION
    @Transactional
    public ProgramDTO updateProgramB(Long id, SubmitProgramBRequest dto, User currentUser) {
        Program program = getProgramOwnedByUser(id, currentUser);

        if (program.getStatus() != ProgramStatus.DRAFT
        && program.getStatus() != ProgramStatus.NEEDS_REVISION) {
            throw new RuntimeException(
                    "Cannot edit a program with status: " + program.getStatus()
            );
        }

        program.setName(dto.name());
        program.setDescription(dto.description());
        return toDTO(programRepository.save(program));
    }

    // FIRM sees their programs B
    public List<ProgramDTO> getMyPrograms(User currentUser) {
        return orgMemberRepository.findAllByUserId(currentUser.getId())
                .stream()
                .filter(m -> m.getRole() == OrgMemberRole.OWNER)
                .flatMap(m -> programRepository
                        .findByOrganizationId(m.getOrganization().getId())
                        .stream()
                )
                .map(this::toDTO)
                .toList();
    }

    // Admin sees all pending review programs
    public List<ProgramDTO> getPendingReview() {
        return programRepository.findByStatus(ProgramStatus.PENDING_REVIEW)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // Admin reviews
    @Transactional
    public ProgramDTO review(Long id, ReviewProgramRequest dto) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Program Not Found"));

        if (program.getStatus() != ProgramStatus.PENDING_REVIEW) {
            throw new RuntimeException("Cannot review a program with status: " + program.getStatus());
        }

        ProgramStatus newStatus = ProgramStatus.valueOf(dto.status());

        if (newStatus != ProgramStatus.APPROVED
                && newStatus != ProgramStatus.NEEDS_REVISION
                && newStatus != ProgramStatus.REJECTED) {
            throw new RuntimeException("Invalid review status: " + newStatus);
        }

        program.setStatus(newStatus);
        program.setAdminComment(dto.adminComment());
        return toDTO(programRepository.save(program));
    }

    // Admin updates Program A details
    @Transactional
    public ProgramDTO update(Long id, ProgramDTO dto) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Program Not Found"));
        program.setName(dto.name());
        program.setDescription(dto.description());
        return toDTO(programRepository.save(program));
    }

    // Admin deactivates - sets to REJECTED
    @Transactional
    public void deactivate(Long id) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Program Not Found"));
        program.setStatus(ProgramStatus.REJECTED);
        programRepository.save(program);
    }

    private Program getProgramOwnedByUser(Long programId, User currentUser) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new RuntimeException("Program Not Found"));

        boolean ownsOrg = orgMemberRepository.findAllByUserId(currentUser.getId())
                .stream()
                .filter(m -> m.getRole() == OrgMemberRole.OWNER)
                .anyMatch(m -> m.getOrganization().getId()
                        .equals(program.getOrganization().getId()));
        if (!ownsOrg) {
            throw new RuntimeException("You do not own this program");
        }

        return program;
    }

    private ProgramDTO toDTO(Program p) {
        return new ProgramDTO(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getType().name(),
                p.getStatus().name(),
                p.getAdminComment(),
                p.getOrganization() != null ? p.getOrganization().getId() : null,
                p.getOrganization() != null ? p.getOrganization().getName() : null,
                p.getUpdatedAt()
        );
    }

//    // Всі активні програми — публічний доступ
//    public List<ProgramDTO> getAllActive() {
//        return programRepository.findByIsActiveTrue()
//                .stream()
//                .map(this::toDTO)
//                .toList();
//    }
//
//    // Одна програма по id
//    public ProgramDTO getById(Long id) {
//        return programRepository.findById(id)
//                .map(this::toDTO)
//                .orElseThrow(() ->
//                        new RuntimeException("Програму не знайдено")
//                );
//    }
//
//    // Створити нову програму (тільки ADMIN)
//    public ProgramDTO create(ProgramDTO dto) {
//        Program program = Program.builder()
//                .name(dto.name())
//                .description(dto.description())
//                .type(ProgramType.valueOf(dto.type()))
//                .build();
//        return toDTO(programRepository.save(program));
//    }
//
//    // Оновити програму (тільки ADMIN)
//    public ProgramDTO update(Long id, ProgramDTO dto) {
//        Program program = programRepository.findById(id)
//                .orElseThrow(() ->
//                        new RuntimeException("Програму не знайдено")
//                );
//        program.setName(dto.name());
//        program.setDescription(dto.description());
//        program.setActive(dto.isActive());
//        return toDTO(programRepository.save(program));
//    }
//
//    // Деактивувати програму
//    public void deactivate(Long id) {
//        Program program = programRepository.findById(id)
//                .orElseThrow(() ->
//                        new RuntimeException("Програму не знайдено")
//                );
//        program.setActive(false);
//        programRepository.save(program);
//    }
//
//    private ProgramDTO toDTO(Program p) {
//        return new ProgramDTO(
//                p.getId(),
//                p.getName(),
//                p.getDescription(),
//                p.getType().name(),
//                p.isActive()
//        );
//    }
//    public List<ProgramDTO> getByType(ProgramType type) {
//        return programRepository.findByType(type)
//                .stream()
//                .filter(Program::isActive)
//                .map(this::toDTO)
//                .toList();
//    }
//
//    public ProgramDTO getByIdAndType(Long id, ProgramType type) {
//        Program program = programRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Програму не знайдено"));
//
//        if (program.getType() != type) {
//            throw new RuntimeException("Програму не знайдено");
//        }
//
//        return toDTO(program);
//    }
}