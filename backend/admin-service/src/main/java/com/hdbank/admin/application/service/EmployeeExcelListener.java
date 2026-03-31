package com.hdbank.admin.application.service;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.hdbank.admin.application.dto.EmployeeImportRow;
import com.hdbank.admin.application.dto.ImportValidationResult;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Getter
public class EmployeeExcelListener implements ReadListener<EmployeeImportRow> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final int BATCH_SIZE = 500;

    private final List<EmployeeImportRow> validRows = new ArrayList<>();
    private final List<ImportValidationResult.RowError> errors = new ArrayList<>();
    private final Set<String> existingOrgCodes;
    private final Set<String> seenEmployeeCodes = new HashSet<>();
    private final Set<String> seenEmails = new HashSet<>();
    private int currentRow = 0;

    public EmployeeExcelListener(Set<String> existingOrgCodes) {
        this.existingOrgCodes = existingOrgCodes;
    }

    @Override
    public void invoke(EmployeeImportRow row, AnalysisContext context) {
        currentRow++;
        boolean rowValid = true;

        // Required field: employeeCode
        if (row.getEmployeeCode() == null || row.getEmployeeCode().isBlank()) {
            errors.add(buildError(currentRow, "employeeCode", "Mã nhân viên không được để trống"));
            rowValid = false;
        } else if (!seenEmployeeCodes.add(row.getEmployeeCode().trim())) {
            errors.add(buildError(currentRow, "employeeCode",
                    "Mã nhân viên trùng lặp trong file: " + row.getEmployeeCode()));
            rowValid = false;
        }

        // Required field: fullName
        if (row.getFullName() == null || row.getFullName().isBlank()) {
            errors.add(buildError(currentRow, "fullName", "Họ và tên không được để trống"));
            rowValid = false;
        }

        // Required field: email with format validation
        if (row.getEmail() == null || row.getEmail().isBlank()) {
            errors.add(buildError(currentRow, "email", "Email không được để trống"));
            rowValid = false;
        } else if (!EMAIL_PATTERN.matcher(row.getEmail().trim()).matches()) {
            errors.add(buildError(currentRow, "email", "Email không đúng định dạng: " + row.getEmail()));
            rowValid = false;
        } else if (!seenEmails.add(row.getEmail().trim().toLowerCase())) {
            errors.add(buildError(currentRow, "email",
                    "Email trùng lặp trong file: " + row.getEmail()));
            rowValid = false;
        }

        // Required field: organizationCode must exist
        if (row.getOrganizationCode() == null || row.getOrganizationCode().isBlank()) {
            errors.add(buildError(currentRow, "organizationCode", "Mã đơn vị không được để trống"));
            rowValid = false;
        } else if (!existingOrgCodes.contains(row.getOrganizationCode().trim())) {
            errors.add(buildError(currentRow, "organizationCode",
                    "Mã đơn vị không tồn tại: " + row.getOrganizationCode()));
            rowValid = false;
        }

        // Required field: employeeType
        if (row.getEmployeeType() == null || row.getEmployeeType().isBlank()) {
            errors.add(buildError(currentRow, "employeeType", "Loại nhân viên không được để trống"));
            rowValid = false;
        } else {
            String type = row.getEmployeeType().trim().toUpperCase();
            if (!type.equals("NGHIEP_VU") && !type.equals("IT")) {
                errors.add(buildError(currentRow, "employeeType",
                        "Loại nhân viên không hợp lệ (NGHIEP_VU hoặc IT): " + row.getEmployeeType()));
                rowValid = false;
            }
        }

        // Required field: role
        if (row.getRole() == null || row.getRole().isBlank()) {
            errors.add(buildError(currentRow, "role", "Vai trò không được để trống"));
            rowValid = false;
        }

        if (rowValid) {
            validRows.add(row);
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // Processing complete
    }

    private ImportValidationResult.RowError buildError(int rowNumber, String field, String message) {
        return ImportValidationResult.RowError.builder()
                .rowNumber(rowNumber)
                .field(field)
                .message(message)
                .build();
    }
}
