package com.nti.nti_backend.user;

public enum AccountStatus {
    PENDING,    // Чекає схвалення адміна
    APPROVED,   // Схвалений — може логінитись
    REJECTED,   // Відхилений
    SUSPENDED   // Заблокований
}