package com.nti.nti_backend.controller;

import com.nti.nti_backend.application.ApplicationMemberRepository;
import com.nti.nti_backend.application.ApplicationRepository;
import com.nti.nti_backend.evaluation.CommissionCriteriaDefaultSeeder;
import com.nti.nti_backend.evaluation.Criteria;
import com.nti.nti_backend.evaluation.CriteriaRepository;
import com.nti.nti_backend.evaluation.Evaluation;
import com.nti.nti_backend.evaluation.EvaluationService;
import com.nti.nti_backend.team.TeamRepository;
import com.nti.nti_backend.user.Role;
import com.nti.nti_backend.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/evaluations")
public class ScoringController {

    private final EvaluationService evaluationService;
    private final CriteriaRepository criteriaRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationMemberRepository applicationMemberRepository;
    private final TeamRepository teamRepository;
    private final CommissionCriteriaDefaultSeeder commissionCriteriaDefaultSeeder;

    public ScoringController(EvaluationService evaluationService,
                             CriteriaRepository criteriaRepository,
                             ApplicationRepository applicationRepository,
                             ApplicationMemberRepository applicationMemberRepository,
                             TeamRepository teamRepository,
                             CommissionCriteriaDefaultSeeder commissionCriteriaDefaultSeeder) {
        this.evaluationService = evaluationService;
        this.criteriaRepository = criteriaRepository;
        this.applicationRepository = applicationRepository;
        this.applicationMemberRepository = applicationMemberRepository;
        this.teamRepository = teamRepository;
        this.commissionCriteriaDefaultSeeder = commissionCriteriaDefaultSeeder;
    }

    // POST /api/evaluations/score  — submit or update one score (лише SUPER_EVALUATOR / адміни)
    @PostMapping("/score")
    @PreAuthorize("hasAnyRole('SUPER_EVALUATOR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Evaluation> submitScore(
            @AuthenticationPrincipal User authUser,
            @RequestBody Evaluation evaluation ) {
        Long evaluatorId = evaluation.getEvaluator() != null ? evaluation.getEvaluator().getId() : null;
        if (!canActAsEvaluator(authUser, evaluatorId)) {
            return ResponseEntity.status(403).build();
        }
        try {
            Evaluation saved = evaluationService.submitScore(evaluation);
            return ResponseEntity.ok(saved);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // GET  /api/evaluations/{appId}/scores — get all scores for one application
    @GetMapping("/{appId}/scores")
    @PreAuthorize("hasAnyRole('EVALUATOR','SUPER_EVALUATOR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<Evaluation>> getScores(@PathVariable Long appId) {
        List<Evaluation> scores = evaluationService.getAllScoresForApplication(appId);
        return ResponseEntity.ok(scores);
    }

    // ── Get pre-filled scores for one evaluator ───────────────────────────────
    // Called when a commission member reopens the evaluation form — pre-fills their previous scores.
    @GetMapping("/{appId}/mine")
    @PreAuthorize("hasAnyRole('SUPER_EVALUATOR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<Evaluation>> getMyScores (
        @AuthenticationPrincipal User authUser,
        @PathVariable Long appId ,
        @RequestParam Long evaluatorId
    ){
        if (!canActAsEvaluator(authUser, evaluatorId)) {
            return ResponseEntity.status(403).build();
        }
        List<Evaluation> myScores = evaluationService.getEvaluatorScoresForApplication(appId , evaluatorId);
        return ResponseEntity.ok(myScores);
    }

    // ── Weighted average — "середній бал" ────────────────────────────────────
    // The key scoring endpoint — returns the weighted average score for one application.
    // Formula: SUM(score × weightPercent) / SUM(weightPercent)
    @GetMapping("/{appId}/average")
    @PreAuthorize("hasAnyRole('EVALUATOR','SUPER_EVALUATOR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String , Object>> getAverage(@PathVariable Long appId) {
        Double weightedAvg = evaluationService.getWeightedAverageScore(appId);
        Double simpleAvg = evaluationService.getAverageScore(appId);

        Map<String , Object> result = new LinkedHashMap<>();
        result.put("application" , appId);
        result.put("weightedAverage" , weightedAvg != null ? Math.round(weightedAvg * 100.0) / 100.0 : null);
        result.put("simpleAverage" , simpleAvg != null ? Math.round(simpleAvg * 100.0) / 100.0 : null);

        return ResponseEntity.ok(result);
    }

    // ── Check if evaluation is complete ──────────────────────────────────────
    // Returns true when the evaluator has scored every criterion for this application.
    // Vue uses this to show the green "complete" badge and enable the Submit button.
    @GetMapping("/{appId}/complete")
    @PreAuthorize("hasAnyRole('SUPER_EVALUATOR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String , Object>> isComplete(
            @AuthenticationPrincipal User authUser,
            @PathVariable Long appId ,
            @RequestParam Long evaluatorId ,
            @RequestParam Long callId
    ) {
        if (!canActAsEvaluator(authUser, evaluatorId)) {
            return ResponseEntity.status(403).build();
        }
        boolean complete;
        long totalCriteria;
        try {
            complete = evaluationService.isEvaluationComplete(appId , evaluatorId , callId);
            totalCriteria = criteriaRepository.countByCall_Id(callId);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }

        Map<String  , Object> result = new LinkedHashMap<>();
        result.put("applicationId" , appId);
        result.put("evaluatorId" , evaluatorId);
        result.put("complete" , complete);
        result.put("totalCriteria" , totalCriteria);

        return ResponseEntity.ok(result);
    }

    // ── Get criteria list for a call ─────────────────────────────────────────
    // Vue evaluation form loads this first to render one input per criterion.
    @GetMapping("/criteria/{callId}")
    @PreAuthorize("hasAnyRole('EVALUATOR','SUPER_EVALUATOR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<Criteria>> getCriteriaList(@PathVariable Long callId) {
        List<Criteria> criteria =
                commissionCriteriaDefaultSeeder.listForCallEnsuringDefaults(callId);
        return ResponseEntity.ok(criteria);
    }

    // ── Commission queue for a call ───────────────────────────────────────────
    @GetMapping("/calls/{callId}/applications")
    @PreAuthorize("hasAnyRole('EVALUATOR','SUPER_EVALUATOR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getCallApplications(@PathVariable Long callId) {
        List<Map<String, Object>> queue = applicationRepository.findByCallIdWithApplicant(callId)
                .stream()
                .map(app -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", app.getId());
                    row.put("status", app.getStatus() != null ? app.getStatus().name() : null);
                    Long applicantId = app.getApplicant() != null ? app.getApplicant().getId() : null;
                    row.put("applicantId", applicantId);
                    row.put("applicantEmail", app.getApplicant() != null ? app.getApplicant().getEmail() : null);
                    row.put("applicantName", app.getApplicant() != null ? app.getApplicant().getName() : null);
                    String programName = null;
                    if (app.getCall() != null && app.getCall().getProgram() != null) {
                        programName = app.getCall().getProgram().getName();
                    }
                    row.put("programName", programName);
                    // Назва команди
                    String teamName = null;
                    if (applicantId != null) {
                        teamName = teamRepository.findByLeader_Id(applicantId)
                                .map(com.nti.nti_backend.team.Team::getName)
                                .orElse(null);
                    }
                    row.put("teamName", teamName);
                    // Снапшот учасників на момент подачі
                    List<Map<String, Object>> members = applicationMemberRepository
                            .findByApplicationId(app.getId())
                            .stream()
                            .map(m -> {
                                Map<String, Object> member = new LinkedHashMap<>();
                                member.put("userId", m.getUserId());
                                member.put("email", m.getEmail());
                                member.put("role", m.getRole());
                                return member;
                            })
                            .toList();
                    row.put("teamMembers", members);
                    return row;
                })
                .toList();
        return ResponseEntity.ok(queue);
    }

    private boolean canActAsEvaluator(User authUser, Long evaluatorId) {
        return authUser != null && evaluatorId != null && (
                evaluatorId.equals(authUser.getId())
                        || authUser.hasRole(Role.ADMIN)
                        || authUser.hasRole(Role.SUPER_ADMIN)
        );
    }
}
