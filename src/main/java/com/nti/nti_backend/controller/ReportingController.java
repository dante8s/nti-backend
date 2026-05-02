package com.nti.nti_backend.controller;

import com.nti.nti_backend.evaluation.CriteriaRepository;
import com.nti.nti_backend.evaluation.EvaluationRepository;
import com.nti.nti_backend.reporting.ReportingService;
import com.nti.nti_backend.studentProfile.StudentProfileRepository;
import com.nti.nti_backend.team.TeamRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/reporting")
public class ReportingController {

    private final ReportingService reportingService;
    private final CriteriaRepository criteriaRepository;
    private final EvaluationRepository evaluationRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeamRepository teamRepository;

    public ReportingController(ReportingService reportingService ,
                               CriteriaRepository criteriaRepository ,
                               EvaluationRepository evaluationRepository,
                               StudentProfileRepository studentProfileRepository,
                               TeamRepository teamRepository) {
        this.reportingService = reportingService;
        this.criteriaRepository =criteriaRepository;
        this.evaluationRepository = evaluationRepository;
        this.teamRepository =teamRepository;
        this.studentProfileRepository = studentProfileRepository;
    }

    // ── GET /api/reporting/export/{callId} ────────────────────────────────────
    // Vue "кнопка Excel" hits this endpoint.
    // Returns the XLSX file as a binary download — browser prompts "Save As".

    @GetMapping("/export/{callId}")
    @PreAuthorize("hasAnyRole('EVALUATOR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportEvaluatorReport(@PathVariable Long callId) {
        try {
            byte[] xlsx = reportingService.generateEvaluationReport(callId);

            // Build filename: evaluation_report_2026-04-01.xlsx
            String filename = "evaluation_report_"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    + ".xlsx";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION ,
                            "attachment; filename=\"" + filename + "\"")
                    .body(xlsx);

        }catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── GET /api/reporting/stats ──────────────────────────────────────────────
    // Vue "адмін дашборд" calls this on page load to populate the stat cards.
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String , Object>> getAdminStats() {
        Map<String , Object> stats = reportingService.getAdminStats();
        return ResponseEntity.ok(stats);
    }


    // ── GET /api/reporting/pr-check/{callId} ──────────────────────────────────
    // "Pull Request → review" from your task card.
    //
    // Before you submit a PR for review, this endpoint runs a checklist
    // that verifies your Week 3-6 work is actually complete and consistent.
    // It checks the same things a reviewer would look for manually:
    //
    //  ✅ At least one StudentProfile exists with a CV uploaded
    //  ✅ At least one Team exists with minimum 3 members
    //  ✅ At least one Call has Criteria configured with weights summing to 100
    //  ✅ At least one Evaluation has been submitted
    //  ✅ Weighted average score is calculable (not null)
    //  ✅ Excel export generates without error
    //
    // Returns a structured report: each check has a name, passed (true/false), and a message.
    // If all checks pass → status 200. If any fail → status 422 (Unprocessable Entity).

    @GetMapping("/pr-check/{callId}")
    @PreAuthorize("hasAnyRole('EVALUATOR','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String , Object>> prReadinessCheck(@PathVariable Long callId) {
        List<Map<String , Object>> checks = new ArrayList<>();
        boolean allPassed = true;

        /// ── Check 1: StudentProfiles with CV
        long  profilesWithCv = studentProfileRepository.countByCvFilePathIsNotNull();
        boolean check1 = profilesWithCv > 0;

        checks.add(buildCheck(
            "StudentProfile - CV upload" ,
                check1 ,
                check1 ? profilesWithCv + " profile(s) have CV uploaded"
                        : "No profiles have a CV - uploadCv() may not be working"
                ));
        if (!check1) allPassed = false;


        //── Check 2: Complete profiles
        long profileComplete = studentProfileRepository.countByProfileCompleteTrue();
        boolean check2 = profileComplete > 0;

        checks.add(buildCheck(
                "StudentProfile - profileComplete flag" ,
                check2 ,
                check2 ?  profileComplete + "profile(s) marked complete "
                        : "No profiles are marked completed - isProfileComplete() logic may be broken"
        ));
        if(!check2) allPassed = false;

        // ── Check 3: Teams with minimum size
        int eligibleTeams = teamRepository.findTeamsWithMinimumSize(3).size();
        boolean check3 = eligibleTeams > 0;

        checks.add(buildCheck(
                "Team - minimum 3 members (Program A rule)" ,
                check3 ,
                check3 ? eligibleTeams + "team(s) meet minimum size"
                        : "No teams have 3+ accepted minimum size"
        ));
        if(!check3) allPassed = false;

        // ── Check 4: Criteria configured for this call
        long criteriaCount  = criteriaRepository.countByCall_Id(callId);
        Integer weightSum = criteriaRepository.sumWeightPercentByCallId(callId);
        boolean check4 =  criteriaCount > 0 && weightSum != null && weightSum == 100;

        checks.add(buildCheck(
                "Criteria - rubric configured for call" + callId ,
                check4 ,
                check4 ? criteriaCount + " criteria , weights sum to 100%"
                        : "Criteria missing or weights don't sum to 100 (current sum: "
                        + (weightSum == null ? 0 : weightSum) + ")"

        ));
        if(!check4) allPassed = false;

        // ── Check 5: Evaluations submitted
        long evalCount = evaluationRepository.countEvaluationsForCall(callId);
        boolean check5 = evalCount > 0 ;

        checks.add(buildCheck(
                "Evaluation - scores submitted" ,
                check5 ,
                check5 ? evalCount + " evaluation(s) submitted"
                        : "No evaluations found - submitScore() may not be working"
        ));
        if(!check5 ) allPassed = false;

        // ── Check 6: Excel export generates without error
        boolean check6;
        String check6Message;
        try {
            byte [] xlsx = reportingService.generateEvaluationReport(callId);
            check6 = xlsx != null && xlsx.length > 0 ;
            int xlsxSize = xlsx == null ? 0 : xlsx.length;
            check6Message = check6
                    ? "XLSX generated successfully (" + xlsxSize + " bytes)"
                    : "XLSX generated but was empty";
        }catch (Exception e){
            check6 = false;
            check6Message = "XLSX generation failed: " + e.getMessage();
        }
        checks.add(buildCheck("Reporting - Excel export (Apache POI)" , check6 , check6Message));
        if (!check6) allPassed = false;

        Map<String , Object> response = new LinkedHashMap<>();
        response.put("CallId" , callId);
        response.put("checkedAt" , LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.put("allPassed" , allPassed);
        response.put("checks" , checks);

        return allPassed
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    // ── Internal helper
    private Map<String , Object> buildCheck (String name , boolean passed , String message) {
        Map<String , Object> check = new LinkedHashMap<>();
        check.put("name" , name);
        check.put("passed" , passed);
        check.put("message" , message);
        return check;
    }

}
