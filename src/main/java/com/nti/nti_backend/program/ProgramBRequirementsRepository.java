package com.nti.nti_backend.program;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProgramBRequirementsRepository
    extends JpaRepository<ProgramBRequirements, Long> {
    Optional<ProgramBRequirements> findByProgramId(Long programId);
}
