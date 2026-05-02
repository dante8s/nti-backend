package com.nti.nti_backend.reporting;

import com.nti.nti_backend.evaluation.Criteria;
import com.nti.nti_backend.evaluation.CriteriaRepository;
import com.nti.nti_backend.evaluation.Evaluation;
import com.nti.nti_backend.evaluation.EvaluationRepository;
import com.nti.nti_backend.studentProfile.StudentProfileRepository;
import com.nti.nti_backend.team.TeamRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportingService {

    private final ReportingRepository reportingRepository;
    private final CriteriaRepository criteriaRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeamRepository teamRepository;
    private final EvaluationRepository evaluationRepository;

    public ReportingService(ReportingRepository reportingRepository,
                            CriteriaRepository criteriaRepository,
                            StudentProfileRepository studentProfileRepository,
                            TeamRepository teamRepository,
                            EvaluationRepository evaluationRepository) {
        this.reportingRepository = reportingRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.teamRepository = teamRepository;
        this.criteriaRepository = criteriaRepository;
        this.evaluationRepository = evaluationRepository;
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

        stats.put("totalStudentProfiles", studentProfileRepository.count());
        stats.put("profileWithCv", studentProfileRepository.countByCvFilePathIsNotNull());
        stats.put("completeProfiles", studentProfileRepository.countByProfileCompleteTrue());
        stats.put("averageGrade", studentProfileRepository.findAverageProfileGrade());
        stats.put("totalTeams", teamRepository.count());
        stats.put("eligibleTeams", teamRepository.findTeamsWithMinimumSize(3).size());

        return stats;
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
