package service;

import dao.AttendanceRecordDAO;
import model.AttendanceRecord;
import model.AttendanceRecord.AttendanceStatus;
import model.AttendanceRecord.VerificationStatus;
import model.Employee;
import util.XlsxReader;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Imports a monthly attendance sheet (.xlsx) into attendance_records.
 *
 * Expected sheet layout (produced by attendance-templates/generate_templates.py):
 *   row 1 : title
 *   row 2 : legend
 *   row 4 : column labels  (STT | Ma NV | Ho va Ten | Phong Ban | day1..dayN | summary...)
 *   row 5 : day numbers under the "TRONG THANG" span
 *   row 6+: one employee per row; the first day cell is column index 4 (0-based, "E")
 *
 * Day-cell legend -> attendance_status:
 *   P -> Present, A -> Absent, L -> Leave, T -> Late, H -> Holiday
 *   a NUMBER (e.g. 2) -> Present that day + that many overtime hours
 *
 * Only the status and overtime hours are stored. Cells left blank are skipped.
 * Existing (employee, date) rows are skipped (not overwritten) unless overwrite=true.
 *
 * Flow: HR Staff imports the ALL-department sheet; every attendance-scoped
 * employee code is accepted (Admin/HR/IT excluded). Records start as Pending. Department managers then
 * confirm (verify) their own team's records.
 */
public class AttendanceImportService {

    /** 0-based column index of the first day cell ("E"). Must match the template. */
    private static final int FIRST_DAY_COL = 4;
    private static final int CODE_COL = 1;      // "Ma NV"  ("B")
    private static final int HEADER_LABEL_ROW = 3; // 0-based row 4
    private static final int DATA_START_ROW = 5;   // 0-based row 6
    private static final Pattern MONTH_YEAR_SLASH =
            Pattern.compile("\\b(0?[1-9]|1[0-2])\\s*[/.-]\\s*((?:20)?\\d{2})\\b");
    private static final Pattern YEAR_MONTH_DASH =
            Pattern.compile("\\b((?:20)?\\d{2})\\s*[-/]\\s*(0?[1-9]|1[0-2])\\b");

    private final AttendanceRecordDAO attendanceDAO = new AttendanceRecordDAO();

    /** Outcome of an import run, surfaced to the user. */
    public static final class Result {
        public int inserted;
        public int skippedExisting;
        public int skippedBlank;
        public final List<String> errors = new ArrayList<>();
        public final List<String> warnings = new ArrayList<>();

        public boolean hasErrors() { return !errors.isEmpty(); }
        public int totalProcessed() { return inserted + skippedExisting; }

        /** Merge another result into this one (for multi-sheet import). */
        public void merge(Result other) {
            this.inserted += other.inserted;
            this.skippedExisting += other.skippedExisting;
            this.skippedBlank += other.skippedBlank;
            this.errors.addAll(other.errors);
            this.warnings.addAll(other.warnings);
        }
    }

    /**
     * Import ALL sheets from a multi-tab .xlsx file (e.g. the ALL-department template).
     * Iterates every Sheet and merges the results.
     */
    public Result importAllSheets(List<XlsxReader.Sheet> sheets, List<Employee> allEmps,
                                  YearMonth month, int importerUid, boolean overwrite)
            throws SQLException {
        Result merged = new Result();
        for (int i = 0; i < sheets.size(); i++) {
            YearMonth sheetMonth = detectSheetMonth(sheets.get(i));
            if (sheetMonth == null) {
                merged.errors.add("Sheet " + (i + 1)
                        + ": could not detect the attendance month/year in the file title.");
            } else if (!sheetMonth.equals(month)) {
                merged.errors.add("Sheet " + (i + 1) + ": file is for " + sheetMonth
                        + ", but the selected import month is " + month + ".");
            }
        }
        if (merged.hasErrors()) return merged;

        for (int i = 0; i < sheets.size(); i++) {
            Result r = importSheet(sheets.get(i), allEmps, month, importerUid, overwrite);
            merged.merge(r);
        }
        return merged;
    }

    /**
     * @param sheet       parsed worksheet
     * @param allEmps     active employees in departments that use attendance
     * @param month       the month the sheet belongs to (year+month)
     * @param importerUid user id recorded as the importer (HR Staff)
     * @param overwrite   if true, replace an existing row for the same (emp, date)
     */
    public Result importSheet(XlsxReader.Sheet sheet, List<Employee> allEmps,
                              YearMonth month, int importerUid, boolean overwrite)
            throws SQLException {

        Result result = new Result();
        YearMonth sheetMonth = detectSheetMonth(sheet);
        if (sheetMonth == null) {
            result.errors.add("Could not detect the attendance month/year in the file title.");
            return result;
        }
        if (!sheetMonth.equals(month)) {
            result.errors.add("File is for " + sheetMonth
                    + ", but the selected import month is " + month + ".");
            return result;
        }

        // code -> employee, for fast lookup
        Map<String, Employee> byCode = new HashMap<>();
        for (Employee e : allEmps) {
            if (e.getEmployeeCode() != null) {
                byCode.put(e.getEmployeeCode().trim().toUpperCase(), e);
            }
        }

        int daysInMonth = month.lengthOfMonth();

        // Map each day-column to a LocalDate using the day-number header row if
        // present, otherwise assume columns run 1..daysInMonth left to right.
        Map<Integer, LocalDate> colToDate = buildColumnDateMap(sheet, month, daysInMonth);
        if (colToDate.isEmpty()) {
            result.errors.add("No day columns found in the file. "
                    + "Please use the correct template for " + month + ".");
            return result;
        }

        for (int r = DATA_START_ROW; r < sheet.rowCount(); r++) {
            String code = sheet.get(r, CODE_COL).trim();
            if (code.isEmpty()) continue; // blank row or the total row

            // Stop at the total row ("DEPARTMENT TOTAL" / "TONG CONG...").
            String first = sheet.get(r, 0).trim();
            if (first.toUpperCase().startsWith("TONG")
                    || first.toUpperCase().startsWith("DEPARTMENT TOTAL")) break;

            Employee emp = byCode.get(code.toUpperCase());
            if (emp == null) {
                result.errors.add("Row " + (r + 1) + ": employee code '" + code
                        + "' not found in the system (skipped).");
                continue;
            }

            for (Map.Entry<Integer, LocalDate> ce : colToDate.entrySet()) {
                int col = ce.getKey();
                LocalDate date = ce.getValue();
                String raw = sheet.get(r, col).trim();
                if (raw.isEmpty()) { result.skippedBlank++; continue; }

                CellValue cv = parseCell(raw);
                if (cv == null) {
                    result.warnings.add("Row " + (r + 1) + " day " + date.getDayOfMonth()
                            + ": invalid code '" + raw + "' (cell skipped).");
                    continue;
                }

                boolean exists = attendanceDAO.existsByEmployeeAndDate(emp.getEmployeeId(), date);
                if (exists && !overwrite) { result.skippedExisting++; continue; }

                AttendanceRecord rec = new AttendanceRecord();
                rec.setEmployeeId(emp.getEmployeeId());
                rec.setWorkDate(date);
                rec.setAttendanceStatus(cv.status);
                rec.setOvertimeHours(cv.overtimeHours);
                // Imported rows start as Pending; the manager verifies them in
                // bulk via "Send to HR Staff".
                rec.setVerificationStatus(VerificationStatus.Pending);
                rec.setNote("Imported from sheet " + month);
                // verifiedBy/verifiedAt left at DB defaults.

                if (exists) {
                    // overwrite: delete the old row, then insert fresh
                    AttendanceRecord old = findExisting(emp.getEmployeeId(), date);
                    if (old != null) attendanceDAO.deleteById(old.getAttendanceId());
                }
                attendanceDAO.insert(rec);
                result.inserted++;
            }
        }
        return result;
    }

    private AttendanceRecord findExisting(int employeeId, LocalDate date) throws SQLException {
        // Cheap path: we only need the id to delete. Re-use findByEmployeeId range
        // narrowed to the single day.
        List<AttendanceRecord> list =
                attendanceDAO.findByEmployeeId(employeeId, date, date);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Walk the day-number header row (0-based row 4) and map any column whose
     * header is an integer 1..daysInMonth to its LocalDate. Falls back to a
     * positional mapping starting at FIRST_DAY_COL when no numbers are found.
     */
    private Map<Integer, LocalDate> buildColumnDateMap(XlsxReader.Sheet sheet,
                                                       YearMonth month, int daysInMonth) {
        Map<Integer, LocalDate> map = new HashMap<>();
        int headerRow = HEADER_LABEL_ROW + 1; // day numbers sit one row below labels
        int cols = sheet.colCount(headerRow);
        for (int c = FIRST_DAY_COL; c < cols; c++) {
            String h = sheet.get(headerRow, c).trim();
            Integer day = asDay(h);
            if (day != null && day >= 1 && day <= daysInMonth) {
                map.put(c, month.atDay(day));
            }
        }
        if (!map.isEmpty()) return map;

        // Fallback: assume FIRST_DAY_COL.. are days 1..daysInMonth in order.
        for (int d = 1; d <= daysInMonth; d++) {
            map.put(FIRST_DAY_COL + d - 1, month.atDay(d));
        }
        return map;
    }

    private YearMonth detectSheetMonth(XlsxReader.Sheet sheet) {
        int rows = Math.min(sheet.rowCount(), 8);
        for (int r = 0; r < rows; r++) {
            int cols = Math.max(sheet.colCount(r), 1);
            for (int c = 0; c < cols; c++) {
                YearMonth ym = parseMonthYear(sheet.get(r, c));
                if (ym != null) return ym;
            }
        }
        return null;
    }

    private YearMonth parseMonthYear(String text) {
        if (text == null || text.isBlank()) return null;

        Matcher slash = MONTH_YEAR_SLASH.matcher(text);
        if (slash.find()) {
            return buildYearMonth(slash.group(2), slash.group(1));
        }

        Matcher dash = YEAR_MONTH_DASH.matcher(text);
        if (dash.find()) {
            return buildYearMonth(dash.group(1), dash.group(2));
        }

        return null;
    }

    private YearMonth buildYearMonth(String yearText, String monthText) {
        try {
            int year = Integer.parseInt(yearText.trim());
            int month = Integer.parseInt(monthText.trim());
            if (year < 100) year += 2000;
            if (year < 2000 || year > 2100 || month < 1 || month > 12) return null;
            return YearMonth.of(year, month);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Integer asDay(String s) {
        if (s == null || s.isEmpty()) return null;
        // header cell may be "1" or "1.0" depending on how it was saved
        try {
            if (s.contains(".")) return (int) Double.parseDouble(s);
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Parsed content of a single day cell: a status plus optional OT hours. */
    static final class CellValue {
        final AttendanceStatus status;
        final java.math.BigDecimal overtimeHours;
        CellValue(AttendanceStatus status, java.math.BigDecimal ot) {
            this.status = status;
            this.overtimeHours = ot;
        }
    }

    /**
     * Parse a day cell. Two forms are accepted:
     *   - a LETTER  (P/A/L/T/O/H) -> the matching status, 0 OT hours
     *   - a NUMBER  (e.g. "2")    -> Present that day + that many OT hours
     * Returns null for anything unrecognized.
     */
    static CellValue parseCell(String raw) {
        java.math.BigDecimal ot = parseOvertime(raw);
        if (ot != null) {
            // Numeric cell: worked that day (Present) with N overtime hours.
            return new CellValue(AttendanceStatus.Present, ot);
        }
        AttendanceStatus status = mapStatus(raw);
        if (status == null) return null;
        return new CellValue(status, java.math.BigDecimal.ZERO);
    }

    /** Returns a non-negative BigDecimal if {@code raw} is a number, else null. */
    private static java.math.BigDecimal parseOvertime(String raw) {
        try {
            java.math.BigDecimal v = new java.math.BigDecimal(raw.replace(",", "."));
            return v.signum() < 0 ? null : v;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Map a legend letter to an attendance status. Returns null if unknown. */
    static AttendanceStatus mapStatus(String raw) {
        switch (raw.toUpperCase()) {
            case "P": return AttendanceStatus.Present;
            case "A": return AttendanceStatus.Absent;
            case "L": return AttendanceStatus.Leave;
            case "T": return AttendanceStatus.Late;
            case "O": return AttendanceStatus.Present; // OT day still counts as present
            case "H": return AttendanceStatus.Holiday;
            default:  return null;
        }
    }
}
