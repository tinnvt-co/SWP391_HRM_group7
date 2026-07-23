package service;

import model.WorkSchedule;

import java.math.BigDecimal;
import java.time.LocalDate;

public class WorkingCalendarServiceTest {

    public static void main(String[] args) {
        WorkSchedule schedule = new WorkSchedule();
        schedule.setWorkingDays("MON,TUE,WED,THU,FRI,SAT");
        schedule.setDailyWorkingHours(new BigDecimal("8"));

        require(count("2026-06-01", "2026-06-30", schedule) == 26,
                "June 2026 must have 26 Monday-to-Saturday working days.");
        require(count("2026-07-01", "2026-07-31", schedule) == 27,
                "July 2026 must have 27 Monday-to-Saturday working days.");
        require(count("2026-02-01", "2026-02-28", schedule) == 24,
                "February 2026 must have 24 Monday-to-Saturday working days.");
        require(count("2026-07-23", "2026-07-31", schedule) == 8,
                "A contract starting on July 23 must expect 8 scheduled days in July.");

        System.out.println("WorkingCalendarServiceTest PASSED");
    }

    private static int count(String start, String end, WorkSchedule schedule) {
        return WorkingCalendarService.countScheduledDays(
                LocalDate.parse(start), LocalDate.parse(end), schedule);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
