package com.nti.nti_backend.organization.repository;


import com.nti.nti_backend.organization.entity.OrgStatus;
import com.nti.nti_backend.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    boolean existsByIco(String ico);

    List<Organization> findAllByStatus(OrgStatus status);

    @Query("SELECT o FROM Organization o LEFT JOIN FETCH o.members m LEFT JOIN FETCH m.user WHERE o.id = :id")
    Optional<Organization> findByIdWithMembers(UUID id);
}
