package com.nti.nti_backend.program;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProgramRepository
        extends JpaRepository<Program, Long> {

    List<Program> findByIsActiveTrue();

    List<Program> findByType(ProgramType type);
}