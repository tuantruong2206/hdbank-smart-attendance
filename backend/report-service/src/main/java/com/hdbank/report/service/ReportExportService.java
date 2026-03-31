package com.hdbank.report.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.hdbank.report.entity.AttendanceRecordView;
import com.hdbank.report.entity.GeneratedReportJpaEntity;
import com.hdbank.report.repository.AttendanceRecordViewRepository;
import com.hdbank.report.repository.GeneratedReportRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportExportService {

    private final AttendanceRecordViewRepository attendanceRecordRepository;
    private final GeneratedReportRepository generatedReportRepository;
    private final MinioClient minioClient;

    @Value("${minio.bucket:reports}")
    private String minioBucket;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Generate attendance Excel report asynchronously.
     * Queries attendance_records, writes to Excel using EasyExcel, uploads to MinIO.
     */
    @Async
    public void generateAttendanceExcel(UUID reportId, UUID orgId, LocalDate dateFrom, LocalDate dateTo) {
        GeneratedReportJpaEntity report = generatedReportRepository.findById(reportId).orElse(null);
        if (report == null) return;

        try {
            report.setStatus("GENERATING");
            generatedReportRepository.save(report);

            Instant from = dateFrom.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant to = dateTo.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

            List<AttendanceRecordView> records = attendanceRecordRepository.findByTimeRange(from, to);

            // Convert to Excel data
            List<List<String>> data = new ArrayList<>();
            data.add(List.of("Mã NV", "Loại", "Thời gian", "Trạng thái", "Điểm rủi ro", "Ghi chú"));

            for (AttendanceRecordView record : records) {
                String checkTime = record.getCheckTime()
                        .atZone(ZoneId.systemDefault())
                        .format(DATETIME_FMT);
                data.add(List.of(
                        record.getEmployeeCode() != null ? record.getEmployeeCode() : "",
                        record.getCheckType() != null ? record.getCheckType() : "",
                        checkTime,
                        record.getStatus() != null ? record.getStatus() : "",
                        String.valueOf(record.getFraudScore()),
                        record.getNotes() != null ? record.getNotes() : ""
                ));
            }

            // Write Excel
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            EasyExcel.write(outputStream)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .sheet("Attendance Report")
                    .doWrite(data);

            byte[] excelBytes = outputStream.toByteArray();
            String fileName = "attendance_" + dateFrom.format(DATE_FMT) + "_" + dateTo.format(DATE_FMT) + ".xlsx";
            String objectKey = "attendance/" + UUID.randomUUID() + "/" + fileName;

            // Upload to MinIO
            uploadToMinio(objectKey, excelBytes, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            // Generate presigned URL
            String downloadUrl = getPresignedUrl(objectKey);

            report.setStatus("COMPLETED");
            report.setFileName(fileName);
            report.setFileUrl(downloadUrl);
            report.setMinioObjectKey(objectKey);
            report.setFileSizeBytes((long) excelBytes.length);
            report.setCompletedAt(Instant.now());
            generatedReportRepository.save(report);

            log.info("Attendance Excel report generated: {}", fileName);

        } catch (Exception e) {
            log.error("Failed to generate attendance Excel: {}", e.getMessage(), e);
            report.setStatus("FAILED");
            report.setErrorMessage(e.getMessage());
            generatedReportRepository.save(report);
        }
    }

    /**
     * Generate timesheet PDF for a specific employee and month.
     */
    @Async
    public void generateTimesheetPdf(UUID reportId, UUID employeeId, int month, int year) {
        GeneratedReportJpaEntity report = generatedReportRepository.findById(reportId).orElse(null);
        if (report == null) return;

        try {
            report.setStatus("GENERATING");
            generatedReportRepository.save(report);

            LocalDate monthStart = LocalDate.of(year, month, 1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

            Instant from = monthStart.atStartOfDay(ZoneId.systemDefault()).toInstant();
            Instant to = monthEnd.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

            List<AttendanceRecordView> records = attendanceRecordRepository
                    .findByEmployeeAndTimeRange(employeeId, from, to);

            // Generate PDF using OpenPDF
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Title
            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Paragraph title = new Paragraph("Bang Cham Cong Thang " + month + "/" + year, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" ")); // spacer

            // Table
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2, 2, 3, 2, 3});

            // Headers
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);
            String[] headers = {"Ma NV", "Loai", "Thoi gian", "Trang thai", "Ghi chu"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
                table.addCell(cell);
            }

            // Data rows
            Font dataFont = new Font(Font.HELVETICA, 9);
            for (AttendanceRecordView record : records) {
                table.addCell(new Phrase(record.getEmployeeCode() != null ? record.getEmployeeCode() : "", dataFont));
                table.addCell(new Phrase(record.getCheckType() != null ? record.getCheckType() : "", dataFont));
                table.addCell(new Phrase(
                        record.getCheckTime().atZone(ZoneId.systemDefault()).format(DATETIME_FMT), dataFont));
                table.addCell(new Phrase(record.getStatus() != null ? record.getStatus() : "", dataFont));
                table.addCell(new Phrase(record.getNotes() != null ? record.getNotes() : "", dataFont));
            }

            document.add(table);

            // Summary
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Tong so ban ghi: " + records.size(), dataFont));

            document.close();

            byte[] pdfBytes = outputStream.toByteArray();
            String fileName = "timesheet_" + employeeId + "_" + year + "-" + String.format("%02d", month) + ".pdf";
            String objectKey = "timesheet/" + UUID.randomUUID() + "/" + fileName;

            // Upload to MinIO
            uploadToMinio(objectKey, pdfBytes, "application/pdf");

            String downloadUrl = getPresignedUrl(objectKey);

            report.setStatus("COMPLETED");
            report.setFileName(fileName);
            report.setFileUrl(downloadUrl);
            report.setMinioObjectKey(objectKey);
            report.setFileSizeBytes((long) pdfBytes.length);
            report.setCompletedAt(Instant.now());
            generatedReportRepository.save(report);

            log.info("Timesheet PDF report generated: {}", fileName);

        } catch (Exception e) {
            log.error("Failed to generate timesheet PDF: {}", e.getMessage(), e);
            report.setStatus("FAILED");
            report.setErrorMessage(e.getMessage());
            generatedReportRepository.save(report);
        }
    }

    @CircuitBreaker(name = "minio-upload", fallbackMethod = "uploadToMinioFallback")
    @Retry(name = "minio-upload")
    protected void uploadToMinio(String objectKey, byte[] data, String contentType) throws Exception {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(minioBucket)
                .object(objectKey)
                .stream(new ByteArrayInputStream(data), data.length, -1)
                .contentType(contentType)
                .build());
    }

    @CircuitBreaker(name = "minio-upload", fallbackMethod = "getPresignedUrlFallback")
    @Retry(name = "minio-upload")
    protected String getPresignedUrl(String objectKey) throws Exception {
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .bucket(minioBucket)
                .object(objectKey)
                .method(Method.GET)
                .expiry(24 * 60 * 60) // 24 hours
                .build());
    }

    /**
     * Fallback when MinIO upload circuit breaker is open or retries exhausted.
     */
    @SuppressWarnings("unused")
    private void uploadToMinioFallback(String objectKey, byte[] data, String contentType, Throwable t) throws Exception {
        log.error("Circuit breaker fallback for MinIO upload of {}: {} - {}",
                objectKey, t.getClass().getSimpleName(), t.getMessage());
        throw new RuntimeException("MinIO upload unavailable: " + t.getMessage(), t);
    }

    /**
     * Fallback when MinIO presigned URL circuit breaker is open or retries exhausted.
     */
    @SuppressWarnings("unused")
    private String getPresignedUrlFallback(String objectKey, Throwable t) throws Exception {
        log.error("Circuit breaker fallback for MinIO presigned URL of {}: {} - {}",
                objectKey, t.getClass().getSimpleName(), t.getMessage());
        throw new RuntimeException("MinIO presigned URL unavailable: " + t.getMessage(), t);
    }
}
