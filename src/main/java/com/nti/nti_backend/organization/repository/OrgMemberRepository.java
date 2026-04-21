package com.nti.nti_backend.organization.repository;

import com.nti.nti_backend.organization.entity.OrgMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrgMemberRepository extends JpaRepository<OrgMember, UUID> {

    Optional<OrgMember> findByOrganizationIdAndUserId(UUID organizationId, Long userId);

    List<OrgMember> findAllByOrganizationId(UUID organizationId);

    boolean existsByOrganizationIdAndUserId(UUID organizationId, Long userId);

    // get my organization
    List<OrgMember> findAllByUserId(Long userId);
}
