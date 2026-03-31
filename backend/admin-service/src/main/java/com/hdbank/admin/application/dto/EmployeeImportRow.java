package com.hdbank.admin.application.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeImportRow {
    @ExcelProperty("Mã nhân viên")
    private String employeeCode;

    @ExcelProperty("Họ và tên")
    private String fullName;

    @ExcelProperty("Email")
    private String email;

    @ExcelProperty("Số điện thoại")
    private String phone;

    @ExcelProperty("Mã đơn vị")
    private String organizationCode;

    @ExcelProperty("Loại nhân viên")
    private String employeeType; // NGHIEP_VU, IT

    @ExcelProperty("Vai trò")
    private String role;
}
