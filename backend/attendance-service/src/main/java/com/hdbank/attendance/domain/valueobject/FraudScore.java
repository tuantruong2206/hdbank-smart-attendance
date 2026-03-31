package com.hdbank.attendance.domain.valueobject;

import java.util.List;

public record FraudScore(int score, List<String> flags) {
    public boolean isSuspicious() { return score >= 70; }
    public boolean requiresEscalation() { return score >= 90; }
}
