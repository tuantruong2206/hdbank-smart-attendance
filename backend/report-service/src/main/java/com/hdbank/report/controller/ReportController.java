package com.hdbank.report.controller;

import com.hdbank.common.dto.ApiResponse;
import com.hdbank.report.entity.GeneratedReportJpaEntity;
import com.hdbank.report.repository.GeneratedReportRepository;
import com.hdbank.report.service.ReportExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportExportService reportExportService;
    private final GeneratedReportRepository generatedReportRepository;

    /**
     * POST /api/v1/reports/generate — generate report (async, returns job ID).
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateReport(
            @RequestBody GenerateReportRequest request) {

        // Create the report record
        GeneratedReportJpaEntity report = GeneratedReportJpaEntity.builder()
                .reportType(request.reportType())
                .organizationId(request.organizationId())
                .employeeId(request.employeeId())
                .dateFrom(request.dateFrom())
                .dateTo(request.dateTo())
                .status("PENDING")
                .requestedBy(request.requestedBy())
                .build();

        report = generatedReportRepository.save(report);

        // Dispatch async generation
        switch (request.reportType()) {
            case "ATTENDANCE_EXCEL" ->
                    reportExportService.generateAttendanceExcel(
                            report.getId(), request.organizationId(),
                            request.dateFrom(), request.dateTo());
            case "TIMESHEET_PDF" -> {
                int month = request.dateFrom() != null ? request.dateFrom().getMonthValue() : LocalDate.now().getMonthValue();
                int year = request.dateFrom() != null ? request.dateFrom().getYear() : LocalDate.now().getYear();
                reportExportService.generateTimesheetPdf(
                        report.getId(), request.employeeId(), month, year);
            }
            default -> {
                report.setStatus("FAILED");
                report.setErrorMessage("Unknown report type: " + request.reportType());
                generatedReportRepository.save(report);
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Unknown report type: " + request.reportType()));
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("reportId", report.getId());
        response.put("status", report.getStatus());
        response.put("message", "Report generation started");

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/reports/{id}/download — download generated report from MinIO.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReportDownload(@PathVariable UUID id) {
        GeneratedReportJpaEntity report = generatedReportRepository.findById(id).orElse(null);

        if (report == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("reportId", report.getId());
        response.put("status", report.getStatus());
        response.put("reportType", report.getReportType());
        response.put("fileName", report.getFileName());

        if ("COMPLETED".equals(report.getStatus())) {
            response.put("downloadUrl", report.getFileUrl());
            response.put("fileSizeBytes", report.getFileSizeBytes());
        } else if ("FAILED".equals(report.getStatus())) {
            response.put("errorMessage", report.getErrorMessage());
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/reports/list — list generated reports.
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<Page<GeneratedReportJpaEntity>>> listReports(
            @RequestParam(required = false) UUID requestedBy,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<GeneratedReportJpaEntity> reports;
        if (requestedBy != null) {
            reports = generatedReportRepository.findByRequestedByOrderByCreatedAtDesc(requestedBy, pageRequest);
        } else if (organizationId != null) {
            reports = generatedReportRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId, pageRequest);
        } else {
            reports = generatedReportRepository.findAll(pageRequest);
        }

        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    record GenerateReportRequest(
            String reportType,
            UUID organizationId,
            UUID employeeId,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            UUID requestedBy
    ) {}
}
