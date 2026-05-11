package com.nti.nti_backend.application;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository
        extends JpaRepository<ApplicationDocument, Long> {

    List<ApplicationDocument> findByApplicationId(
            Long applicationId
    );

    Optional<ApplicationDocument>
    findByApplicationIdAndDocumentType(
            Long applicationId,
            DocumentType documentType
    );
}