package com.hdbank.admin.adapter.in.web;

import com.hdbank.admin.application.dto.ImportValidationResult;
import com.hdbank.admin.application.service.ExcelImportService;
import com.hdbank.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/import")
@RequiredArgsConstructor
public class ImportController {

    private final ExcelImportService excelImportService;

    /**
     * Download Excel template for employee import.
     */
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] template = excelImportService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=employee_import_template.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(template);
    }

    /**
     * Upload and validate an Excel file. Returns validation errors per row.
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<ImportValidationResult>> upload(
            @RequestParam("file") MultipartFile file) throws IOException {
        ImportValidationResult result = excelImportService.validateFile(file);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Preview what the import would do (insert/update/skip counts).
     */
    @PostMapping("/dry-run")
    public ResponseEntity<ApiResponse<ImportValidationResult>> dryRun(
            @RequestParam("file") MultipartFile file) throws IOException {
        ImportValidationResult result = excelImportService.dryRun(file);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Execute the import with the specified mode (INSERT, UPDATE, UPSERT).
     * All records are tagged with a batchId for rollback support.
     */
    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<ImportValidationResult>> execute(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "UPSERT") String mode,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) throws IOException {
        UUID importedBy = userId != null ? userId : UUID.randomUUID();
        ImportValidationResult result = excelImportService.execute(file, mode, importedBy);
        return ResponseEntity.ok(ApiResponse.success("Import thành công", result));
    }

    /**
     * Rollback an import batch within 24 hours.
     */
    @PostMapping("/rollback/{batchId}")
    public ResponseEntity<ApiResponse<Void>> rollback(@PathVariable UUID batchId) {
        excelImportService.rollback(batchId);
        return ResponseEntity.ok(ApiResponse.success("Rollback thành công", null));
    }
}
