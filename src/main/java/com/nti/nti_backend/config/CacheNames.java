package com.nti.nti_backend.config;

public final class CacheNames {

    private CacheNames() {}

    // Organization
    public static final String ORGANIZATIONS         = "organizations";
    public static final String ORGANIZATION          = "organization";
    public static final String ORGANIZATIONS_PUBLIC  = "organizations-public";
    public static final String ORG_MEMBERS           = "org-members";

    // Mentorship
    public static final String MENTORSHIPS_MY        = "mentorships-my";
    public static final String MENTORSHIP            = "mentorship";

    // Milestone
    public static final String MILESTONE             = "milestone";
    public static final String MILESTONES_PENDING    = "milestones-pending";

    // Program
    public static final String PROGRAMS_PUBLIC       = "programs-public";
    public static final String MENTORS_PUBLIC        = "mentors-public";
    // Application
    public static final String APPLICATIONS_MY      = "applications-my";
    public static final String APPLICATIONS_ALL     = "applications-all";
    public static final String APPLICATIONS_CALL    = "applications-by-call";

    // Call
    public static final String CALLS_OPEN           = "calls-open";
    public static final String CALL                 = "call";
    public static final String CALLS_BY_PROGRAM     = "calls-by-program";

    // Criteria & Evaluation
    public static final String CRITERIA             = "criteria";
    public static final String EVALUATIONS          = "evaluations";
    public static final String SCORE_WEIGHTED       = "score-weighted";
    public static final String SCORE_AVERAGE        = "score-average";

    // Student Profile
    public static final String STUDENT_PROFILE      = "student-profile";

    // Team
    public static final String TEAM                 = "team";
    public static final String TEAM_FOR_USER        = "team-for-user";
    public static final String TEAM_INVITES         = "team-invites";
    public static final String ELIGIBLE_TEAMS       = "eligible-teams";
}
