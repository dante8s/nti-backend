package com.nti.nti_backend.application;

import com.nti.nti_backend.audit.AuditService;
import com.nti.nti_backend.call.Call;
import com.nti.nti_backend.call.CallRepository;
import com.nti.nti_backend.email.EmailService;
import com.nti.nti_backend.program.ProgramType;
import com.nti.nti_backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository appRepository;
    private final CallRepository callRepository;
    private final DocumentRepository documentRepository;
    private final EmailService emailService;
    private final AuditService auditService;

    private static final Map<ApplicationStatus,
            List<ApplicationStatus>> ALLOWED = Map.of(
            ApplicationStatus.DRAFT,
            List.of(ApplicationStatus.SUBMITTED),
            ApplicationStatus.SUBMITTED,
            List.of(ApplicationStatus.IN_REVIEW),
            ApplicationStatus.IN_REVIEW,
            List.of(
                    ApplicationStatus.APPROVED,
                    ApplicationStatus.REJECTED,
                    ApplicationStatus.NEEDS_REVISION
            ),
            ApplicationStatus.NEEDS_REVISION,
            List.of(ApplicationStatus.SUBMITTED)
    );

    // Створити draft
    public ApplicationDTO createDraft(
            User applicant,
            CreateApplicationRequest request) {

        // Якщо заявка вже є — повертаємо її
        // замість помилки
        Optional<Application> existing =
                appRepository.findByApplicantIdAndCallId(
                        applicant.getId(), request.callId()
                );
        if (existing.isPresent()) {
            return toDTO(existing.get());
        }

        Call call = callRepository
                .findById(request.callId())
                .orElseThrow(() ->
                        new RuntimeException("Виклик не знайдено")
                );

        Application app = Application.builder()
                .call(call)
                .applicant(applicant)
                .status(ApplicationStatus.DRAFT)
                .build();

        Application saved = appRepository.save(app);

        auditService.log(
                applicant,
                "APPLICATION_CREATED",
                "APPLICATION",
                saved.getId(),
                "Створено чернетку заявки"
        );

        return toDTO(saved);
    }

    // Оновити дані форми (тільки DRAFT або NEEDS_REVISION)
    public ApplicationDTO updateDraft(
            Long appId,
            Long userId,
            UpdateApplicationRequest request) {

        Application app = findAndCheckOwner(appId, userId);

        if (app.getStatus() != ApplicationStatus.DRAFT
                && app.getStatus()
                != ApplicationStatus.NEEDS_REVISION) {
            throw new RuntimeException(
                    "Редагування доступне тільки для "
                            + "чернеток або заявок що потребують правок"
            );
        }

        app.setFormData(request.formData());
        Application saved = appRepository.save(app);

        auditService.log(
                app.getApplicant(),
                "APPLICATION_UPDATED",
                "APPLICATION",
                appId,
                "Оновлено дані заявки"
        );

        return toDTO(saved);
    }

    // Знайти мою заявку для виклику
    public Optional<ApplicationDTO> getMyByCall(
            Long userId, Long callId) {
        return appRepository
                .findByApplicantIdAndCallId(userId, callId)
                .map(this::toDTO);
    }

    // Відправити заявку
    public ApplicationDTO submit(
            Long appId, Long userId) {

        Application app = findAndCheckOwner(appId, userId);

        validateTransition(
                app.getStatus(), ApplicationStatus.SUBMITTED
        );

        // Перевірка документів
        ProgramType programType =
                app.getCall().getProgram().getType();
        List<RequiredDocument> required =
                DocumentRequirements.forProgram(programType);
        List<ApplicationDocument> uploaded =
                documentRepository.findByApplicationId(appId);

        Set<DocumentType> uploadedTypes = uploaded.stream()
                .map(ApplicationDocument::getDocumentType)
                .collect(Collectors.toSet());

        List<String> missing = required.stream()
                .filter(r -> !uploadedTypes.contains(r.type()))
                .map(RequiredDocument::label)
                .toList();

        if (!missing.isEmpty()) {
            throw new RuntimeException(
                    "Не вистачає документів: "
                            + String.join(", ", missing)
            );
        }

        app.setStatus(ApplicationStatus.SUBMITTED);
        Application saved = appRepository.save(app);

        auditService.log(
                app.getApplicant(),
                "STATUS_CHANGED",
                "APPLICATION",
                appId,
                "Заявку відправлено на розгляд"
        );

        emailService.sendApplicationStatusChanged(
                app.getApplicant().getEmail(),
                app.getApplicant().getName(),
                "SUBMITTED", null
        );

        return toDTO(saved);
    }

    // Завантажити документ
    public DocumentDTO saveDocument(
            Long appId, Long userId,
            String fileName, String filePath,
            String fileType, DocumentType documentType) {

        Application app = findAndCheckOwner(appId, userId);

        if (app.getStatus() != ApplicationStatus.DRAFT
                && app.getStatus()
                != ApplicationStatus.NEEDS_REVISION) {
            throw new RuntimeException(
                    "Не можна змінювати документи "
                            + "після відправки заявки"
            );
        }

        // Якщо документ такого типу вже є — замінюємо
        documentRepository
                .findByApplicationIdAndDocumentType(
                        appId, documentType
                )
                .ifPresent(documentRepository::delete);

        ApplicationDocument doc = ApplicationDocument
                .builder()
                .application(app)
                .fileName(fileName)
                .filePath(filePath)
                .fileType(fileType)
                .documentType(documentType)
                .build();

        ApplicationDocument saved =
                documentRepository.save(doc);

        auditService.log(
                app.getApplicant(),
                "DOCUMENT_UPLOADED",
                "APPLICATION",
                appId,
                "Завантажено: " + fileName
        );

        return toDocumentDTO(saved);
    }

    // Статус документів
    public List<DocumentStatusDTO> getDocumentStatus(
            Long appId) {
        Application app = appRepository.findById(appId)
                .orElseThrow(() ->
                        new RuntimeException("Заявку не знайдено")
                );

        ProgramType programType =
                app.getCall().getProgram().getType();
        List<RequiredDocument> required =
                DocumentRequirements.forProgram(programType);
        List<ApplicationDocument> uploaded =
                documentRepository.findByApplicationId(appId);

        Map<DocumentType, ApplicationDocument> uploadedMap =
                uploaded.stream().collect(
                        Collectors.toMap(
                                ApplicationDocument::getDocumentType,
                                d -> d
                        )
                );

        return required.stream()
                .map(req -> new DocumentStatusDTO(
                        req.type().name(),
                        req.label(),
                        req.description(),
                        uploadedMap.containsKey(req.type()),
                        uploadedMap.containsKey(req.type())
                                ? uploadedMap.get(req.type())
                                .getFileName()
                                : null,
                        uploadedMap.containsKey(req.type())
                                ? uploadedMap.get(req.type()).getId()
                                : null
                ))
                .toList();
    }

    public List<ApplicationDTO> getMyApplications(
            Long userId) {
        return appRepository
                .findByApplicantId(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public ApplicationDTO getById(Long id) {
        return toDTO(appRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Заявку не знайдено")
                )
        );
    }

    // Тільки не-чернетки для адміна
    public List<ApplicationDTO> getAll() {
        return appRepository.findAll()
                .stream()
                .filter(a ->
                        a.getStatus() != ApplicationStatus.DRAFT
                )
                .map(this::toDTO)
                .toList();
    }

    public ApplicationDTO changeStatus(
            Long appId,
            ApplicationStatus newStatus,
            String comment,
            User admin) {

        Application app = appRepository.findById(appId)
                .orElseThrow(() ->
                        new RuntimeException("Заявку не знайдено")
                );

        ApplicationStatus old = app.getStatus();
        validateTransition(old, newStatus);

        app.setStatus(newStatus);
        app.setAdminComment(comment);
        Application saved = appRepository.save(app);

        auditService.log(
                admin,
                "STATUS_CHANGED",
                "APPLICATION",
                appId,
                "Статус: " + old + " → " + newStatus
                        + (comment != null
                        ? ". Коментар: " + comment : "")
        );

        emailService.sendApplicationStatusChanged(
                app.getApplicant().getEmail(),
                app.getApplicant().getName(),
                newStatus.name(), comment
        );

        return toDTO(saved);
    }

    private void validateTransition(
            ApplicationStatus current,
            ApplicationStatus next) {
        List<ApplicationStatus> allowed =
                ALLOWED.getOrDefault(current, List.of());
        if (!allowed.contains(next)) {
            throw new RuntimeException(
                    "Недозволений перехід: "
                            + current + " → " + next
            );
        }
    }

    private Application findAndCheckOwner(
            Long appId, Long userId) {
        Application app = appRepository.findById(appId)
                .orElseThrow(() ->
                        new RuntimeException("Заявку не знайдено")
                );
        if (!app.getApplicant().getId().equals(userId)) {
            throw new RuntimeException("Немає доступу");
        }
        return app;
    }

    private ApplicationDTO toDTO(Application a) {
        return new ApplicationDTO(
                a.getId(),
                a.getCall().getId(),
                a.getCall().getTitle(),
                a.getCall().getProgram().getName(),
                a.getCall().getProgram().getType().name(),
                a.getStatus().name(),
                a.getAdminComment(),
                a.getFormData(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }

    private DocumentDTO toDocumentDTO(
            ApplicationDocument d) {
        String label = DocumentRequirements
                .forProgram(
                        d.getApplication().getCall()
                                .getProgram().getType()
                )
                .stream()
                .filter(r -> r.type() == d.getDocumentType())
                .findFirst()
                .map(RequiredDocument::label)
                .orElse(d.getDocumentType().name());

        return new DocumentDTO(
                d.getId(),
                d.getFileName(),
                d.getFileType(),
                d.getDocumentType().name(),
                label,
                d.getUploadedAt()
        );
    }
}
