package com.nti.nti_backend.user;

public enum AccountStatus {
    PENDING,     // Awaiting admin approval
    APPROVED,    // Approved — can log in
    REJECTED,    // Rejected
    SUSPENDED,   // Suspended
    ANONYMIZED   // Personal data deleted at user request
}