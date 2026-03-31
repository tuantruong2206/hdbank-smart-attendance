package com.hdbank.attendance.adapter.in.web;

import com.hdbank.attendance.application.port.in.QrCodeUseCase;
import com.hdbank.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attendance/qr")
@RequiredArgsConstructor
public class QrCodeController {

    private final QrCodeUseCase qrCodeUseCase;

    /**
     * Generate a dynamic QR code for a location.
     * Typically called by admin or location manager.
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<QrCodeUseCase.QrCodeResult>> generate(
            @RequestBody GenerateQrRequest request) {
        QrCodeUseCase.QrCodeResult result = qrCodeUseCase.generateQrCode(request.locationId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Validate a QR code token. Returns the locationId if valid.
     * Called during check-in when employee scans a QR code.
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<ValidateQrResponse>> validate(
            @RequestBody ValidateQrRequest request) {
        UUID locationId = qrCodeUseCase.validateQrCode(request.token());
        return ResponseEntity.ok(ApiResponse.success(new ValidateQrResponse(locationId, true)));
    }

    public record GenerateQrRequest(UUID locationId) {}
    public record ValidateQrRequest(String token) {}
    public record ValidateQrResponse(UUID locationId, boolean valid) {}
}
