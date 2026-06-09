package com.nti.nti_backend.application;

import com.nti.nti_backend.audit.AuditService;
import com.nti.nti_backend.call.Call;
import com.nti.nti_backend.call.CallRepository;
import com.nti.nti_backend.organization.entity.Organization;
import com.nti.nti_backend.organization.repository.OrgMemberRepository;
import com.nti.nti_backend.team.TeamRepository;
import com.nti.nti_backend.teamMember.TeamMemberRepository;
import com.nti.nti_backend.email.EmailService;
import com.nti.nti_backend.program.ProgramType;
import com.nti.nti_backend.user.Role;
import com.nti.nti_backend.user.User;
import com.nti.nti_backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import static com.nti.nti_backend.config.CacheNames.*;
import org.springframework.cache.annotation.*;
import com.nti.nti_backend.exception.AppException;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    /** Віддача файлу документа заявки (перегляд / завантаження). */
    public record ServedApplicationDocument(
            Resource resource,
            MediaType contentType,
            String filename
    ) {}

    private final ApplicationRepository appRepository;
    private final CallRepository callRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final EmailService emailService;
    private final AuditService auditService;
    private final OrgMemberRepository memberRepository;
    private final ApplicationMemberRepository applicationMemberRepository;
    private final NotificationService notificationService;
    private final OrgMemberRepository memberRepository;


    // Від main: повніша state machine з усіма статусами
    private static final Map<ApplicationStatus, List<ApplicationStatus>> ALLOWED = Map.ofEntries(
            Map.entry(ApplicationStatus.DRAFT,
                    List.of(ApplicationStatus.SUBMITTED)),
            Map.entry(ApplicationStatus.SUBMITTED,
                    List.of(ApplicationStatus.FORMALLY_VERIFIED)),
            Map.entry(ApplicationStatus.FORMALLY_VERIFIED,
                    List.of(ApplicationStatus.IN_REVIEW)),
            Map.entry(ApplicationStatus.IN_REVIEW,
                    List.of(ApplicationStatus.APPROVED, ApplicationStatus.REJECTED,
                            ApplicationStatus.NEEDS_REVISION)),
            Map.entry(ApplicationStatus.NEEDS_REVISION,
                    List.of(ApplicationStatus.SUBMITTED)),
            Map.entry(ApplicationStatus.APPROVED,
                    List.of(ApplicationStatus.ONBOARDING, ApplicationStatus.COMPLETION_REQUESTED)),
            Map.entry(ApplicationStatus.ONBOARDING,
                    List.of(ApplicationStatus.ACTIVE)),
            Map.entry(ApplicationStatus.ACTIVE,
                    List.of(ApplicationStatus.SUSPENDED, ApplicationStatus.ARCHIVED)),
            Map.entry(ApplicationStatus.SUSPENDED,
                    List.of(ApplicationStatus.ACTIVE, ApplicationStatus.ARCHIVED)),
            Map.entry(ApplicationStatus.COMPLETION_REQUESTED,
                    List.of(ApplicationStatus.COMPLETED, ApplicationStatus.APPROVED))
    );

    // Створити draft
    @CacheEvict(value = APPLICATIONS_MY, key = "#applicant.id")
    public ApplicationDTO createDraft(User applicant, CreateApplicationRequest request) {

        // Якщо заявка вже є — повертаємо її
        // замість помилки
        Optional<Application> existing =
                appRepository.findByApplicantIdAndCallId(
                        applicant.getId(), request.callId()
                );
        if (existing.isPresent()) {
            return toDTO(existing.get());
        }

        assertApplicantIsTeamLeader(applicant);

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
    @CacheEvict(value = APPLICATIONS_MY, key = "#userId")
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
    @Caching(evict = {
            @CacheEvict(value = APPLICATIONS_MY, key = "#userId"),
            @CacheEvict(value = APPLICATIONS_ALL, allEntries = true)
    })
    @Transactional
    public ApplicationDTO submit(
            Long appId, Long userId) {

        Application app = findAndCheckOwner(appId, userId);
        assertApplicantIsTeamLeader(app.getApplicant());
        assertTeamIsFullyAssembled(app.getApplicant());

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
    public List<DocumentStatusDTO> getDocumentStatus(Long appId, User viewer) {
        Application app = appRepository.findById(appId)
                .orElseThrow(() ->
                        new RuntimeException("Заявку не знайдено")
                );
        if (viewer == null || !canViewApplication(viewer, app)) {
            throw new RuntimeException("Немає доступу");
        }

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

    /**
     * Файл обов'язкового документа для перегляду/завантаження (з диска).
     */
    public ServedApplicationDocument serveApplicationDocument(
            Long applicationId,
            DocumentType documentType,
            User viewer
    ) {
        Application app = appRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Заявку не знайдено"));
        if (viewer == null || !canViewApplication(viewer, app)) {
            throw new RuntimeException("Немає доступу");
        }
        ApplicationDocument doc = documentRepository
                .findByApplicationIdAndDocumentType(applicationId, documentType)
                .orElseThrow(() -> new RuntimeException("Документ не знайдено"));
        Path path = Paths.get(doc.getFilePath()).normalize();
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new RuntimeException("Файл на диску не знайдено");
        }
        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("Файл недоступний для читання");
            }
            MediaType ct = resolveDocumentMediaType(doc);
            String name = doc.getFileName() != null ? doc.getFileName()
                    : documentType.name().toLowerCase() + "." + extensionFor(doc.getFileType());
            return new ServedApplicationDocument(resource, ct, name);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Некоректний шлях до файлу", e);
        }
    }

    public static String contentDispositionAttachment(String filename) {
        String enc = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "attachment; filename*=UTF-8''" + enc;
    }

    public static String contentDispositionInline(String filename) {
        String enc = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "inline; filename*=UTF-8''" + enc;
    }

    private static MediaType resolveDocumentMediaType(ApplicationDocument doc) {
        if (doc.getFileType() != null && doc.getFileType().equalsIgnoreCase("DOCX")) {
            return MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        }
        return MediaType.APPLICATION_PDF;
    }

    private static String extensionFor(String fileType) {
        if (fileType != null && fileType.equalsIgnoreCase("DOCX")) {
            return "docx";
        }
        return "pdf";
    }

    @Cacheable(value = APPLICATIONS_MY, key = "#userId")
    @Transactional(readOnly = true)
    public List<ApplicationDTO> getMyApplications(Long userId) {
        if (teamRepository.findByLeader_Id(userId).isPresent()) {
            return appRepository.findByApplicantIdWithDetails(userId)
                    .stream().map(this::toDTO).collect(Collectors.toCollection(ArrayList::new));
        }

        return teamRepository.findAcceptedTeamsByUserId(userId)
                .stream().findFirst()
                .map(team -> appRepository
                        .findByApplicantIdWithDetails(team.getLeader().getId())
                        .stream().map(this::toDTO).collect(Collectors.toCollection(ArrayList::new)))
                .orElseGet(ArrayList::new);
    }

    // Одна заявка
    @Transactional
    public ApplicationDTO getById(Long id) {
        return toDTO(appRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Заявку не знайдено")
                )
        );
    }

    // Всі заявки (ADMIN)
    @Transactional
    /**
     * Перегляд заявки з перевіркою прав: студент — лише своя; комісія та адмін — будь-яка.
     */
    public ApplicationDTO getByIdForViewer(Long id, User viewer) {
        Application app = appRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заявку не знайдено"));
        if (viewer == null || !canViewApplication(viewer, app)) {
            throw new RuntimeException("Немає доступу");
        }
        return toDTO(app);
    }

    private boolean canViewApplication(User viewer, Application app) {
        if (viewer.hasRole(Role.ADMIN) || viewer.hasRole(Role.SUPER_ADMIN)) {
            return true;
        }
        if (viewer.hasRole(Role.EVALUATOR) || viewer.hasRole(Role.SUPER_EVALUATOR)) {
            return true;
        }
        if (viewer.hasRole(Role.MENTOR)) {
            return true;
        }
        if (viewer.hasRole(Role.FIRM)) {
            if (app.getCall() != null &&
                    app.getCall().getProgram() != null &&
                    app.getCall().getProgram().getOrganization() != null) {

                UUID orgId = app.getCall().getProgram().getOrganization().getId();

                boolean isMember = memberRepository.findByOrganizationIdAndUserId(orgId, viewer.getId()).isPresent();

                if (isMember) {
                    return true;
                }
            }
        }

        return viewer.hasRole(Role.STUDENT)
                && app.getApplicant().getId().equals(viewer.getId());
    }

    // Тільки не-чернетки для адміна
    @Cacheable(value = APPLICATIONS_ALL)
    public List<ApplicationDTO> getAll() {
        return appRepository.findAll()
                .stream()
                .filter(a ->
                        a.getStatus() != ApplicationStatus.DRAFT
                )
                .map(this::toDTO)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Caching(evict = {
            @CacheEvict(value = APPLICATIONS_ALL, allEntries = true),
            @CacheEvict(value = APPLICATIONS_CALL, allEntries = true)
    })
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

    // set OWNER of the product
    @Caching(evict = {
            @CacheEvict(value = APPLICATIONS_ALL, allEntries = true),
            @CacheEvict(value = APPLICATIONS_CALL, allEntries = true),
            @CacheEvict(value = APPLICATIONS_MY, allEntries = true)
    })
    @Transactional
    public ApplicationDTO setProductOwner(Long appId, Long userId, User currentUser) {
        Application app = appRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        if (app.getStatus() != ApplicationStatus.APPROVED) {
            throw new RuntimeException("Product owner can only be set on approved applications");
        }
        // only program b
        if (app.getCall().getProgram().getType() != ProgramType.PROGRAM_B) {
            throw new RuntimeException(" program owner can only be set on Program B applications");
        }

        User productOwner = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        app.setProductOwner(productOwner);
        return toDTO(appRepository.save(app));
    }

    @Cacheable(value = APPLICATIONS_CALL, key = "#callId")
    public List<ApplicationDTO> getByCall(Long callId) {
        return appRepository.findByCallId(callId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    // Валідація переходу між статусами
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
        boolean isProgramB = a.getCall().getProgram().getType()
                == ProgramType.PROGRAM_B;
        UUID organizationId = null;
        String organizationName = null;

        if (isProgramB && a.getCall().getProgram().getOrganization() != null) {
            organizationId = a.getCall().getProgram().getOrganization().getId();
            organizationName = a.getCall().getProgram().getOrganization().getName();
        }
        Long productOwnerId = null;
        String productOwnerName = null;
        if (a.getProductOwner() != null) {
            productOwnerId = a.getProductOwner().getId();
            productOwnerName = a.getProductOwner().getName();
        }

        return new ApplicationDTO(
                a.getId(),
                a.getCall().getId(),
                a.getCall().getTitle(),
                a.getCall().getProgram().getName(),
                a.getCall().getProgram().getType().name(),
                a.getStatus().name(),
                a.getAdminComment(),
                organizationId,
                organizationName,
                productOwnerId,
                productOwnerName,
                a.getApplicant() != null ? a.getApplicant().getId() : null,
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

    /** Заявку на виклик подає лідер команди (applicant_id = leader). */
    private void assertApplicantIsTeamLeader(User applicant) {
        if (applicant.hasRole(Role.SUPER_ADMIN)) {
            return;
        }
        if (!teamRepository.findByLeader_Id(applicant.getId()).isPresent()) {
            throw new RuntimeException(
                    "Подавати заявку на виклик може лише лідер команди");
        }
    }

    /** Команда повністю укомплектована (кількість ACCEPTED == maxCapacity). */
    private void assertTeamIsFullyAssembled(User applicant) {
        if (applicant.hasRole(Role.SUPER_ADMIN)) {
            return;
        }
        teamRepository.findByLeader_Id(applicant.getId()).ifPresent(team -> {
            long accepted = teamMemberRepository.countAcceptedMembers(team.getId());
            if (accepted < team.getMaxCapacity()) {
                throw new RuntimeException(
                        "Команда не укомплектована: потрібно " + team.getMaxCapacity()
                        + " учасник(ів), зараз " + accepted + ".");
            }
        });
    }
}
