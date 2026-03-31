package com.hdbank.admin.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportValidationResult {
    private int totalRows;
    private int validRows;
    private int errorRows;
    private List<RowError> errors;
    private List<DryRunRow> preview;
    private UUID batchId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowError {
        private int rowNumber;
        private String field;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DryRunRow {
        private int rowNumber;
        private String employeeCode;
        private String fullName;
        private String email;
        private String action; // INSERT, UPDATE, SKIP
        private String reason;
    }
}
