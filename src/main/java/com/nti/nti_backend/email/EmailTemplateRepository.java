package com.nti.nti_backend.email;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {
    Optional<EmailTemplate> findByType(EmailTemplateType type);
    boolean existsByType(EmailTemplateType type);
}
