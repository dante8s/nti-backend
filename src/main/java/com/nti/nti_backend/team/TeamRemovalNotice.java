package com.nti.nti_backend.team;

import java.time.LocalDateTime;

/** Notification for the user who was removed from the team. */
public record TeamRemovalNotice(
        Long teamId,
        String teamName,
        LocalDateTime removedAt
) {
}
