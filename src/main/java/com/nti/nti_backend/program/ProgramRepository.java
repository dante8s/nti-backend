package com.nti.nti_backend.program;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProgramRepository
        extends JpaRepository<Program, Long> {

    //List<Program> findByIsActiveTrue();

    List<Program> findByType(ProgramType type);

    List<Program> findByStatus(ProgramStatus status);

    List<Program> findByTypeAndStatus(ProgramType type,ProgramStatus status);

    List<Program> findByOrganizationId(UUID organizationId);
}