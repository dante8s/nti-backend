package com.nti.nti_backend.auth;

public record CompleteOrgMemberInviteRequest(
        String inviteToken,
        String name,
        String password
) {
}
