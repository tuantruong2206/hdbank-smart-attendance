package com.hdbank.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtils {
    public static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final DateTimeFormatter VN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter VN_DATETIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    public static final DateTimeFormatter VN_TIME = DateTimeFormatter.ofPattern("HH:mm");

    private DateTimeUtils() {}

    public static LocalDateTime nowVN() {
        return LocalDateTime.now(VN_ZONE);
    }

    public static LocalDate todayVN() {
        return LocalDate.now(VN_ZONE);
    }

    public static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime.atZone(VN_ZONE).toInstant();
    }

    public static LocalDateTime toVNTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, VN_ZONE);
    }
}
