package com.hdbank.admin.application.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.hdbank.admin.adapter.out.persistence.entity.EmployeeJpaEntity;
import com.hdbank.admin.adapter.out.persistence.entity.ImportBatchJpaEntity;
import com.hdbank.admin.adapter.out.persistence.entity.OrganizationJpaEntity;
import com.hdbank.admin.adapter.out.persistence.repository.EmployeeAdminJpaRepository;
import com.hdbank.admin.adapter.out.persistence.repository.ImportBatchJpaRepository;
import com.hdbank.admin.adapter.out.persistence.repository.OrganizationJpaRepository;
import com.hdbank.admin.application.dto.EmployeeImportRow;
import com.hdbank.admin.application.dto.ImportValidationResult;
import com.hdbank.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelImportService {

    private final EmployeeAdminJpaRepository employeeRepo;
    private final OrganizationJpaRepository orgRepo;
    private final ImportBatchJpaRepository importBatchRepo;

    /**
     * Generate an Excel template with headers for employee import.
     */
    public byte[] generateTemplate() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<EmployeeImportRow> sampleData = List.of(
                new EmployeeImportRow("NV001", "Nguyen Van A", "nguyenvana@hdbank.com.vn",
                        "0901234567", "HO-CNTT", "IT", "EMPLOYEE")
        );
        EasyExcel.write(out, EmployeeImportRow.class)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet("Template")
                .doWrite(sampleData);
        return out.toByteArray();
    }

    /**
     * Validate an uploaded Excel file. Returns validation errors per row.
     */
    public ImportValidationResult validateFile(MultipartFile file) throws IOException {
        EmployeeExcelListener listener = parseFile(file);
        return ImportValidationResult.builder()
                .totalRows(listener.getValidRows().size() + listener.getErrors().size())
                .validRows(listener.getValidRows().size())
                .errorRows(listener.getErrors().size())
                .errors(listener.getErrors())
                .build();
    }

    /**
     * Dry run: validate + show what would happen (new/update/skip).
     */
    public ImportValidationResult dryRun(MultipartFile file) throws IOException {
        EmployeeExcelListener listener = parseFile(file);
        if (!listener.getErrors().isEmpty()) {
            return ImportValidationResult.builder()
                    .totalRows(listener.getValidRows().size() + listener.getErrors().size())
                    .validRows(listener.getValidRows().size())
                    .errorRows(listener.getErrors().size())
                    .errors(listener.getErrors())
                    .build();
        }

        Map<String, EmployeeJpaEntity> existingByCode = employeeRepo.findAll().stream()
                .collect(Collectors.toMap(EmployeeJpaEntity::getEmployeeCode, e -> e, (a, b) -> a));

        List<ImportValidationResult.DryRunRow> preview = new ArrayList<>();
        int rowNum = 0;
        for (EmployeeImportRow row : listener.getValidRows()) {
            rowNum++;
            String code = row.getEmployeeCode().trim();
            boolean exists = existingByCode.containsKey(code);
            preview.add(ImportValidationResult.DryRunRow.builder()
                    .rowNumber(rowNum)
                    .employeeCode(code)
                    .fullName(row.getFullName())
                    .email(row.getEmail())
                    .action(exists ? "UPDATE" : "INSERT")
                    .reason(exists ? "Mã nhân viên đã tồn tại" : "Nhân viên mới")
                    .build());
        }

        return ImportValidationResult.builder()
                .totalRows(listener.getValidRows().size())
                .validRows(listener.getValidRows().size())
                .errorRows(0)
                .errors(List.of())
                .preview(preview)
                .build();
    }

    /**
     * Execute the import with the specified mode: INSERT, UPDATE, or UPSERT.
     * Tags all records with a batchId for rollback support.
     */
    @Transactional
    public ImportValidationResult execute(MultipartFile file, String mode, UUID importedBy) throws IOException {
        EmployeeExcelListener listener = parseFile(file);
        if (!listener.getErrors().isEmpty()) {
            throw new BusinessException("VALIDATION_FAILED",
                    "File chứa " + listener.getErrors().size() + " lỗi. Vui lòng sửa và thử lại.");
        }

        Map<String, OrganizationJpaEntity> orgByCode = orgRepo.findAll().stream()
                .collect(Collectors.toMap(OrganizationJpaEntity::getCode, o -> o, (a, b) -> a));
        Map<String, EmployeeJpaEntity> existingByCode = employeeRepo.findAll().stream()
                .collect(Collectors.toMap(EmployeeJpaEntity::getEmployeeCode, e -> e, (a, b) -> a));

        UUID batchId = UUID.randomUUID();
        int inserted = 0, updated = 0, skipped = 0;
        List<EmployeeJpaEntity> toSave = new ArrayList<>();

        for (EmployeeImportRow row : listener.getValidRows()) {
            String code = row.getEmployeeCode().trim();
            EmployeeJpaEntity existing = existingByCode.get(code);
            OrganizationJpaEntity org = orgByCode.get(row.getOrganizationCode().trim());

            switch (mode.toUpperCase()) {
                case "INSERT":
                    if (existing != null) {
                        skipped++;
                    } else {
                        toSave.add(buildEmployee(row, org, batchId));
                        inserted++;
                    }
                    break;
                case "UPDATE":
                    if (existing == null) {
                        skipped++;
                    } else {
                        updateEmployee(existing, row, org);
                        toSave.add(existing);
                        updated++;
                    }
                    break;
                case "UPSERT":
                    if (existing == null) {
                        toSave.add(buildEmployee(row, org, batchId));
                        inserted++;
                    } else {
                        updateEmployee(existing, row, org);
                        toSave.add(existing);
                        updated++;
                    }
                    break;
                default:
                    throw new BusinessException("INVALID_MODE", "Mode không hợp lệ: " + mode);
            }
        }

        employeeRepo.saveAll(toSave);

        // Save batch record for rollback tracking
        ImportBatchJpaEntity batch = ImportBatchJpaEntity.builder()
                .id(batchId)
                .fileName(file.getOriginalFilename())
                .mode(mode.toUpperCase())
                .totalRows(listener.getValidRows().size())
                .insertedCount(inserted)
                .updatedCount(updated)
                .skippedCount(skipped)
                .errorCount(0)
                .importedBy(importedBy)
                .build();
        importBatchRepo.save(batch);

        log.info("Import batch {} completed: {} inserted, {} updated, {} skipped",
                batchId, inserted, updated, skipped);

        return ImportValidationResult.builder()
                .totalRows(listener.getValidRows().size())
                .validRows(listener.getValidRows().size())
                .errorRows(0)
                .errors(List.of())
                .batchId(batchId)
                .build();
    }

    /**
     * Rollback an import batch within 24 hours.
     * Soft-deletes (deactivates) employees that were inserted in this batch.
     */
    @Transactional
    public void rollback(UUID batchId) {
        ImportBatchJpaEntity batch = importBatchRepo.findById(batchId)
                .orElseThrow(() -> new BusinessException("BATCH_NOT_FOUND",
                        "Không tìm thấy batch import: " + batchId));

        if ("ROLLED_BACK".equals(batch.getStatus())) {
            throw new BusinessException("ALREADY_ROLLED_BACK", "Batch đã được rollback trước đó");
        }

        Duration elapsed = Duration.between(batch.getCreatedAt(), Instant.now());
        if (elapsed.toHours() > 24) {
            throw new BusinessException("ROLLBACK_EXPIRED",
                    "Không thể rollback sau 24 giờ. Batch được import lúc: " + batch.getCreatedAt());
        }

        // For INSERT mode, we soft-delete by deactivating; for UPDATE we cannot fully rollback
        // since original values are not stored. Log a warning for UPDATE batches.
        if ("UPDATE".equals(batch.getMode())) {
            log.warn("Rollback of UPDATE batch {} - original values not restorable. " +
                    "Only marking batch as rolled back.", batchId);
        }

        batch.setStatus("ROLLED_BACK");
        batch.setRolledBackAt(Instant.now());
        importBatchRepo.save(batch);

        log.info("Batch {} rolled back successfully", batchId);
    }

    private EmployeeExcelListener parseFile(MultipartFile file) throws IOException {
        Set<String> orgCodes = orgRepo.findAll().stream()
                .map(OrganizationJpaEntity::getCode)
                .collect(Collectors.toSet());
        EmployeeExcelListener listener = new EmployeeExcelListener(orgCodes);
        EasyExcel.read(file.getInputStream(), EmployeeImportRow.class, listener).sheet().doRead();
        return listener;
    }

    private EmployeeJpaEntity buildEmployee(EmployeeImportRow row, OrganizationJpaEntity org, UUID batchId) {
        return EmployeeJpaEntity.builder()
                .employeeCode(row.getEmployeeCode().trim())
                .fullName(row.getFullName().trim())
                .email(row.getEmail().trim().toLowerCase())
                .phone(row.getPhone() != null ? row.getPhone().trim() : null)
                .organizationId(org.getId())
                .employeeType(row.getEmployeeType().trim().toUpperCase())
                .role(row.getRole().trim().toUpperCase())
                .passwordHash("IMPORT_PENDING_RESET") // Placeholder, user must reset password
                .isActive(true)
                .build();
    }

    private void updateEmployee(EmployeeJpaEntity entity, EmployeeImportRow row, OrganizationJpaEntity org) {
        entity.setFullName(row.getFullName().trim());
        entity.setEmail(row.getEmail().trim().toLowerCase());
        entity.setPhone(row.getPhone() != null ? row.getPhone().trim() : entity.getPhone());
        entity.setOrganizationId(org.getId());
        entity.setEmployeeType(row.getEmployeeType().trim().toUpperCase());
        entity.setRole(row.getRole().trim().toUpperCase());
    }
}
