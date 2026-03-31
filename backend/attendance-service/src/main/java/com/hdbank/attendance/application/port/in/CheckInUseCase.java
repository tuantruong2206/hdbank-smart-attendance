package com.hdbank.attendance.application.port.in;

import com.hdbank.attendance.application.dto.CheckInCommand;
import com.hdbank.attendance.application.dto.CheckInResult;

public interface CheckInUseCase {
    CheckInResult checkIn(CheckInCommand command);
}
