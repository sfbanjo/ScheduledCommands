package io.github.sfbanjo.scheduledcommands;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeParser {

    private static final Pattern FULL_DURATION = Pattern.compile("^(?:\\d+\\s*[dhms]\\s*)+$");
    private static final Pattern DURATION_PART = Pattern.compile("(\\d+)\\s*([dhms])");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm[:ss]");

    private TimeParser() {
    }

    public static ZoneId parseZoneId(String raw) {
        if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("SYSTEM")) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(raw.trim());
        } catch (Exception e) {
            return null;
        }
    }

    public static LocalTime parseTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(raw.trim(), TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static Duration parseDuration(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String clean = raw.trim().toLowerCase(Locale.ROOT);
        if (!FULL_DURATION.matcher(clean).matches()) {
            return null;
        }

        long totalSeconds = 0;
        Matcher matcher = DURATION_PART.matcher(clean);
        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            switch (unit) {
                case "d" -> totalSeconds += value * 86400L;
                case "h" -> totalSeconds += value * 3600L;
                case "m" -> totalSeconds += value * 60L;
                case "s" -> totalSeconds += value;
                default -> { }
            }
        }

        return (totalSeconds > 0) ? Duration.ofSeconds(totalSeconds) : null;
    }

    public static Set<DayOfWeek> parseDays(List<String> rawDays) {
        if (rawDays == null || rawDays.isEmpty()) {
            return EnumSet.allOf(DayOfWeek.class);
        }
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (String raw : rawDays) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                days.add(DayOfWeek.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                // Giorno non valido: lo segnaleremo durante il caricamento
            }
        }
        return days;
    }

    public static String formatDuration(Duration duration) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            return "0s";
        }
        long totalSeconds = duration.getSeconds();
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");
        return sb.toString().trim();
    }
}