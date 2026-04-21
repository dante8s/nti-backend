package com.nti.nti_backend.application;

import com.nti.nti_backend.call.Call;
import com.nti.nti_backend.call.CallRepository;
import com.nti.nti_backend.email.EmailService;
import com.nti.nti_backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository appRepository;
    private final CallRepository callRepository;
    private final EmailService emailService;

    // Дозволені переходи між статусами
    private static final Map<ApplicationStatus,
            List<ApplicationStatus>> ALLOWED =
            Map.of(
                    ApplicationStatus.DRAFT,
                    List.of(ApplicationStatus.SUBMITTED),
                    ApplicationStatus.SUBMITTED,
                    List.of(ApplicationStatus.IN_REVIEW),
                    ApplicationStatus.IN_REVIEW,
                    List.of(ApplicationStatus.APPROVED,
                            ApplicationStatus.REJECTED,
                            ApplicationStatus.NEEDS_REVISION),
                    ApplicationStatus.NEEDS_REVISION,
                    List.of(ApplicationStatus.SUBMITTED)
            );

    // Створити draft заявку
    public ApplicationDTO createDraft(
            User applicant,
            CreateApplicationRequest request) {

        // Перевірка чи не подав вже заявку на цей виклик
        if (appRepository.existsByApplicantIdAndCallId(
                applicant.getId(), request.callId())) {
            throw new RuntimeException(
                    "Ви вже подали заявку на цей виклик"
            );
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

        return toDTO(appRepository.save(app));
    }

    // Відправити заявку
    public ApplicationDTO submit(
            Long appId, Long userId) {
        Application app = findAndCheckOwner(appId, userId);

        validateTransition(
                app.getStatus(),
                ApplicationStatus.SUBMITTED
        );

        app.setStatus(ApplicationStatus.SUBMITTED);
        Application saved = appRepository.save(app);

        emailService.sendApplicationStatusChanged(
                app.getApplicant().getEmail(),
                app.getApplicant().getName(),
                "SUBMITTED",
                null
        );

        return toDTO(saved);
    }

    // Мої заявки
    public List<ApplicationDTO> getMyApplications(
            Long userId) {
        return appRepository
                .findByApplicantId(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // Одна заявка
    public ApplicationDTO getById(Long id) {
        return toDTO(appRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Заявку не знайдено")
                )
        );
    }

    // Всі заявки (ADMIN)
    public List<ApplicationDTO> getAll() {
        return appRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // Змінити статус (ADMIN)
    public ApplicationDTO changeStatus(
            Long appId,
            ApplicationStatus newStatus,
            String comment) {

        Application app = appRepository.findById(appId)
                .orElseThrow(() ->
                        new RuntimeException("Заявку не знайдено")
                );

        validateTransition(app.getStatus(), newStatus);

        app.setStatus(newStatus);
        app.setAdminComment(comment);
        Application saved = appRepository.save(app);

        emailService.sendApplicationStatusChanged(
                app.getApplicant().getEmail(),
                app.getApplicant().getName(),
                newStatus.name(),
                comment
        );

        return toDTO(saved);
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

    // Перевірити що заявка належить юзеру
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
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}