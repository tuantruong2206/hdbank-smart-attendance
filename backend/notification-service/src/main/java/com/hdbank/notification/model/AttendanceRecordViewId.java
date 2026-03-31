package com.hdbank.notification.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecordViewId implements Serializable {
    private UUID id;
    private Instant checkTime;
}
