package com.nti.nti_backend.studentProfile;

import java.util.List;

/** Hint for the student whether they are ready to proceed to the application pages (team module without application changes). */
public record CallApplicationEligibility(
        boolean profileComplete,
        boolean teamLeader,
        boolean teamFull,
        boolean suggestsReadyForCallFlow,
        List<String> remindersUk
) {
}
