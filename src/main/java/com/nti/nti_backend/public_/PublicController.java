package com.nti.nti_backend.public_;

import com.nti.nti_backend.organization.entity.OrgStatus;
import com.nti.nti_backend.organization.repository.OrganizationRepository;
import com.nti.nti_backend.public_.dto.PublicMentorDTO;
import com.nti.nti_backend.public_.dto.PublicOrganizationDTO;
import com.nti.nti_backend.user.Role;
import com.nti.nti_backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    //GET /api/public/organizations
    @GetMapping("/organizations")
    public ResponseEntity<List<PublicOrganizationDTO>> getPublicOrganizations() {
        List<PublicOrganizationDTO> result = organizationRepository
                .findAllByStatus(OrgStatus.ACTIVE)
                .stream()
                .map(org -> PublicOrganizationDTO.builder()
                        .id(org.getId())
                        .name(org.getName())
                        .sector(org.getSector())
                        .website(org.getWebsite())
                        .description(org.getDescription())
                        .build()
                ).toList();

        return ResponseEntity.ok(result);
    }

    // GET /api/public/mentors
    @GetMapping("/mentors")
    public ResponseEntity<List<PublicMentorDTO>> getPublicMentors() {
        List<PublicMentorDTO> result = userRepository
                .findAllByRole(Role.MENTOR)
                .stream()
                .map(user -> PublicMentorDTO.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .build()
                ).toList();
        return ResponseEntity.ok(result);
    }
}
