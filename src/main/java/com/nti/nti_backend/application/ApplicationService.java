package com.nti.nti_backend.application;

import com.nti.nti_backend.audit.AuditService;
import com.nti.nti_backend.call.Call;
import com.nti.nti_backend.call.CallRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final ApplicationMemberRepository applicationMemberRepository;

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
            List.of(ApplicationStatus.SUBMITTED),
            ApplicationStatus.APPROVED,
            List.of(ApplicationStatus.COMPLETION_REQUESTED),
            ApplicationStatus.COMPLETION_REQUESTED,
            List.of(ApplicationStatus.COMPLETED, ApplicationStatus.APPROVED)
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

        assertApplicantIsTeamLeader(applicant);

        // Команда не може мати більше одного активного проекту
        if (appRepository.existsApprovedByApplicantId(applicant.getId())) {
            throw new IllegalStateException(
                    "Команда вже має активний проект. Завершіть поточний проект перш ніж подавати нову заявку.");
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
    @Transactional
    public ApplicationDTO submit(
            Long appId, Long userId) {

        Application app = findAndCheckOwner(appId, userId);
        assertApplicantIsTeamLeader(app.getApplicant());
        assertTeamIsFullyAssembled(app.getApplicant());

        // Перевірка: не можна відправити нову заявку якщо команда вже має активний проект
        // (виключаємо саму цю заявку, щоб дати змогу повторно відправити після NEEDS_REVISION)
        boolean hasOtherActiveProject = appRepository
                .existsByApplicantIdAndStatusInAndIdNot(
                        app.getApplicant().getId(),
                        List.of(ApplicationStatus.APPROVED, ApplicationStatus.COMPLETION_REQUESTED),
                        app.getId()
                );
        if (hasOtherActiveProject) {
            throw new IllegalStateException(
                    "Команда вже має активний проект. Завершіть поточний проект перш ніж подавати нову заявку.");
        }

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

        // Зберігаємо снапшот складу команди на момент подачі
        List<com.nti.nti_backend.teamMember.TeamMember> members =
                teamMemberRepository.findAcceptedMembersByTeamLeader(app.getApplicant().getId());
        for (com.nti.nti_backend.teamMember.TeamMember m : members) {
            ApplicationMember snap = ApplicationMember.builder()
                    .application(saved)
                    .userId(m.getUser().getId())
                    .email(m.getUser().getEmail())
                    .role(m.getRole().name())
                    .build();
            applicationMemberRepository.save(snap);
        }

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

    @Transactional(readOnly = true)
    public List<ApplicationDTO> getMyApplications(Long userId) {
        // Якщо користувач — лідер, повертаємо його власні заявки
        if (teamRepository.findByLeader_Id(userId).isPresent()) {
            return appRepository.findByApplicantIdWithDetails(userId)
                    .stream().map(this::toDTO).toList();
        }

        // Якщо учасник — повертаємо заявки лідера своєї команди
        return teamRepository.findAcceptedTeamsByUserId(userId)
                .stream().findFirst()
                .map(team -> appRepository
                        .findByApplicantIdWithDetails(team.getLeader().getId())
                        .stream().map(this::toDTO).toList())
                .orElse(List.of());
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
        return viewer.hasRole(Role.STUDENT)
                && app.getApplicant().getId().equals(viewer.getId());
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

    // set OWNER of the product
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

    public List<ApplicationDTO> getByCall(Long callId) {
        return appRepository.findByCallId(callId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /** Завершити проект — лідер або адмін */
    /** Лідер надсилає запит на завершення проекту */
    public ApplicationDTO completeProject(Long appId, Long userId, boolean isAdmin) {
        Application app = appRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Заявку не знайдено"));

        if (!isAdmin) {
            boolean isLeader = teamRepository.findByLeader_Id(userId)
                    .map(team -> app.getApplicant().getId().equals(userId))
                    .orElse(false);
            if (!isLeader) {
                throw new RuntimeException("Тільки лідер команди може надіслати запит на завершення");
            }
            validateTransition(app.getStatus(), ApplicationStatus.COMPLETION_REQUESTED);
            app.setStatus(ApplicationStatus.COMPLETION_REQUESTED);
            Application saved = appRepository.save(app);
            auditService.log(app.getApplicant(), "STATUS_CHANGED", "APPLICATION", appId,
                    "Лідер надіслав запит на завершення проекту");
            return toDTO(saved);
        }

        // Адмін — одразу завершує
        validateTransition(app.getStatus(), ApplicationStatus.COMPLETED);
        app.setStatus(ApplicationStatus.COMPLETED);
        Application saved = appRepository.save(app);
        auditService.log(app.getApplicant(), "STATUS_CHANGED", "APPLICATION", appId, "Проект завершено адміном");
        return toDTO(saved);
    }

    /** Адмін підтверджує завершення */
    public ApplicationDTO approveCompletion(Long appId) {
        Application app = appRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Заявку не знайдено"));
        if (app.getStatus() != ApplicationStatus.COMPLETION_REQUESTED) {
            throw new RuntimeException("Заявка не перебуває у статусі запиту на завершення");
        }
        validateTransition(app.getStatus(), ApplicationStatus.COMPLETED);
        app.setStatus(ApplicationStatus.COMPLETED);
        Application saved = appRepository.save(app);
        auditService.log(app.getApplicant(), "STATUS_CHANGED", "APPLICATION", appId,
                "Адмін підтвердив завершення проекту");
        return toDTO(saved);
    }

    /** Адмін відхиляє запит на завершення — повертає APPROVED, повідомляє лідера */
    public ApplicationDTO rejectCompletion(Long appId) {
        Application app = appRepository.findById(appId)
                .orElseThrow(() -> new RuntimeException("Заявку не знайдено"));
        if (app.getStatus() != ApplicationStatus.COMPLETION_REQUESTED) {
            throw new RuntimeException("Заявка не перебуває у статусі запиту на завершення");
        }
        validateTransition(app.getStatus(), ApplicationStatus.APPROVED);
        app.setStatus(ApplicationStatus.APPROVED);
        Application saved = appRepository.save(app);
        auditService.log(app.getApplicant(), "STATUS_CHANGED", "APPLICATION", appId,
                "Адмін відхилив запит на завершення проекту");
        try {
            emailService.sendCompletionRejected(
                    app.getApplicant().getEmail(),
                    app.getApplicant().getName(),
                    app.getCall().getProgram().getName()
            );
        } catch (Exception ignored) {}
        return toDTO(saved);
    }

    /** Список заявок зі статусом COMPLETION_REQUESTED для адміна */
    @Transactional(readOnly = true)
    public List<ApplicationDTO> getCompletionRequests() {
        return appRepository.findByStatus(ApplicationStatus.COMPLETION_REQUESTED)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /** Проекти користувача: активний (APPROVED) та завершені (COMPLETED) */
    @Transactional(readOnly = true)
    public ProjectHistoryDTO getMyProjects(Long userId) {
        Set<Long> allIds = new java.util.HashSet<>();

        // Чи є userId зараз лідером якоїсь команди?
        boolean isCurrentLeader = teamRepository.findByLeader_Id(userId).isPresent();

        // ID поточного лідера команди (якщо userId є учасником)
        Long myLeaderId = null;
        if (!isCurrentLeader) {
            myLeaderId = teamRepository.findAcceptedTeamsByUserId(userId)
                    .stream().findFirst()
                    .map(team -> team.getLeader().getId())
                    .orElse(null);
        }

        if (isCurrentLeader) {
            // Лідер: бачить всі свої заявки (APPROVED + COMPLETED)
            appRepository.findByApplicantId(userId)
                    .stream().map(Application::getId)
                    .forEach(allIds::add);
        } else if (myLeaderId != null) {
            // Учасник: бачить тільки заявки свого ПОТОЧНОГО лідера де є у снапшоті
            Set<Long> snapshotIds = new java.util.HashSet<>(
                    applicationMemberRepository.findApplicationIdsByUserId(userId));
            appRepository.findByApplicantId(myLeaderId)
                    .stream().map(Application::getId)
                    .filter(snapshotIds::contains)
                    .forEach(allIds::add);
        }
        // Якщо користувач не лідер і не учасник жодної команди — показуємо порожньо

        List<Application> apps = appRepository.findAllById(allIds)
                .stream()
                .filter(a -> a.getStatus() == ApplicationStatus.APPROVED
                        || a.getStatus() == ApplicationStatus.COMPLETION_REQUESTED
                        || a.getStatus() == ApplicationStatus.COMPLETED)
                .toList();

        ProjectHistoryDTO.ProjectEntryDTO current = null;
        List<ProjectHistoryDTO.ProjectEntryDTO> history = new ArrayList<>();

        for (Application a : apps) {
            List<ApplicationDTO.MemberSnapshotDTO> members =
                    applicationMemberRepository.findByApplicationId(a.getId())
                            .stream()
                            .map(m -> new ApplicationDTO.MemberSnapshotDTO(
                                    m.getUserId(), m.getEmail(), m.getRole()))
                            .toList();

            String teamName = teamRepository.findByLeader_Id(a.getApplicant().getId())
                    .map(t -> t.getName()).orElse(null);

            ProjectHistoryDTO.ProjectEntryDTO entry = new ProjectHistoryDTO.ProjectEntryDTO(
                    a.getId(),
                    a.getStatus().name(),
                    a.getCall().getProgram().getName(),
                    a.getCall().getTitle(),
                    teamName,
                    a.getCreatedAt(),
                    a.getUpdatedAt(),
                    members
            );

            if (a.getStatus() == ApplicationStatus.APPROVED
                    || a.getStatus() == ApplicationStatus.COMPLETION_REQUESTED) {
                current = entry;
            } else {
                history.add(entry);
            }
        }

        return new ProjectHistoryDTO(current, history);
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

        List<ApplicationDTO.MemberSnapshotDTO> teamMembers =
                applicationMemberRepository.findByApplicationId(a.getId())
                        .stream()
                        .map(m -> new ApplicationDTO.MemberSnapshotDTO(m.getUserId(), m.getEmail(), m.getRole()))
                        .toList();

        return new ApplicationDTO(
                a.getId(),
                a.getApplicant().getId(),
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
                a.getFormData(),
                a.getCreatedAt(),
                a.getUpdatedAt(),
                teamMembers
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
