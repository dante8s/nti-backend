package com.nti.nti_backend.application;

import com.nti.nti_backend.program.ProgramType;

import java.util.List;

public class DocumentRequirements {

    public static List<RequiredDocument> forProgram(
            ProgramType type) {
        return switch (type) {
            case PROGRAM_A -> List.of(
                    new RequiredDocument(
                            DocumentType.RESUME_A,
                            "Резюме",
                            "Короткий опис проблеми, рішення, "
                                    + "ринку та переваг"
                    ),
                    new RequiredDocument(
                            DocumentType.TECHNICAL_ARCH,
                            "Технічна архітектура",
                            "Опис рішення, технологій, "
                                    + "модулів і роботи"
                    ),
                    new RequiredDocument(
                            DocumentType.ROADMAP,
                            "Дорожня карта",
                            "Віхи, дорожня карта та хронологія"
                    ),
                    new RequiredDocument(
                            DocumentType.BUDGET,
                            "Бюджет",
                            "План розтягування грантів "
                                    + "та очікувані витрати"
                    ),
                    new RequiredDocument(
                            DocumentType.RISK_ANALYSIS,
                            "Аналіз ризиків",
                            "Виявлення ризиків, впливів "
                                    + "і заходів пом'якшення"
                    ),
                    new RequiredDocument(
                            DocumentType.MONETIZATION,
                            "Модель монетизації",
                            "Спосіб створити цінність "
                                    + "продукту та дохід"
                    )
            );
            case PROGRAM_B -> List.of(
                    new RequiredDocument(
                            DocumentType.RESUME_B,
                            "Резюме",
                            "Резюме команди та досвід"
                    ),
                    new RequiredDocument(
                            DocumentType.MOTIVATION,
                            "Мотивація",
                            "Чому ваша команда підходить "
                                    + "для цього проекту"
                    ),
                    new RequiredDocument(
                            DocumentType.SOLUTION_PROPOSAL,
                            "Пропозиція рішення",
                            "Як ви вирішите поставлене завдання"
                    ),
                    new RequiredDocument(
                            DocumentType.IMPLEMENTATION,
                            "Впровадження",
                            "План впровадження та технології"
                    )
            );
        };
    }
}