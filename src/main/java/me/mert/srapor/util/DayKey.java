package me.mert.srapor.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DayKey {

    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private DayKey() {}

    public static String today(ZoneId zoneId) {
        return LocalDate.now(zoneId).format(F);
    }
}
