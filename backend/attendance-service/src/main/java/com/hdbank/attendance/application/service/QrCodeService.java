package com.hdbank.attendance.application.service;

import com.hdbank.attendance.application.port.in.QrCodeUseCase;
import com.hdbank.attendance.application.port.out.QrCodeCachePort;
import com.hdbank.attendance.domain.service.QrCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrCodeService implements QrCodeUseCase {

    private final QrCodeGenerator qrCodeGenerator;
    private final QrCodeCachePort qrCodeCachePort;

    @Override
    public QrCodeResult generateQrCode(UUID locationId) {
        QrCodeGenerator.QrToken qrToken = qrCodeGenerator.generate(locationId);

        // Store in Redis with TTL
        qrCodeCachePort.store(qrToken.token(), qrToken.locationId(), qrToken.ttlSeconds());

        log.info("Generated QR code for location {} with TTL {}s", locationId, qrToken.ttlSeconds());

        return new QrCodeResult(
                qrToken.token(),
                qrToken.locationId(),
                qrToken.expiresAt(),
                qrToken.ttlSeconds()
        );
    }

    @Override
    public UUID validateQrCode(String token) {
        return qrCodeCachePort.validateAndConsume(token)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Mã QR không hợp lệ hoặc đã hết hạn"));
    }
}
