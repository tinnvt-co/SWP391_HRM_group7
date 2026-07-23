package service;

import model.Employee;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttendanceImportPunchValidationTest {

    private static final LocalDate WORK_DATE = LocalDate.of(2026, 6, 1);

    public static void main(String[] args) {
        rejectsAfternoonOnlyPunchAsMissingCheckIn();
        rejectsMorningOnlyPunchAsMissingCheckOut();
        acceptsCompletePunchPair();
        warnsAndUsesOuterTimesForMultiplePunches();
        rejectsDuplicatePunchTimes();
        System.out.println("AttendanceImportPunchValidationTest PASSED");
    }

    private static void rejectsAfternoonOnlyPunchAsMissingCheckIn() {
        AttendanceImportService.Result result = validate(List.of(LocalTime.of(17, 30)));

        require(result.errors.size() == 1, "An afternoon-only punch must be rejected.");
        require(result.errors.get(0).contains("MP-TEST-001 on 2026-06-01"),
                "The error must identify the employee and work date.");
        require(result.errors.get(0).contains("only one punch at 17:30"),
                "The error must show the incomplete punch time.");
        require(result.errors.get(0).contains("missing check-in"),
                "An afternoon-only punch must be reported as a missing check-in.");
    }

    private static void rejectsMorningOnlyPunchAsMissingCheckOut() {
        AttendanceImportService.Result result = validate(List.of(LocalTime.of(7, 30)));

        require(result.errors.size() == 1, "A morning-only punch must be rejected.");
        require(result.errors.get(0).contains("missing check-out"),
                "A morning-only punch must be reported as a missing check-out.");
    }

    private static void acceptsCompletePunchPair() {
        AttendanceImportService.Result result = validate(
                List.of(LocalTime.of(7, 30), LocalTime.of(17, 30)));

        require(result.errors.isEmpty(), "A complete punch pair must be accepted.");
        require(result.warnings.isEmpty(), "A complete punch pair must not create a warning.");
    }

    private static void warnsAndUsesOuterTimesForMultiplePunches() {
        AttendanceImportService.Result result = validate(List.of(
                LocalTime.of(12, 0),
                LocalTime.of(17, 30),
                LocalTime.of(7, 30)));

        require(result.errors.isEmpty(), "Multiple distinct punches must remain importable.");
        require(result.warnings.size() == 1, "Multiple punches must produce one warning.");
        require(result.warnings.get(0).contains("07:30 was used as check-in"),
                "The warning must identify the earliest punch as check-in.");
        require(result.warnings.get(0).contains("17:30 as check-out"),
                "The warning must identify the latest punch as check-out.");
    }

    private static void rejectsDuplicatePunchTimes() {
        AttendanceImportService.Result result = validate(
                List.of(LocalTime.of(7, 30), LocalTime.of(7, 30)));

        require(result.errors.size() == 1, "Duplicate punch times must be rejected.");
        require(result.errors.get(0).contains("distinct check-in and check-out are required"),
                "The duplicate-time error must explain the required correction.");
    }

    private static AttendanceImportService.Result validate(List<LocalTime> times) {
        Employee employee = new Employee();
        employee.setEmployeeId(1);
        employee.setEmployeeCode("MP-TEST-001");

        Map<Integer, Map<LocalDate, List<LocalTime>>> punches = new HashMap<>();
        punches.put(employee.getEmployeeId(), Map.of(WORK_DATE, times));

        AttendanceImportService.Result result = new AttendanceImportService.Result();
        AttendanceImportService.validatePunchPairs(punches, List.of(employee), result);
        return result;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
