package com.nti.nti_backend.studentProfile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    Optional<StudentProfile> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);

    List<StudentProfile> findByCvFilePathIsNotNull();

    List<StudentProfile> findByProfileCompleteTrue();

    long countByCvFilePathIsNotNull();

    long countByProfileCompleteTrue();

    @Query("SELECT AVG(sp.profileAverageGrade) FROM StudentProfile sp WHERE sp.profileAverageGrade IS NOT NULL")
    Double findAverageProfileGrade();
}
