package com.nti.nti_backend.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nti.nti_backend.application.Application;
import com.nti.nti_backend.application.DocumentRepository;
import com.nti.nti_backend.application.DocumentType;
import com.nti.nti_backend.evaluation.EvaluationRepository;
import com.nti.nti_backend.milestone.MilestoneRepository;
import com.nti.nti_backend.milestone.entity.MilestoneStatus;
import com.nti.nti_backend.program.ProgramType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectReportService {

    private final ProjectReportRepository reportRepository;
    private final EvaluationRepository evaluationRepository;
    private final MilestoneRepository milestoneRepository;
    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public ProjectReport createReport(Application app) {
        // If report already exists — do not duplicate
        if (reportRepository.findByApplication_Id(app.getId()).isPresent()) {
            return reportRepository.findByApplication_Id(app.getId()).get();
        }

        // Project name from formData
        String projectName = extractProjectName(app.getFormData());

        // Program type
        String programType = app.getCall().getProgram().getType().name();

        // Team leader and members
        String leaderName = app.getApplicant().getName();
        String members = app.getTeamSnapshot().stream()
                .map(m -> m.getEmail() + " (" + m.getRole() + ")")
                .collect(Collectors.joining(", "));

        // Product Owner (Program B only)
        String poName = null;
        if (app.getCall().getProgram().getType() == ProgramType.PROGRAM_B
                && app.getProductOwner() != null) {
            poName = app.getProductOwner().getName();
        }

        // KPI — weighted average score
        Double kpiScore = evaluationRepository
                .findWeightAverageScoreByApplicationId(app.getId())
                .orElse(null);

        // KPI details — scores per criterion
        String kpiDetails = buildKpiDetails(app.getId());

        // Result documents
        String resultDocs = buildResultDocuments(app.getId());

        // Milestones
        var milestones = milestoneRepository.findAllByApplication_Id(app.getId());
        int total = milestones.size();
        int done = (int) milestones.stream()
                .filter(m -> m.getStatus() == MilestoneStatus.COMPLETED)
                .count();

        ProjectReport report = ProjectReport.builder()
                .application(app)
                .projectName(projectName)
                .programType(programType)
                .teamLeaderName(leaderName)
                .teamMembers(members)
                .productOwnerName(poName)
                .completedAt(LocalDateTime.now())
                .kpiScore(kpiScore)
                .kpiDetails(kpiDetails)
                .resultDocuments(resultDocs)
                .milestonesTotal(total)
                .milestonesDone(done)
                .build();

        return reportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public List<ProjectReportDTO> getAllReports() {
        return reportRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectReport getById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found"));
    }

    public String exportToCsv(List<ProjectReportDTO> reports) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Project Name,Program,Team Leader,Product Owner,Completion Date,KPI Score,Milestones (done/total),Members\n");
        for (ProjectReportDTO r : reports) {
            sb.append(csvCell(r.id())).append(",");
            sb.append(csvCell(r.projectName())).append(",");
            sb.append(csvCell(r.programType())).append(",");
            sb.append(csvCell(r.teamLeaderName())).append(",");
            sb.append(csvCell(r.productOwnerName())).append(",");
            sb.append(csvCell(r.completedAt() != null ? r.completedAt().toString() : "")).append(",");
            sb.append(csvCell(r.kpiScore() != null ? String.format("%.2f", r.kpiScore()) : "—")).append(",");
            sb.append(csvCell(r.milestonesDone() + "/" + r.milestonesTotal())).append(",");
            sb.append(csvCell(r.teamMembers())).append("\n");
        }
        return sb.toString();
    }

    // --- helpers ---

    private String extractProjectName(String formData) {
        if (formData == null || formData.isBlank()) return "—";
        try {
            Map<?, ?> map = objectMapper.readValue(formData, Map.class);
            // Search for common project name keys
            for (String key : List.of("projectName", "project_name", "name", "title", "teamName")) {
                Object val = map.get(key);
                if (val != null) return val.toString();
            }
        } catch (Exception ignored) {}
        return "—";
    }

    private String buildKpiDetails(Long appId) {
        var evals = evaluationRepository.findByApplication_Id(appId);
        if (evals.isEmpty()) return null;
        // Group by criterion: name → average score
        Map<String, Double> byCriteria = evals.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCriteria().getName(),
                        Collectors.averagingDouble(e -> e.getScore())
                ));
        try {
            return objectMapper.writeValueAsString(byCriteria);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildResultDocuments(Long appId) {
        var docs = documentRepository.findByApplicationId(appId);
        List<Map<String, String>> result = docs.stream()
                .filter(d -> d.getDocumentType() == DocumentType.RESULT_1
                        || d.getDocumentType() == DocumentType.RESULT_2)
                .map(d -> Map.of(
                        "type", d.getDocumentType().name(),
                        "fileName", d.getFileName() != null ? d.getFileName() : "",
                        "filePath", d.getFilePath() != null ? d.getFilePath() : ""
                ))
                .toList();
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return null;
        }
    }

    private String csvCell(Object value) {
        if (value == null) return "";
        String s = value.toString().replace("\"", "\"\"");
        return "\"" + s + "\"";
    }

    public ProjectReportDTO toDTO(ProjectReport r) {
        return new ProjectReportDTO(
                r.getId(),
                r.getApplication().getId(),
                r.getProjectName(),
                r.getProgramType(),
                r.getTeamLeaderName(),
                r.getTeamMembers(),
                r.getProductOwnerName(),
                r.getCompletedAt(),
                r.getKpiScore(),
                r.getKpiDetails(),
                r.getResultDocuments(),
                r.getMilestonesTotal(),
                r.getMilestonesDone(),
                r.getCreatedAt()
        );
    }
}
