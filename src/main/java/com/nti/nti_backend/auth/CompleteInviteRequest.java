package com.nti.nti_backend.auth;

public record CompleteInviteRequest(
        String inviteToken,
        String name,
        String password
) {
}
