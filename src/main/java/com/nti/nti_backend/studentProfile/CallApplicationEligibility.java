package com.nti.nti_backend.studentProfile;

import java.util.List;

/** Підказка для студента чи він готовий іти на сторінки заявки (командний модуль без змін застосування). */
public record CallApplicationEligibility(
        boolean profileComplete,
        boolean teamLeader,
        boolean suggestsReadyForCallFlow,
        List<String> remindersUk
) {
}
