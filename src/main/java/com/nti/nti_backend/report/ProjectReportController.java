package com.nti.nti_backend.report;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class ProjectReportController {

    private final ProjectReportService reportService;

    @GetMapping
    public ResponseEntity<List<ProjectReportDTO>> getAll() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportOne(@PathVariable Long id) {
        ProjectReport report = reportService.getById(id);
        String csv = reportService.exportToCsv(List.of(reportService.toDTO(report)));
        return buildCsvResponse(csv, "report_" + id + ".csv");
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAll() {
        List<ProjectReportDTO> all = reportService.getAllReports();
        String csv = reportService.exportToCsv(all);
        return buildCsvResponse(csv, "all_reports.csv");
    }

    private ResponseEntity<byte[]> buildCsvResponse(String csv, String filename) {
        byte[] bytes = ("﻿" + csv).getBytes(StandardCharsets.UTF_8); // BOM для Excel
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }
}
