package com.hdbank.admin.adapter.in.web;

import com.hdbank.admin.adapter.out.persistence.entity.WifiAccessPointJpaEntity;
import com.hdbank.admin.adapter.out.persistence.repository.WifiAccessPointJpaRepository;
import com.hdbank.admin.application.dto.WifiSurveyRequest;
import com.hdbank.common.dto.ApiResponse;
import com.hdbank.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/wifi-aps")
@RequiredArgsConstructor
public class WifiAccessPointController {

    private final WifiAccessPointJpaRepository wifiRepo;

    @GetMapping("/location/{locationId}")
    public ResponseEntity<ApiResponse<List<WifiAccessPointJpaEntity>>> getByLocation(
            @PathVariable UUID locationId) {
        return ResponseEntity.ok(ApiResponse.success(
                wifiRepo.findByLocationIdAndIsActiveTrue(locationId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WifiAccessPointJpaEntity>> create(
            @RequestBody WifiAccessPointJpaEntity ap) {
        return ResponseEntity.ok(ApiResponse.success(wifiRepo.save(ap)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WifiAccessPointJpaEntity>> update(
            @PathVariable UUID id, @RequestBody WifiAccessPointJpaEntity ap) {
        if (!wifiRepo.existsById(id)) {
            throw new ResourceNotFoundException("WifiAccessPoint", id.toString());
        }
        ap.setId(id);
        return ResponseEntity.ok(ApiResponse.success(wifiRepo.save(ap)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        WifiAccessPointJpaEntity ap = wifiRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WifiAccessPoint", id.toString()));
        ap.setActive(false);
        wifiRepo.save(ap);
        return ResponseEntity.ok(ApiResponse.success("Đã vô hiệu hóa WiFi AP", null));
    }

    /**
     * WiFi survey: save multiple access points at once for a location.
     */
    @PostMapping("/survey")
    public ResponseEntity<ApiResponse<List<WifiAccessPointJpaEntity>>> survey(
            @RequestBody WifiSurveyRequest request) {
        List<WifiAccessPointJpaEntity> entities = new ArrayList<>();
        for (WifiSurveyRequest.AccessPointEntry entry : request.getAccessPoints()) {
            WifiAccessPointJpaEntity ap = WifiAccessPointJpaEntity.builder()
                    .bssid(entry.getBssid())
                    .ssid(entry.getSsid())
                    .locationId(request.getLocationId())
                    .floor(entry.getFloor())
                    .signalStrengthThreshold(entry.getSignalStrengthThreshold())
                    .isActive(true)
                    .build();
            entities.add(ap);
        }
        List<WifiAccessPointJpaEntity> saved = wifiRepo.saveAll(entities);
        return ResponseEntity.ok(ApiResponse.success("WiFi survey hoàn thành", saved));
    }
}
