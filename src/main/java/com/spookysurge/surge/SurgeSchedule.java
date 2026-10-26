package com.spookysurge.surge;

import com.spookysurge.config.SurgeConfig;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;

/** Decides which scheduled surge, if any, is due right now. Pure, so the clock logic is unit tested. */
public final class SurgeSchedule {
    private SurgeSchedule() {}

    /** A slot that should be running. {@code key} identifies it so a restart does not start it twice. */
    public record Due(String key, Instant start) {}

    public static Optional<Due> due(Instant now, SurgeConfig config, Set<String> alreadyStarted) {
        Instant windowStart = parse(config.windowStart);
        Instant windowEnd = parse(config.windowEnd);
        if (windowStart == null || windowEnd == null || now.isBefore(windowStart) || !now.isBefore(windowEnd)) {
            return Optional.empty();
        }
        ZoneId zone = zone(config.timezone);
        LocalDate today = now.atZone(zone).toLocalDate();
        Duration lead = Duration.ofSeconds(config.countdownSeconds);
        Duration length = Duration.ofMinutes(config.durationMinutes);
        Due best = null;
        for (LocalDate day : new LocalDate[] {today.minusDays(1), today, today.plusDays(1)}) {
            for (String time : config.startTimes) {
                LocalTime local;
                try {
                    local = LocalTime.parse(time);
                } catch (DateTimeException e) {
                    continue;
                }
                Instant start = ZonedDateTime.of(day, local, zone).toInstant();
                String key = day + "@" + time;
                if (alreadyStarted.contains(key)) continue;
                boolean inRange = !now.isBefore(start.minus(lead)) && now.isBefore(start.plus(length));
                if (!inRange) continue;
                if (start.isBefore(windowStart) || !start.isBefore(windowEnd)) continue;
                if (best == null || start.isBefore(best.start())) best = new Due(key, start);
            }
        }
        return Optional.ofNullable(best);
    }

    static Instant parse(String text) {
        try {
            return Instant.parse(text);
        } catch (RuntimeException e) {
            return null;
        }
    }

    static ZoneId zone(String id) {
        try {
            return ZoneId.of(id);
        } catch (RuntimeException e) {
            return ZoneId.of("UTC");
        }
    }
}
