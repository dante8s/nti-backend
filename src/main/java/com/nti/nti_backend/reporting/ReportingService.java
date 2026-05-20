package com.nti.nti_backend.reporting;

import com.nti.nti_backend.application.Application;
import com.nti.nti_backend.application.ApplicationRepository;
import com.nti.nti_backend.application.ApplicationStatus;
import com.nti.nti_backend.call.Call;
import com.nti.nti_backend.call.CallRepository;
import com.nti.nti_backend.call.CallStatus;
import com.nti.nti_backend.evaluation.Criteria;
import com.nti.nti_backend.evaluation.CriteriaRepository;
import com.nti.nti_backend.evaluation.Evaluation;
import com.nti.nti_backend.evaluation.EvaluationRepository;
import com.nti.nti_backend.mentorship.repository.MentorshipRepository;
import com.nti.nti_backend.milestone.MilestoneRepository;
import com.nti.nti_backend.milestone.entity.Milestone;
import com.nti.nti_backend.milestone.entity.MilestoneStatus;
import com.nti.nti_backend.organization.entity.OrgStatus;
import com.nti.nti_backend.organization.entity.Organization;
import com.nti.nti_backend.organization.repository.OrgMemberRepository;
import com.nti.nti_backend.organization.repository.OrganizationRepository;
import com.nti.nti_backend.program.Program;
import com.nti.nti_backend.program.ProgramRepository;
import com.nti.nti_backend.program.ProgramType;
import com.nti.nti_backend.studentProfile.StudentProfileRepository;
import com.nti.nti_backend.team.Team;
import com.nti.nti_backend.team.TeamRepository;
import com.nti.nti_backend.teamMember.TeamMember;
import com.nti.nti_backend.teamMember.TeamMemberRepository;
import com.nti.nti_backend.user.Role;
import com.nti.nti_backend.user.User;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportingService {

    private static final float PDF_MARGIN = 36f;
    private static final float PDF_TABLE_FONT = 7f;
    private static final float PDF_TABLE_LEADING = 9f;
    private static final float PDF_CELL_PAD = 3f;

    private final CriteriaRepository criteriaRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final EvaluationRepository evaluationRepository;
    private final ApplicationRepository applicationRepository;
    private final CallRepository callRepository;
    private final OrganizationRepository organizationRepository;
    private final ProgramRepository programRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final MilestoneRepository milestoneRepository;
    private final MentorshipRepository mentorshipRepository;

    public ReportingService(CriteriaRepository criteriaRepository,
                            StudentProfileRepository studentProfileRepository,
                            TeamRepository teamRepository,
                            TeamMemberRepository teamMemberRepository,
                            EvaluationRepository evaluationRepository,
                            ApplicationRepository applicationRepository,
                            CallRepository callRepository,
                            OrganizationRepository organizationRepository,
                            ProgramRepository programRepository,
                            OrgMemberRepository orgMemberRepository,
                            MilestoneRepository milestoneRepository,
                            MentorshipRepository mentorshipRepository) {
        this.studentProfileRepository = studentProfileRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.criteriaRepository = criteriaRepository;
        this.evaluationRepository = evaluationRepository;
        this.applicationRepository = applicationRepository;
        this.callRepository = callRepository;
        this.organizationRepository = organizationRepository;
        this.programRepository = programRepository;
        this.orgMemberRepository = orgMemberRepository;
        this.milestoneRepository = milestoneRepository;
        this.mentorshipRepository = mentorshipRepository;
    }

    public byte[] generateEvaluationReport(Long callId) throws IOException {
        List<Criteria> criteria = criteriaRepository.findByCall_IdOrderBySortOrderAsc(callId);
        List<Evaluation> evaluations = evaluationRepository.findAllByCallId(callId);

        Map<Long, List<Evaluation>> byApplication = evaluations.stream()
                .collect(Collectors.groupingBy(e -> e.getApplication().getId()));

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Evaluation Report");

            CellStyle headerStyle = buildHeaderStyle(workbook);
            Row header = sheet.createRow(0);
            int col = 0;

            createStyledCell(header, col++, "Application ID", headerStyle);
            createStyledCell(header, col++, "Applicant", headerStyle);

            for (Criteria c : criteria) {
                Integer w = c.getWeightPercent();
                createStyledCell(header, col++,
                        c.getName() + " (" + (w != null ? w : 0) + "%)", headerStyle);
            }

            createStyledCell(header, col++, "Weighted Avg", headerStyle);
            createStyledCell(header, col++, "Recommendation", headerStyle);

            int rowNum = 1;
            for (Map.Entry<Long, List<Evaluation>> entry : byApplication.entrySet()) {
                Long appId = entry.getKey();
                List<Evaluation> appEvals = entry.getValue();

                Row row = sheet.createRow(rowNum++);
                col = 0;

                row.createCell(col++).setCellValue(appId);

                String applicationName = "Unknown";
                if (!appEvals.isEmpty()
                        && appEvals.get(0).getApplication() != null
                        && appEvals.get(0).getApplication().getApplicant() != null) {
                    String applicantName = appEvals.get(0).getApplication().getApplicant().getName();
                    Long applicantId = appEvals.get(0).getApplication().getApplicant().getId();
                    applicationName = applicantName != null && !applicantName.isBlank()
                            ? applicantName
                            : ("User#" + applicantId);
                }

                row.createCell(col++).setCellValue(applicationName);

                Map<Long, Double> scoreByCriteria = appEvals.stream()
                        .collect(Collectors.groupingBy(
                                e -> e.getCriteria().getId(),
                                Collectors.averagingDouble(Evaluation::getScore)
                        ));

                double weightedSum = 0;
                int totalWeight = 0;
                for (Criteria c : criteria) {
                    Double score = scoreByCriteria.get(c.getId());
                    row.createCell(col++).setCellValue(score != null ? score : 0);
                    if (score != null && c.getWeightPercent() != null) {
                        weightedSum += score * c.getWeightPercent();
                        totalWeight += c.getWeightPercent();
                    }
                }

                double weightedAvg = totalWeight > 0 ? weightedSum / totalWeight : 0;
                row.createCell(col++).setCellValue(
                        Math.round(weightedAvg * 100.0) / 100.0);

                String recommendation = appEvals.stream()
                        .filter(e -> e.getRecommendation() != null)
                        .collect(Collectors.groupingBy(
                                e -> e.getRecommendation().name(), Collectors.counting()))
                        .entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("PENDING");
                row.createCell(col).setCellValue(recommendation);
            }

            for (int i = 0; i <= col; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            return out.toByteArray();
        }
    }

    public Map<String, Object> getAdminStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("openCalls", callRepository.countByStatus(CallStatus.OPEN));
        stats.put("totalApplications", applicationRepository.count());
        stats.put("applicationsDraft", applicationRepository.countByStatus(ApplicationStatus.DRAFT));
        stats.put("applicationsSubmitted", applicationRepository.countByStatus(ApplicationStatus.SUBMITTED));
        stats.put("applicationsInReview", applicationRepository.countByStatus(ApplicationStatus.IN_REVIEW));
        stats.put("applicationsNeedsRevision", applicationRepository.countByStatus(ApplicationStatus.NEEDS_REVISION));
        stats.put("applicationsApproved", applicationRepository.countByStatus(ApplicationStatus.APPROVED));
        stats.put("applicationsRejected", applicationRepository.countByStatus(ApplicationStatus.REJECTED));
        stats.put("activePartnerOrganizations", organizationRepository.countByStatus(OrgStatus.ACTIVE));
        stats.put("totalOrganizations", organizationRepository.count());

        stats.put("totalStudentProfiles", studentProfileRepository.count());
        stats.put("profileWithCv", studentProfileRepository.countByCvFilePathIsNotNull());
        stats.put("completeProfiles", studentProfileRepository.countByProfileCompleteTrue());
        stats.put("averageGrade", studentProfileRepository.findAverageProfileGrade());
        stats.put("totalTeams", teamRepository.count());
        stats.put("eligibleTeams", teamRepository.findTeamsWithMinimumSize(3).size());
        stats.put("callsWithWinningTeam",
                applicationRepository.countDistinctCallsByStatus(ApplicationStatus.APPROVED));

        return stats;
    }

    public Map<String, Object> getTeamsReport(Long callId,
                                              String program,
                                              String applicationStatus,
                                              Boolean linkedToCall,
                                              Boolean winnerOnly,
                                              Integer minMembers,
                                              int page,
                                              int size) {
        List<Map<String, Object>> rows = buildTeamListRows(
                callId, program, applicationStatus, linkedToCall, winnerOnly, minMembers);
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 200);
        int total = rows.size();
        int from = Math.min(safePage * safeSize, total);
        int to = Math.min(from + safeSize, total);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", rows.subList(from, to));
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    public byte[] exportTeamsReport(String format,
                                    Long callId,
                                    String program,
                                    String applicationStatus,
                                    Boolean linkedToCall,
                                    Boolean winnerOnly,
                                    Integer minMembers) throws IOException {
        String f = format == null ? "csv" : format.trim().toLowerCase(Locale.ROOT);
        List<String[]> table = buildTeamExportTable(
                callId, program, applicationStatus, linkedToCall, winnerOnly, minMembers);

        return switch (f) {
            case "csv" -> toCsv(table);
            case "xlsx" -> toXlsx(table, "Teams");
            case "pdf" -> toPdf(table);
            case "docx" -> toDocx(table);
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        };
    }

    public Map<String, Object> getStudentDashboard(Long userId) {
        Map<String, Object> root = new LinkedHashMap<>();
        List<Application> apps = applicationRepository.findByApplicantId(userId);
        List<Map<String, Object>> rows = new ArrayList<>();
        int pendingMilestones = 0;
        int overdueMilestones = 0;
        LocalDate today = LocalDate.now();

        for (Application a : apps) {
            Call c = a.getCall();
            Program p = c != null ? c.getProgram() : null;
            List<Milestone> ms = milestoneRepository.findAllByApplication_Id(a.getId());
            int pend = 0;
            int overdue = 0;
            for (Milestone m : ms) {
                if (m.getStatus() == MilestoneStatus.PENDING_APPROVAL) {
                    pend++;
                }
                boolean notDone = m.getStatus() != MilestoneStatus.COMPLETED;
                if (m.getStatus() == MilestoneStatus.OVERDUE
                        || (notDone && m.getDueDate() != null && m.getDueDate().isBefore(today))) {
                    overdue++;
                }
            }
            pendingMilestones += pend;
            overdueMilestones += overdue;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("applicationId", a.getId());
            row.put("status", a.getStatus().name());
            row.put("callId", c != null ? c.getId() : null);
            row.put("callTitle", c != null ? c.getTitle() : null);
            row.put("callDeadline", c != null ? c.getDeadline() : null);
            row.put("callStatus", c != null ? c.getStatus().name() : null);
            row.put("programName", p != null ? p.getName() : null);
            row.put("programType", p != null ? p.getType().name() : null);
            row.put("pendingApprovalMilestones", pend);
            row.put("overdueOrAttentionMilestones", overdue);
            row.put("totalMilestones", ms.size());
            rows.add(row);
        }

        List<Map<String, Object>> teams = new ArrayList<>();
        for (Team t : teamRepository.findAcceptedTeamsByUserId(userId)) {
            Map<String, Object> tr = new LinkedHashMap<>();
            tr.put("teamId", t.getId());
            tr.put("name", t.getName());
            boolean leader = t.getLeader() != null && Objects.equals(t.getLeader().getId(), userId);
            tr.put("role", leader ? "LEADER" : "MEMBER");
            int accepted = 0;
            if (t.getMembers() != null) {
                accepted = (int) t.getMembers().stream()
                        .filter(m -> m.getInviteStatus() == TeamMember.InviteStatus.ACCEPTED)
                        .count();
            }
            tr.put("acceptedMembers", accepted);
            teams.add(tr);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("applicationCount", apps.size());
        summary.put("pendingApprovalMilestones", pendingMilestones);
        summary.put("overdueOrAttentionMilestones", overdueMilestones);
        summary.put("teamCount", teams.size());

        root.put("summary", summary);
        root.put("applications", rows);
        root.put("teams", teams);
        return root;
    }

    public Map<String, Object> getFirmDashboard(Long userId) {
        Map<String, Object> root = new LinkedHashMap<>();
        var memberships = orgMemberRepository.findAllByUserId(userId);
        if (memberships.isEmpty()) {
            root.put("organizations", List.of());
            root.put("summary", Map.of(
                    "programCount", 0,
                    "applicationCount", 0,
                    "mentorshipCount", 0,
                    "openCallCount", 0
            ));
            return root;
        }

        List<Map<String, Object>> orgBlocks = new ArrayList<>();
        int totalPrograms = 0;
        int totalApplications = 0;
        int totalMentorships = 0;
        int openCalls = 0;

        for (var om : memberships) {
            Organization org = om.getOrganization();
            if (org == null) {
                continue;
            }
            UUID oid = org.getId();
            List<Program> programs = programRepository.findByOrganizationId(oid);
            long appCnt = applicationRepository.countByProgramOrganizationId(oid);
            long menCnt = mentorshipRepository.countByProgramOrganizationId(oid);

            List<Map<String, Object>> programRows = new ArrayList<>();
            for (Program pr : programs) {
                Map<String, Object> prRow = new LinkedHashMap<>();
                prRow.put("programId", pr.getId());
                prRow.put("name", pr.getName());
                prRow.put("type", pr.getType().name());
                prRow.put("status", pr.getStatus().name());
                prRow.put("applicationsOnProgram", applicationRepository.countByCall_Program_Id(pr.getId()));
                programRows.add(prRow);
            }

            List<Map<String, Object>> callRows = new ArrayList<>();
            for (Program pr : programs) {
                for (Call call : callRepository.findByProgramId(pr.getId())) {
                    if (call.getStatus() == CallStatus.OPEN) {
                        openCalls++;
                    }
                    Map<String, Object> cr = new LinkedHashMap<>();
                    cr.put("callId", call.getId());
                    cr.put("title", call.getTitle());
                    cr.put("deadline", call.getDeadline());
                    cr.put("status", call.getStatus().name());
                    cr.put("programName", pr.getName());
                    cr.put("applicationsOnCall", applicationRepository.countByCallId(call.getId()));
                    callRows.add(cr);
                }
            }

            totalPrograms += programs.size();
            totalApplications += (int) appCnt;
            totalMentorships += (int) menCnt;

            Map<String, Object> block = new LinkedHashMap<>();
            block.put("organizationId", oid);
            block.put("organizationName", org.getName());
            block.put("programs", programRows);
            block.put("calls", callRows);
            block.put("applicationCount", appCnt);
            block.put("mentorshipCount", menCnt);
            orgBlocks.add(block);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("programCount", totalPrograms);
        summary.put("applicationCount", totalApplications);
        summary.put("mentorshipCount", totalMentorships);
        summary.put("openCallCount", openCalls);

        root.put("organizations", orgBlocks);
        root.put("summary", summary);
        return root;
    }

    public byte[] exportApplicationsReport(String format,
                                           Long callId,
                                           String program,
                                           String status,
                                           String roleFilter) throws IOException {
        String f = format == null ? "csv" : format.trim().toLowerCase(Locale.ROOT);
        ProgramType programType = parseProgram(program);
        ApplicationStatus st = parseStatus(status);

        List<Application> filtered = filterApplications(
                applicationRepository.findAllForReportingExport(),
                callId,
                programType,
                st,
                roleFilter);

        List<String[]> table = buildExportRows(filtered);

        return switch (f) {
            case "csv" -> toCsv(table);
            case "xlsx" -> toXlsx(table, "Applications");
            case "pdf" -> toPdf(table);
            case "docx" -> toDocx(table);
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        };
    }

    private List<Map<String, Object>> buildTeamListRows(Long callIdFilter,
                                                        String program,
                                                        String applicationStatusStr,
                                                        Boolean linkedToCall,
                                                        Boolean winnerOnly,
                                                        Integer minMembers) {
        List<Team> teams = teamRepository.findAllForReporting();
        Map<Long, List<Application>> appsByLeader = loadApplicationsByLeader(teams);
        ApplicationStatus statusFilter = parseStatus(applicationStatusStr);
        ProgramType programType = parseProgram(program);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Team team : teams) {
            if (!passesMinMembers(team, minMembers)) {
                continue;
            }
            User leader = team.getLeader();
            Long leaderId = leader != null ? leader.getId() : null;
            List<Application> leaderApps = leaderId == null
                    ? List.of()
                    : appsByLeader.getOrDefault(leaderId, List.of());

            if (!passesLinkedFilter(leaderApps, linkedToCall)) {
                continue;
            }

            List<Application> filtered = filterTeamApplications(
                    leaderApps, callIdFilter, programType, statusFilter);

            if (Boolean.TRUE.equals(linkedToCall) && filtered.isEmpty()) {
                continue;
            }
            if (callIdFilter != null || programType != null || statusFilter != null) {
                if (filtered.isEmpty()) {
                    continue;
                }
            }
            if (Boolean.TRUE.equals(winnerOnly) && !hasApprovedApplication(filtered)) {
                continue;
            }

            Application primary = filtered.isEmpty() ? null : filtered.get(0);
            rows.add(buildTeamRowMap(team, leader, leaderApps, filtered, primary, true));
        }
        return rows;
    }

    private List<String[]> buildTeamExportTable(Long callIdFilter,
                                                String program,
                                                String applicationStatusStr,
                                                Boolean linkedToCall,
                                                Boolean winnerOnly,
                                                Integer minMembers) {
        List<Team> teams = teamRepository.findAllForReporting();
        Map<Long, List<Application>> appsByLeader = loadApplicationsByLeader(teams);
        ApplicationStatus statusFilter = parseStatus(applicationStatusStr);
        ProgramType programType = parseProgram(program);

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{
                "teamId", "teamName", "leaderId", "leaderName", "leaderEmail",
                "acceptedMembers", "maxCapacity", "linkedToCall",
                "applicationId", "callId", "callTitle", "callDeadline", "programType",
                "organizationName", "applicationStatus", "isWinner",
                "applicationsCount", "pendingApprovalMilestones",
                "overdueOrAttentionMilestones", "totalMilestones"
        });

        DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        for (Team team : teams) {
            if (!passesMinMembers(team, minMembers)) {
                continue;
            }
            User leader = team.getLeader();
            Long leaderId = leader != null ? leader.getId() : null;
            List<Application> leaderApps = leaderId == null
                    ? List.of()
                    : appsByLeader.getOrDefault(leaderId, List.of());

            if (!passesLinkedFilter(leaderApps, linkedToCall)) {
                continue;
            }

            List<Application> filtered = filterTeamApplications(
                    leaderApps, callIdFilter, programType, statusFilter);

            if (Boolean.TRUE.equals(linkedToCall) && filtered.isEmpty()) {
                continue;
            }
            if (callIdFilter != null || programType != null || statusFilter != null) {
                if (filtered.isEmpty()) {
                    continue;
                }
            }
            if (Boolean.TRUE.equals(winnerOnly) && !hasApprovedApplication(filtered)) {
                continue;
            }

            long accepted = teamMemberRepository.countAcceptedMembers(team.getId());
            MilestoneTotals totals = aggregateMilestonesForApplications(filtered.isEmpty()
                    ? leaderApps
                    : filtered);

            if (filtered.isEmpty()) {
                rows.add(teamExportCells(team, leader, accepted, null, leaderApps.size(), totals, dtf));
            } else {
                for (Application app : filtered) {
                    rows.add(teamExportCells(team, leader, accepted, app, leaderApps.size(), totals, dtf));
                }
            }
        }
        return rows;
    }

    private Map<Long, List<Application>> loadApplicationsByLeader(List<Team> teams) {
        List<Long> leaderIds = teams.stream()
                .map(Team::getLeader)
                .filter(Objects::nonNull)
                .map(User::getId)
                .distinct()
                .toList();
        if (leaderIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<Application>> grouped = new LinkedHashMap<>();
        for (Application app : applicationRepository.findByApplicantIdInWithDetails(leaderIds)) {
            Long applicantId = app.getApplicant().getId();
            grouped.computeIfAbsent(applicantId, k -> new ArrayList<>()).add(app);
        }
        return grouped;
    }

    private boolean passesMinMembers(Team team, Integer minMembers) {
        if (minMembers == null || minMembers < 1) {
            return true;
        }
        return teamMemberRepository.countAcceptedMembers(team.getId()) >= minMembers;
    }

    private boolean passesLinkedFilter(List<Application> leaderApps, Boolean linkedToCall) {
        if (linkedToCall == null) {
            return true;
        }
        boolean hasAny = !leaderApps.isEmpty();
        return linkedToCall ? hasAny : !hasAny;
    }

    private List<Application> filterTeamApplications(List<Application> apps,
                                                     Long callIdFilter,
                                                     ProgramType programType,
                                                     ApplicationStatus statusFilter) {
        return apps.stream().filter(a -> {
            Call c = a.getCall();
            if (c == null) {
                return false;
            }
            if (callIdFilter != null && !Objects.equals(c.getId(), callIdFilter)) {
                return false;
            }
            Program p = c.getProgram();
            if (programType != null && (p == null || p.getType() != programType)) {
                return false;
            }
            if (statusFilter != null && a.getStatus() != statusFilter) {
                return false;
            }
            return true;
        }).toList();
    }

    private boolean hasApprovedApplication(List<Application> apps) {
        return apps.stream().anyMatch(a -> a.getStatus() == ApplicationStatus.APPROVED);
    }

    private Map<String, Object> buildTeamRowMap(Team team,
                                                User leader,
                                                List<Application> allLeaderApps,
                                                List<Application> filteredApps,
                                                Application primary,
                                                boolean listView) {
        long accepted = teamMemberRepository.countAcceptedMembers(team.getId());
        List<Application> milestoneSource = filteredApps.isEmpty() ? allLeaderApps : filteredApps;
        MilestoneTotals totals = aggregateMilestonesForApplications(milestoneSource);

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("teamId", team.getId());
        row.put("teamName", team.getName());
        row.put("leaderId", leader != null ? leader.getId() : null);
        row.put("leaderName", leader != null ? leader.getName() : null);
        row.put("leaderEmail", leader != null ? leader.getEmail() : null);
        row.put("acceptedMembers", accepted);
        row.put("maxCapacity", team.getMaxCapacity());
        row.put("applicationsCount", allLeaderApps.size());
        row.put("linkedToCall", !allLeaderApps.isEmpty());
        row.put("pendingApprovalMilestones", totals.pendingApproval);
        row.put("overdueOrAttentionMilestones", totals.overdueOrAttention);
        row.put("totalMilestones", totals.total);

        if (primary == null) {
            row.put("applicationId", null);
            row.put("callId", null);
            row.put("callTitle", null);
            row.put("callDeadline", null);
            row.put("programType", null);
            row.put("organizationName", null);
            row.put("applicationStatus", null);
            row.put("isWinner", false);
        } else {
            Call c = primary.getCall();
            Program p = c != null ? c.getProgram() : null;
            Organization o = p != null ? p.getOrganization() : null;
            row.put("applicationId", primary.getId());
            row.put("callId", c != null ? c.getId() : null);
            row.put("callTitle", c != null ? c.getTitle() : null);
            row.put("callDeadline", c != null ? c.getDeadline() : null);
            row.put("programType", p != null ? p.getType().name() : null);
            row.put("organizationName", o != null ? o.getName() : null);
            row.put("applicationStatus", primary.getStatus().name());
            row.put("isWinner", primary.getStatus() == ApplicationStatus.APPROVED);
        }

        if (listView && filteredApps.size() > 1) {
            row.put("additionalCallsCount", filteredApps.size() - 1);
        }
        return row;
    }

    private String[] teamExportCells(Team team,
                                     User leader,
                                     long acceptedMembers,
                                     Application app,
                                     int applicationsCount,
                                     MilestoneTotals totals,
                                     DateTimeFormatter dtf) {
        boolean linked = applicationsCount > 0;
        if (app == null) {
            return new String[]{
                    String.valueOf(team.getId()),
                    safe(team.getName()),
                    leader != null ? String.valueOf(leader.getId()) : "",
                    safe(leader != null ? leader.getName() : null),
                    safe(leader != null ? leader.getEmail() : null),
                    String.valueOf(acceptedMembers),
                    team.getMaxCapacity() != null ? String.valueOf(team.getMaxCapacity()) : "",
                    String.valueOf(linked),
                    "", "", "", "", "", "",
                    "", "false",
                    String.valueOf(applicationsCount),
                    String.valueOf(totals.pendingApproval),
                    String.valueOf(totals.overdueOrAttention),
                    String.valueOf(totals.total)
            };
        }
        Call c = app.getCall();
        Program p = c != null ? c.getProgram() : null;
        Organization o = p != null ? p.getOrganization() : null;
        return new String[]{
                String.valueOf(team.getId()),
                safe(team.getName()),
                leader != null ? String.valueOf(leader.getId()) : "",
                safe(leader != null ? leader.getName() : null),
                safe(leader != null ? leader.getEmail() : null),
                String.valueOf(acceptedMembers),
                team.getMaxCapacity() != null ? String.valueOf(team.getMaxCapacity()) : "",
                String.valueOf(linked),
                String.valueOf(app.getId()),
                c != null ? String.valueOf(c.getId()) : "",
                safe(c != null ? c.getTitle() : null),
                c != null && c.getDeadline() != null ? dtf.format(c.getDeadline()) : "",
                p != null ? p.getType().name() : "",
                safe(o != null ? o.getName() : null),
                app.getStatus().name(),
                String.valueOf(app.getStatus() == ApplicationStatus.APPROVED),
                String.valueOf(applicationsCount),
                String.valueOf(totals.pendingApproval),
                String.valueOf(totals.overdueOrAttention),
                String.valueOf(totals.total)
        };
    }

    private MilestoneTotals aggregateMilestonesForApplications(List<Application> applications) {
        MilestoneTotals totals = new MilestoneTotals();
        LocalDate today = LocalDate.now();
        Set<Long> seenAppIds = new HashSet<>();
        for (Application app : applications) {
            if (!seenAppIds.add(app.getId())) {
                continue;
            }
            for (Milestone m : milestoneRepository.findAllByApplication_Id(app.getId())) {
                totals.total++;
                if (m.getStatus() == MilestoneStatus.PENDING_APPROVAL) {
                    totals.pendingApproval++;
                }
                boolean notDone = m.getStatus() != MilestoneStatus.COMPLETED;
                if (m.getStatus() == MilestoneStatus.OVERDUE
                        || (notDone && m.getDueDate() != null && m.getDueDate().isBefore(today))) {
                    totals.overdueOrAttention++;
                }
            }
        }
        return totals;
    }

    private static final class MilestoneTotals {
        int pendingApproval;
        int overdueOrAttention;
        int total;
    }

    private List<Application> filterApplications(List<Application> all,
                                                 Long callId,
                                                 ProgramType programType,
                                                 ApplicationStatus status,
                                                 String roleFilter) {
        String rf = roleFilter == null || roleFilter.isBlank()
                ? null
                : roleFilter.trim().toLowerCase(Locale.ROOT);

        return all.stream().filter(a -> {
            Call c = a.getCall();
            if (c == null) {
                return false;
            }
            if (callId != null && !Objects.equals(c.getId(), callId)) {
                return false;
            }
            Program p = c.getProgram();
            if (programType != null && (p == null || p.getType() != programType)) {
                return false;
            }
            if (status != null && a.getStatus() != status) {
                return false;
            }
            User applicant = a.getApplicant();
            if (rf != null && applicant != null) {
                Set<Role> roles = applicant.getRoles();
                if ("student".equals(rf)) {
                    if (!roles.contains(Role.STUDENT)) {
                        return false;
                    }
                } else if ("company".equals(rf)) {
                    if (!roles.contains(Role.FIRM) && !roles.contains(Role.FIRM_USER)) {
                        return false;
                    }
                } else if ("evaluator".equals(rf)) {
                    if (!roles.contains(Role.EVALUATOR) && !roles.contains(Role.SUPER_EVALUATOR)) {
                        return false;
                    }
                }
            }
            return true;
        }).toList();
    }

    private List<String[]> buildExportRows(List<Application> apps) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{
                "applicationId", "callId", "callTitle", "programName", "programType",
                "organization", "applicantId", "applicantName", "applicantEmail",
                "applicantRoles", "status", "updatedAt"
        });
        DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        for (Application a : apps) {
            Call c = a.getCall();
            Program p = c != null ? c.getProgram() : null;
            Organization o = p != null ? p.getOrganization() : null;
            User u = a.getApplicant();
            String roles = u != null && u.getRoles() != null
                    ? u.getRoles().stream().map(Enum::name).collect(Collectors.joining(";"))
                    : "";
            rows.add(new String[]{
                    String.valueOf(a.getId()),
                    c != null ? String.valueOf(c.getId()) : "",
                    safe(c != null ? c.getTitle() : null),
                    safe(p != null ? p.getName() : null),
                    p != null ? p.getType().name() : "",
                    safe(o != null ? o.getName() : null),
                    u != null ? String.valueOf(u.getId()) : "",
                    safe(u != null ? u.getName() : null),
                    safe(u != null ? u.getEmail() : null),
                    roles,
                    a.getStatus().name(),
                    a.getUpdatedAt() != null ? dtf.format(a.getUpdatedAt()) : ""
            });
        }
        return rows;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /** PDF Type1 fonts are limited; avoid showText failures on Cyrillic. */
    private static String asciiOnly(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            b.append(ch >= 32 && ch < 127 ? ch : '?');
        }
        return b.toString();
    }

    private static ProgramType parseProgram(String program) {
        if (program == null || program.isBlank()) {
            return null;
        }
        return switch (program.trim().toUpperCase(Locale.ROOT)) {
            case "A", "PROGRAM_A" -> ProgramType.PROGRAM_A;
            case "B", "PROGRAM_B" -> ProgramType.PROGRAM_B;
            default -> null;
        };
    }

    private static ApplicationStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ApplicationStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private byte[] toCsv(List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (i > 0) {
                    sb.append(';');
                }
                String cell = row[i] == null ? "" : row[i];
                if (cell.contains(";") || cell.contains("\"") || cell.contains("\n")) {
                    sb.append('"').append(cell.replace("\"", "\"\"")).append('"');
                } else {
                    sb.append(cell);
                }
            }
            sb.append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] toXlsx(List<String[]> rows) throws IOException {
        return toXlsx(rows, "Applications");
    }

    private byte[] toXlsx(List<String[]> rows, String sheetName) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(sheetName != null ? sheetName : "Report");
            CellStyle headerStyle = buildHeaderStyle(wb);
            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r);
                String[] cells = rows.get(r);
                for (int c = 0; c < cells.length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(cells[c] != null ? cells[c] : "");
                    if (r == 0) {
                        cell.setCellStyle(headerStyle);
                    }
                }
            }
            for (int c = 0; c < rows.get(0).length; c++) {
                sheet.autoSizeColumn(c);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    private byte[] toPdf(List<String[]> rows) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            if (rows.isEmpty()) {
                PDPage page = new PDPage(landscapeA4());
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    drawPdfTitle(cs, page.getMediaBox().getHeight() - PDF_MARGIN);
                }
            } else {
                String[] header = rows.get(0);
                List<String[]> data = rows.subList(1, rows.size());
                int dataIndex = 0;
                boolean first = true;
                int stall = 0;
                int pageGuard = 0;
                while (first || dataIndex < data.size()) {
                    first = false;
                    if (++pageGuard > 500) {
                        break;
                    }
                    int beforePageIndex = dataIndex;
                    PDPage page = new PDPage(landscapeA4());
                    doc.addPage(page);
                    float pageH = page.getMediaBox().getHeight();
                    float pageW = page.getMediaBox().getWidth();
                    float bottom = PDF_MARGIN;
                    float tableLeft = PDF_MARGIN;
                    float tableWidth = pageW - 2 * PDF_MARGIN;

                    try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                        float yTop = pageH - PDF_MARGIN;
                        yTop = drawPdfTitle(cs, yTop);

                        float[] colWidths = pdfColumnWidths(tableWidth, header.length);
                        yTop = drawPdfTableHeader(cs, tableLeft, yTop, bottom, header, colWidths);

                        while (dataIndex < data.size()) {
                            float rowH = measurePdfDataRowHeight(data.get(dataIndex), colWidths);
                            if (yTop - rowH < bottom) {
                                break;
                            }
                            yTop = drawPdfDataRow(cs, tableLeft, yTop, colWidths, data.get(dataIndex), rowH);
                            dataIndex++;
                        }
                    }
                    if (dataIndex == beforePageIndex && dataIndex < data.size()) {
                        if (++stall > 5) {
                            dataIndex++;
                            stall = 0;
                        }
                    } else {
                        stall = 0;
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private static PDRectangle landscapeA4() {
        return new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
    }

    private static float[] pdfColumnWidths(float tableWidth, int cols) {
        /* Трохи ширші колонки для текстів (як у Word); сума = 1.0 */
        double[] w = {
                0.055, 0.045, 0.10, 0.085, 0.065, 0.075,
                0.045, 0.075, 0.095, 0.12, 0.065, 0.115
        };
        if (cols != w.length) {
            float eq = tableWidth / cols;
            float[] uniform = new float[cols];
            Arrays.fill(uniform, eq);
            return uniform;
        }
        float[] out = new float[cols];
        for (int i = 0; i < cols; i++) {
            out[i] = (float) (tableWidth * w[i]);
        }
        return out;
    }

    private static float drawPdfTitle(PDPageContentStream cs, float yTop) throws IOException {
        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        cs.beginText();
        cs.newLineAtOffset(PDF_MARGIN, yTop - 14);
        cs.showText("NTI export zayavok");
        cs.endText();
        return yTop - 28;
    }

    private static float pdfStringWidth(PDType1Font font, float fontSize, String text) throws IOException {
        return font.getStringWidth(text) / 1000f * fontSize;
    }

    private static List<String> wrapPdfCell(String cell, PDType1Font font, float fontSize, float maxWidth)
            throws IOException {
        String t = asciiOnly(cell == null ? "" : cell).replace('\t', ' ');
        if (t.isEmpty()) {
            return List.of("");
        }
        List<String> lines = new ArrayList<>();
        int start = 0;
        while (start < t.length()) {
            int end = t.length();
            while (end > start && pdfStringWidth(font, fontSize, t.substring(start, end)) > maxWidth - 2 * PDF_CELL_PAD) {
                end--;
            }
            if (end == start) {
                end = start + 1;
            }
            lines.add(t.substring(start, end));
            start = end;
        }
        return lines;
    }

    private static float measurePdfHeaderHeight(String[] header, float[] colWidths) throws IOException {
        float maxLines = 1;
        for (int c = 0; c < header.length; c++) {
            List<String> lines = wrapPdfCell(header[c], PDType1Font.HELVETICA_BOLD, PDF_TABLE_FONT, colWidths[c]);
            maxLines = Math.max(maxLines, lines.size());
        }
        return maxLines * PDF_TABLE_LEADING + 2 * PDF_CELL_PAD;
    }

    private static float drawPdfTableHeader(PDPageContentStream cs, float tableLeft, float yTop, float bottom,
                                            String[] header, float[] colWidths) throws IOException {
        float rowH = measurePdfHeaderHeight(header, colWidths);
        float rowBottom = yTop - rowH;
        if (rowBottom < bottom) {
            return rowBottom;
        }
        float x = tableLeft;
        for (int c = 0; c < header.length; c++) {
            float cw = colWidths[c];
            cs.setNonStrokingColor(0.88f, 0.88f, 0.88f);
            cs.addRect(x, rowBottom, cw, rowH);
            cs.fill();
            cs.setStrokingColor(0.45f, 0.45f, 0.45f);
            cs.setLineWidth(0.5f);
            cs.addRect(x, rowBottom, cw, rowH);
            cs.stroke();
            cs.setNonStrokingColor(0f, 0f, 0f);
            List<String> lines = wrapPdfCell(header[c], PDType1Font.HELVETICA_BOLD, PDF_TABLE_FONT, cw);
            float textY = rowTopBaseline(rowBottom, rowH, lines.size());
            drawPdfTextLines(cs, PDType1Font.HELVETICA_BOLD, PDF_TABLE_FONT, x + PDF_CELL_PAD, textY, lines);
            x += cw;
        }
        return rowBottom;
    }

    private static void drawPdfTextLines(PDPageContentStream cs, PDType1Font font, float fontSize,
                                         float leftX, float baselineY, List<String> lines) throws IOException {
        if (lines.isEmpty()) {
            return;
        }
        cs.setFont(font, fontSize);
        cs.beginText();
        cs.newLineAtOffset(leftX, baselineY);
        cs.showText(lines.get(0));
        for (int i = 1; i < lines.size(); i++) {
            cs.newLineAtOffset(0, -PDF_TABLE_LEADING);
            cs.showText(lines.get(i));
        }
        cs.endText();
    }

    private static float rowTopBaseline(float rowBottom, float rowHeight, int lineCount) {
        float blockH = lineCount * PDF_TABLE_LEADING;
        float padTop = (rowHeight - blockH) / 2f;
        return rowBottom + rowHeight - padTop - PDF_TABLE_FONT * 0.85f;
    }

    private static float measurePdfDataRowHeight(String[] cells, float[] colWidths) throws IOException {
        float maxLines = 1;
        for (int c = 0; c < cells.length; c++) {
            List<String> lines = wrapPdfCell(cells[c], PDType1Font.HELVETICA, PDF_TABLE_FONT, colWidths[c]);
            maxLines = Math.max(maxLines, lines.size());
        }
        return maxLines * PDF_TABLE_LEADING + 2 * PDF_CELL_PAD;
    }

    private static float drawPdfDataRow(PDPageContentStream cs, float tableLeft, float yTop,
                                        float[] colWidths, String[] cells, float rowH) throws IOException {
        float rowBottom = yTop - rowH;
        float x = tableLeft;
        for (int c = 0; c < cells.length; c++) {
            float cw = colWidths[c];
            cs.setNonStrokingColor(1f, 1f, 1f);
            cs.addRect(x, rowBottom, cw, rowH);
            cs.fill();
            cs.setStrokingColor(0.55f, 0.55f, 0.55f);
            cs.setLineWidth(0.4f);
            cs.addRect(x, rowBottom, cw, rowH);
            cs.stroke();
            cs.setNonStrokingColor(0f, 0f, 0f);
            List<String> lines = wrapPdfCell(cells[c], PDType1Font.HELVETICA, PDF_TABLE_FONT, cw);
            float textY = rowTopBaseline(rowBottom, rowH, lines.size());
            drawPdfTextLines(cs, PDType1Font.HELVETICA, PDF_TABLE_FONT, x + PDF_CELL_PAD, textY, lines);
            x += cw;
        }
        return rowBottom;
    }

    private byte[] toDocx(List<String[]> rows) throws IOException {
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph title = doc.createParagraph();
            XWPFRun tr = title.createRun();
            tr.setBold(true);
            tr.setText("NTI export zayavok");
            tr.addBreak();

            if (rows.isEmpty()) {
                XWPFParagraph p = doc.createParagraph();
                p.createRun().setText("(empty)");
            } else {
                int cols = rows.get(0).length;
                XWPFTable table = doc.createTable(rows.size(), cols);
                for (int r = 0; r < rows.size(); r++) {
                    String[] cells = rows.get(r);
                    for (int c = 0; c < cols; c++) {
                        XWPFTableCell cell = table.getRow(r).getCell(c);
                        String val = c < cells.length && cells[c] != null ? cells[c] : "";
                        cell.setText(val);
                    }
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }

    private CellStyle buildHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private void createStyledCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
}
