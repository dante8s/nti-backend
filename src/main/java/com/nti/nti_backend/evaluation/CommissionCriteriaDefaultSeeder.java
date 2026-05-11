package com.nti.nti_backend.evaluation;

import com.nti.nti_backend.call.Call;
import com.nti.nti_backend.call.CallRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Якщо для виклику ще немає критеріїв у БД, створює стандартний набір для оцінювання комісії.
 * Узгоджено з шаблоном екрану (бали 1–100 та коментар).
 */
@Service
public class CommissionCriteriaDefaultSeeder {

    private record DefaultCriterion(String name, String description, int weightPercent, int sortOrder) {}

    private static final List<DefaultCriterion> DEFAULT_SET = List.of(
            new DefaultCriterion(
                    "Оригінальність ідеї",
                    "Наскільки проєкт відрізняється від уже відомих рішень, свіжість "
                            + "формулювання проблеми та підходів.",
                    25,
                    1),
            new DefaultCriterion(
                    "Технічна реалізованість",
                    "Реалістичність технічного плану, архітектури та дорожньої карти виконання.",
                    25,
                    2),
            new DefaultCriterion(
                    "Соціальний та ринковий вплив",
                    "Користь для аудиторії, соціально-екологічний або економічний ефект, "
                            + "потенціал масштабування.",
                    25,
                    3),
            new DefaultCriterion(
                    "Якість і повнота матеріалів",
                    "Структурованість поданої документації: бюджет, аналіз ризиків, "
                            + "монетизація та відповідність вимогам виклику.",
                    25,
                    4)
    );

    private final CriteriaRepository criteriaRepository;
    private final CallRepository callRepository;

    /** Захист від подвійного створення під паралельними першими запитами. */
    private final ConcurrentHashMap<Long, Object> localLocks = new ConcurrentHashMap<>();

    public CommissionCriteriaDefaultSeeder(
            CriteriaRepository criteriaRepository,
            CallRepository callRepository) {
        this.criteriaRepository = criteriaRepository;
        this.callRepository = callRepository;
    }

    @Transactional
    public List<Criteria> listForCallEnsuringDefaults(Long callId) {
        if (callId == null) {
            return List.of();
        }
        List<Criteria> first = criteriaRepository.findByCall_IdOrderBySortOrderAsc(callId);
        if (!first.isEmpty()) {
            return first;
        }

        Long key = Objects.requireNonNull(callId);
        synchronized (localLocks.computeIfAbsent(key, k -> new Object())) {
            List<Criteria> again = criteriaRepository.findByCall_IdOrderBySortOrderAsc(callId);
            if (!again.isEmpty()) {
                return again;
            }

            Call call = callRepository.findById(callId).orElse(null);
            if (call == null) {
                return List.of();
            }

            for (DefaultCriterion d : DEFAULT_SET) {
                Criteria c = new Criteria();
                c.setCall(call);
                c.setName(d.name());
                c.setDescription(d.description());
                c.setWeightPercent(d.weightPercent());
                c.setSortOrder(d.sortOrder());
                c.setMaxScore(100.0);
                criteriaRepository.save(c);
            }
            return criteriaRepository.findByCall_IdOrderBySortOrderAsc(callId);
        }
    }
}
