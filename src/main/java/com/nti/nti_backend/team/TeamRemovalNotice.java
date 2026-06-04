package com.nti.nti_backend.team;

import java.time.LocalDateTime;

/** Повідомлення для користувача, якого виключили з команди. */
public record TeamRemovalNotice(
        Long teamId,
        String teamName,
        LocalDateTime removedAt
) {
}
