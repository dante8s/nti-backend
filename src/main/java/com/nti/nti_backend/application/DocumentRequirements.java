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
                            "Resume",
                            "Brief description of the problem, solution, "
                                    + "market, and benefits"
                    ),
                    new RequiredDocument(
                            DocumentType.TECHNICAL_ARCH,
                            "Technical Architecture",
                            "Description of the solution, technologies, "
                                    + "modules, and operations"
                    ),
                    new RequiredDocument(
                            DocumentType.ROADMAP,
                            "Roadmap",
                            "Milestones, roadmap, and timeline"
                    ),
                    new RequiredDocument(
                            DocumentType.BUDGET,
                            "Budget",
                            "Grant allocation plan "
                                    + "and expected expenses"
                    ),
                    new RequiredDocument(
                            DocumentType.RISK_ANALYSIS,
                            "Risk Analysis",
                            "Risk identification, impact assessment, "
                                    + "and mitigation measures"
                    ),
                    new RequiredDocument(
                            DocumentType.MONETIZATION,
                            "Monetization Model",
                            "How to create product value "
                                    + "and generate revenue"
                    )
            );
            case PROGRAM_B -> List.of(
                    new RequiredDocument(
                            DocumentType.RESUME_B,
                            "Resume",
                            "Team resume and experience"
                    ),
                    new RequiredDocument(
                            DocumentType.MOTIVATION,
                            "Motivation",
                            "Why your team is a good fit "
                                    + "for this project"
                    ),
                    new RequiredDocument(
                            DocumentType.SOLUTION_PROPOSAL,
                            "Solution Proposal",
                            "How you will solve the given task"
                    ),
                    new RequiredDocument(
                            DocumentType.IMPLEMENTATION,
                            "Implementation",
                            "Implementation plan and technologies"
                    )
            );
        };
    }
}