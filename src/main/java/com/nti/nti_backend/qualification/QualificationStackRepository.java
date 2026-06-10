package com.nti.nti_backend.qualification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QualificationStackRepository extends JpaRepository<QualificationStack, Long> {
    List<QualificationStack> findAllByOrderByStackNumberAsc();
    Optional<QualificationStack> findBySpecializationKey(String specializationKey);
}
