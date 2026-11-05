package com.pocketledger.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;

public class DateUtilsTest {

    @Test
    public void computesWeekBoundsFromMondayToSunday() {
        LocalDate wednesday = LocalDate.of(2026, 11, 4);
        assertEquals(LocalDate.of(2026, 11, 2), DateUtils.startOfWeek(wednesday));
        assertEquals(LocalDate.of(2026, 11, 8), DateUtils.endOfWeek(wednesday));
        assertEquals(7, DateUtils.daysInclusive(DateUtils.startOfWeek(wednesday),
                DateUtils.endOfWeek(wednesday)));
    }

    @Test
    public void computesMonthAndYearBounds() {
        LocalDate date = LocalDate.of(2026, 11, 4);
        assertEquals(LocalDate.of(2026, 11, 1), DateUtils.startOfMonth(date));
        assertEquals(LocalDate.of(2026, 11, 30), DateUtils.endOfMonth(date));
        assertEquals(LocalDate.of(2026, 1, 1), DateUtils.startOfYear(date));
        assertEquals(LocalDate.of(2026, 12, 31), DateUtils.endOfYear(date));
        assertEquals(29, DateUtils.daysInMonth(YearMonth.of(2024, 2)));
    }

    @Test
    public void convertsBetweenDhakaAndUtc() {
        assertEquals("2026-11-04T02:00:00.000Z",
                DateUtils.atLocalDateTime(LocalDate.of(2026, 11, 4), 8, 0));
        assertEquals("2026-11-03T18:00:00.000Z",
                DateUtils.startOfDayIso(LocalDate.of(2026, 11, 4)));
        assertEquals("2026-11-04T17:59:59.999Z",
                DateUtils.endOfDayIso(LocalDate.of(2026, 11, 4)));
        assertEquals("2026-11-04", DateUtils.localDate("2026-11-03T20:00:00.000Z"));
    }

    @Test
    public void formatsAndParsesInstants() {
        assertEquals("2026-11-04T02:00:00.000Z",
                DateUtils.toIso(Instant.parse("2026-11-04T02:00:00Z")));
        assertEquals(Instant.EPOCH, DateUtils.parseInstant(null));
        assertEquals(Instant.EPOCH, DateUtils.parseInstant("not-a-date"));
        assertEquals(Instant.parse("2026-11-04T02:00:00Z"),
                DateUtils.parseInstant("2026-11-04T02:00:00.000Z"));
    }

    @Test
    public void countsInclusiveDays() {
        assertEquals(1, DateUtils.daysInclusive(LocalDate.of(2026, 11, 4),
                LocalDate.of(2026, 11, 4)));
        assertEquals(31, DateUtils.daysInclusive(LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 12, 1)));
    }
}
