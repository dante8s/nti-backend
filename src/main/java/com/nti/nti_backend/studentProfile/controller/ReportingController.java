package com.nti.nti_backend.studentProfile.controller;

import com.nti.nti_backend.audit.AuditService;
import com.nti.nti_backend.evaluation.CriteriaRepository;
import com.nti.nti_backend.evaluation.EvaluationRepository;
import com.nti.nti_backend.reporting.ReportingService;
import com.nti.nti_backend.studentProfile.StudentProfileRepository;
import com.nti.nti_backend.team.TeamRepository;
import com.nti.nti_backend.user.User;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
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
    private final AuditService auditService;

    public ReportingController(ReportingService reportingService,
                               CriteriaRepository criteriaRepository,
                               EvaluationRepository evaluationRepository,
                               StudentProfileRepository studentProfileRepository,
                               TeamRepository teamRepository,
                               AuditService auditService) {
        this.reportingService = reportingService;
        this.criteriaRepository = criteriaRepository;
        this.evaluationRepository = evaluationRepository;
        this.teamRepository = teamRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.auditService = auditService;
    }

    // ── GET /api/reporting/export/{callId} ────────────────────────────────────
    // Vue "Excel button" hits this endpoint.
    // Returns the XLSX file as a binary download — browser prompts "Save As".

    @GetMapping("/export/{callId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportEvaluatorReport(
            @PathVariable Long callId,
            @AuthenticationPrincipal User actor) {
        try {
            byte[] xlsx = reportingService.generateEvaluationReport(callId);
            auditService.log(actor, "EXPORT", "CALL", callId, "XLSX evaluation report export");

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
    // Vue "admin dashboard" calls this on page load to populate the stat cards.
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String , Object>> getAdminStats() {
        Map<String , Object> stats = reportingService.getAdminStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/teams")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getTeamsReport(
            @RequestParam(required = false) Long callId,
            @RequestParam(required = false) String program,
            @RequestParam(required = false) String applicationStatus,
            @RequestParam(required = false) Boolean linkedToCall,
            @RequestParam(required = false) Boolean winnerOnly,
            @RequestParam(required = false) Integer minMembers,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reportingService.getTeamsReport(
                callId, program, applicationStatus, linkedToCall, winnerOnly, minMembers, page, size));
    }

    @GetMapping("/dashboard/student")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN','SUPER_ADMIN','EVALUATOR','SUPER_EVALUATOR')")
    public ResponseEntity<Map<String, Object>> studentDashboard(
            @AuthenticationPrincipal User authUser) {
        if (authUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(reportingService.getStudentDashboard(authUser.getId()));
    }

    @GetMapping("/dashboard/firm")
    @PreAuthorize("hasAnyRole('FIRM','FIRM_USER','ADMIN','SUPER_ADMIN','EVALUATOR','SUPER_EVALUATOR')")
    public ResponseEntity<Map<String, Object>> firmDashboard(
            @AuthenticationPrincipal User authUser) {
        if (authUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(reportingService.getFirmDashboard(authUser.getId()));
    }

    /**
     * Export the list of applications by filter: CSV, XLSX, PDF, DOCX.
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam String format,
            @RequestParam(defaultValue = "applications") String reportType,
            @RequestParam(required = false) Long callId,
            @RequestParam(required = false) String program,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String applicationStatus,
            @RequestParam(required = false) Boolean linkedToCall,
            @RequestParam(required = false) Boolean winnerOnly,
            @RequestParam(required = false) Integer minMembers,
            @AuthenticationPrincipal User actor) {
        try {
            auditService.log(actor, "EXPORT", "REPORT", callId,
                    "Export " + format.toUpperCase() + " report of type: " + reportType);
            String type = reportType == null ? "applications" : reportType.trim().toLowerCase(Locale.ROOT);
            byte[] body = switch (type) {
                case "teams" -> reportingService.exportTeamsReport(
                        format, callId, program,
                        applicationStatus != null ? applicationStatus : status,
                        linkedToCall, winnerOnly, minMembers);
                case "applications" -> reportingService.exportApplicationsReport(
                        format, callId, program, status, role);
                default -> throw new IllegalArgumentException("Unsupported reportType: " + reportType);
            };
            String ext = format == null ? "bin" : format.trim().toLowerCase();
            MediaType mediaType = switch (ext) {
                case "csv" -> MediaType.parseMediaType("text/csv; charset=UTF-8");
                case "xlsx" -> MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                case "pdf" -> MediaType.APPLICATION_PDF;
                case "docx" -> MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                default -> MediaType.APPLICATION_OCTET_STREAM;
            };
            String prefix = "teams".equals(type) ? "nti_teams_export_" : "nti_applications_export_";
            String filename = prefix
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"))
                    + "." + ext;
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .body(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
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
    @PreAuthorize("hasAnyRole('EVALUATOR','SUPER_EVALUATOR','ADMIN','SUPER_ADMIN')")
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
