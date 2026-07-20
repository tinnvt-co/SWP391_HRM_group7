package service;

import dao.AttendanceRecordDAO;
import dao.HolidayDAO;
import dao.LeaveRequestDAO;
import model.AttendanceRecord;
import model.AttendanceRecord.AttendanceStatus;
import model.AttendanceRecord.VerificationStatus;
import model.Employee;
import model.LeaveRequest;
import model.LeaveRequest.LeaveType;
import util.XlsxReader;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Imports monthly attendance into attendance_records.
 *
 * Supported workbook format:
 *   A workbook with an "Attendance Detail" sheet using exactly these source
 *   columns: No., Attendance Code, Employee Code, Timestamp.
 * Reference/summary sheets may remain in the workbook, but imported punch data
 * always comes from "Attendance Detail".
 */
public class AttendanceImportService {

    private static final String REQUIRED_DETAIL_SHEET_NAME = "ATTENDANCE DETAIL";

    /** 0-based column index of the first day cell ("E") in the old template. */
    private static final int FIRST_DAY_COL = 4;
    private static final int CODE_COL = 1;
    private static final int HEADER_LABEL_ROW = 3;
    private static final int DATA_START_ROW = 5;

    private static final LocalTime STANDARD_CHECK_IN = LocalTime.of(7, 30);
    private static final LocalTime STANDARD_CHECK_OUT = LocalTime.of(17, 30);
    private static final LocalDate EXCEL_EPOCH = LocalDate.of(1899, 12, 30);
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static final Pattern MONTH_YEAR_SLASH =
            Pattern.compile("\\b(0?[1-9]|1[0-2])\\s*[/.-]\\s*((?:20)?\\d{2})\\b");
    private static final Pattern YEAR_MONTH_DASH =
            Pattern.compile("\\b((?:20)?\\d{2})\\s*[-/]\\s*(0?[1-9]|1[0-2])\\b");
    private static final Pattern EMPLOYEE_CODE_PARTS =
            Pattern.compile("^([A-Z]+)0*(\\d+)$");

    private static final DateTimeFormatter[] DETAIL_TIME_FORMATTERS = {
        formatter("d/M/uuuu h:mm a"),
        formatter("d/M/uuuu hh:mm a"),
        formatter("d/M/uuuu H:mm"),
        formatter("d/M/uuuu HH:mm"),
        formatter("d-M-uuuu h:mm a"),
        formatter("d-M-uuuu H:mm"),
        formatter("uuuu-M-d H:mm"),
        formatter("uuuu-M-d h:mm a")
    };

    private final AttendanceRecordDAO attendanceDAO = new AttendanceRecordDAO();
    private final LeaveRequestDAO leaveDAO = new LeaveRequestDAO();
    private final HolidayDAO holidayDAO = new HolidayDAO();

    /** Outcome of an import run, surfaced to the user. */
    public static final class Result {
        public int inserted;
        public int skippedExisting;
        public int skippedBlank;
        public final List<String> errors = new ArrayList<>();
        public final List<String> warnings = new ArrayList<>();

        public boolean hasErrors() { return !errors.isEmpty(); }
        public int totalProcessed() { return inserted + skippedExisting; }

        public void merge(Result other) {
            this.inserted += other.inserted;
            this.skippedExisting += other.skippedExisting;
            this.skippedBlank += other.skippedBlank;
            this.errors.addAll(other.errors);
            this.warnings.addAll(other.warnings);
        }
    }

    /**
     * Import all sheets from a multi-tab .xlsx file.
     *
     * Only the "Attendance Detail" sheet is accepted as the source of punch data.
     * Other sheets remain as human-readable reference only.
     */
    public Result importAllSheets(List<XlsxReader.Sheet> sheets, List<Employee> allEmps,
                                  YearMonth month, int importerUid, boolean overwrite)
            throws SQLException {
        Result result = new Result();
        XlsxReader.Sheet detailSheet = findDetailSheet(sheets);
        if (detailSheet == null) {
            result.errors.add("Workbook must contain a sheet named 'Attendance Detail'. "
                    + "Old day-column attendance templates are no longer accepted.");
            return result;
        }

        Set<String> referenceEmployeeCodes = findReferenceEmployeeCodes(sheets, detailSheet);
        return importDetailSheet(detailSheet, allEmps, referenceEmployeeCodes,
                month, importerUid, overwrite);
    }

    /**
     * Import the original day-column template.
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

        Map<String, Employee> byCode = buildEmployeeLookup(allEmps);
        int daysInMonth = month.lengthOfMonth();
        Map<Integer, LocalDate> colToDate = buildColumnDateMap(sheet, month, daysInMonth);
        if (colToDate.isEmpty()) {
            result.errors.add("No day columns found in the file. "
                    + "Please use the correct template for " + month + ".");
            return result;
        }

        for (int r = DATA_START_ROW; r < sheet.rowCount(); r++) {
            String code = sheet.get(r, CODE_COL).trim();
            if (code.isEmpty()) continue;

            String first = sheet.get(r, 0).trim();
            String normalizedFirst = first.toUpperCase(Locale.ROOT);
            if (normalizedFirst.startsWith("TONG")
                    || normalizedFirst.startsWith("TOTAL")
                    || normalizedFirst.startsWith("COMPANY TOTAL")
                    || normalizedFirst.startsWith("DEPARTMENT TOTAL")) break;

            Employee emp = byCode.get(normalizeEmployeeCode(code));
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

                AttendanceRecord rec = baseRecord(emp.getEmployeeId(), date, cv.status);
                rec.setOvertimeHours(cv.overtimeHours);
                rec.setNote(limitNote("Imported from template " + month));
                saveRecord(result, rec, overwrite);
            }
        }
        return result;
    }

    /**
     * Import the machine-detail sheet with punch timestamps.
     */
    private Result importDetailSheet(XlsxReader.Sheet sheet, List<Employee> allEmps,
                                     Set<String> referenceEmployeeCodes,
                                     YearMonth month, int importerUid, boolean overwrite)
            throws SQLException {
        Result result = new Result();
        DetailHeader header = findDetailHeader(sheet);
        if (header == null) {
            result.errors.add("Attendance Detail must contain exactly these header columns: "
                    + "No., Attendance Code, Employee Code, Timestamp.");
            return result;
        }

        YearMonth titleMonth = detectSheetMonthBeforeRow(sheet, header.row);
        if (titleMonth == null) {
            result.errors.add("Attendance Detail title must include the attendance month/year.");
            return result;
        }
        if (!titleMonth.equals(month)) {
            result.errors.add("Attendance Detail is for " + titleMonth
                    + ", but the selected import month is " + month + ".");
            return result;
        }

        Map<String, Employee> byCode = buildEmployeeLookup(allEmps);

        Map<Integer, Map<LocalDate, List<LocalTime>>> punchesByEmployee = new HashMap<>();
        int punchRows = 0;

        for (int r = header.row + 1; r < sheet.rowCount(); r++) {
            String employeeCode = firstNonBlank(
                    sheet.get(r, header.employeeCodeCol),
                    header.attendanceCodeCol >= 0 ? sheet.get(r, header.attendanceCodeCol) : "");
            String rawTime = sheet.get(r, header.timestampCol).trim();
            if (employeeCode.isBlank() && rawTime.isBlank()) continue;

            if (employeeCode.isBlank() || rawTime.isBlank()) {
                result.warnings.add("Row " + (r + 1)
                        + ": missing employee code or punch time (skipped).");
                continue;
            }

            Employee emp = byCode.get(normalizeEmployeeCode(employeeCode));
            if (emp == null) {
                result.errors.add("Row " + (r + 1) + ": employee code '" + employeeCode
                        + "' not found in attendance departments.");
                continue;
            }

            LocalDateTime punch = parsePunchTime(rawTime);
            if (punch == null) {
                result.errors.add("Row " + (r + 1) + ": invalid punch time '" + rawTime + "'.");
                continue;
            }

            YearMonth punchMonth = YearMonth.from(punch);
            if (!punchMonth.equals(month)) {
                result.errors.add("Row " + (r + 1) + ": punch time " + punch
                        + " is for " + punchMonth + ", but the selected import month is " + month + ".");
                continue;
            }

            punchesByEmployee
                    .computeIfAbsent(emp.getEmployeeId(), k -> new HashMap<>())
                    .computeIfAbsent(punch.toLocalDate(), k -> new ArrayList<>())
                    .add(punch.toLocalTime());
            punchRows++;
        }

        if (punchRows == 0) {
            result.errors.add("The detail attendance sheet has no valid punch rows for " + month + ".");
        }
        if (result.hasErrors()) return result;

        List<Employee> importEmployees = scopedEmployees(allEmps, referenceEmployeeCodes,
                punchesByEmployee.keySet(), result);
        Map<Integer, Employee> employeesById = new LinkedHashMap<>();
        for (Employee e : importEmployees) employeesById.put(e.getEmployeeId(), e);

        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();
        Map<Integer, Map<LocalDate, LeaveDay>> leaveDays = buildApprovedLeaveDays(monthStart, monthEnd, employeesById.keySet());
        Set<LocalDate> holidays = holidayDAO.findActiveDatesByMonth(month.getYear(), month.getMonthValue());

        for (Employee emp : importEmployees) {
            Map<LocalDate, List<LocalTime>> punchesByDate =
                    punchesByEmployee.getOrDefault(emp.getEmployeeId(), Collections.emptyMap());
            Map<LocalDate, LeaveDay> empLeaves =
                    leaveDays.getOrDefault(emp.getEmployeeId(), Collections.emptyMap());

            for (LocalDate date = monthStart; !date.isAfter(monthEnd); date = date.plusDays(1)) {
                List<LocalTime> punches = punchesByDate.get(date);
                LeaveDay leaveDay = empLeaves.get(date);
                AttendanceRecord rec;

                if (punches != null && !punches.isEmpty()) {
                    if (leaveDay != null) {
                        result.warnings.add(emp.getEmployeeCode() + " " + date
                                + ": has punches and an approved leave request; punch data was used.");
                    }
                    rec = buildPunchRecord(emp, date, punches, month);
                } else if (date.getDayOfWeek() == DayOfWeek.SUNDAY && !holidays.contains(date)) {
                    result.skippedBlank++;
                    continue;
                } else if (leaveDay != null) {
                    rec = buildLeaveRecord(emp, date, leaveDay, month);
                } else if (holidays.contains(date)) {
                    rec = baseRecord(emp.getEmployeeId(), date, AttendanceStatus.Holiday);
                    rec.setNote(limitNote("Imported from detail sheet " + month + "; official holiday without punch"));
                } else {
                    rec = baseRecord(emp.getEmployeeId(), date, AttendanceStatus.Absent);
                    rec.setNote(limitNote("Imported from detail sheet " + month + "; no punch and no approved leave"));
                }

                saveRecord(result, rec, overwrite);
            }
        }

        return result;
    }

    private AttendanceRecord buildPunchRecord(Employee emp, LocalDate date,
                                              List<LocalTime> punches, YearMonth month) {
        List<LocalTime> times = new ArrayList<>(punches);
        Collections.sort(times);
        LocalTime checkIn = times.get(0);
        LocalTime checkOut = times.size() > 1 ? times.get(times.size() - 1) : null;

        int lateMinutes = checkIn.isAfter(STANDARD_CHECK_IN)
                ? (int) Duration.between(STANDARD_CHECK_IN, checkIn).toMinutes()
                : 0;
        BigDecimal latePenalty = latePenaltyAmount(lateMinutes);
        BigDecimal overtime = overtimeHours(checkOut);

        AttendanceStatus status = lateMinutes > 0 ? AttendanceStatus.Late : AttendanceStatus.Present;
        AttendanceRecord rec = baseRecord(emp.getEmployeeId(), date, status);
        rec.setCheckInTime(checkIn);
        rec.setCheckOutTime(checkOut);
        rec.setLateMinutes(lateMinutes);
        rec.setLatePenaltyAmount(latePenalty);
        rec.setOvertimeHours(overtime);

        StringBuilder note = new StringBuilder("Imported from detail sheet ").append(month)
                .append("; in ").append(checkIn);
        if (checkOut != null) note.append("; out ").append(checkOut);
        else note.append("; missing checkout");
        if (lateMinutes > 0) {
            note.append("; late ").append(lateMinutes).append(" min")
                .append("; penalty ").append(latePenalty.toPlainString());
        }
        if (overtime.signum() > 0) {
            note.append("; OT ").append(overtime.toPlainString()).append("h");
        }
        rec.setNote(limitNote(note.toString()));
        return rec;
    }

    private AttendanceRecord buildLeaveRecord(Employee emp, LocalDate date,
                                              LeaveDay leaveDay, YearMonth month) {
        AttendanceStatus status = switch (leaveDay.type) {
            case UnpaidLeave -> AttendanceStatus.UnpaidLeave;
            case MaternityLeave -> AttendanceStatus.MaternityLeave;
            default -> AttendanceStatus.Leave;
        };
        AttendanceRecord rec = baseRecord(emp.getEmployeeId(), date, status);
        rec.setNote(limitNote("Imported from detail sheet " + month
                + "; approved " + leaveDay.type.getDbValue()
                + " request #" + leaveDay.leaveRequestId));
        return rec;
    }

    private AttendanceRecord baseRecord(int employeeId, LocalDate date, AttendanceStatus status) {
        AttendanceRecord rec = new AttendanceRecord();
        rec.setEmployeeId(employeeId);
        rec.setWorkDate(date);
        rec.setAttendanceStatus(status);
        rec.setOvertimeHours(ZERO);
        rec.setLateMinutes(0);
        rec.setLatePenaltyAmount(ZERO);
        rec.setVerificationStatus(VerificationStatus.Pending);
        return rec;
    }

    private void saveRecord(Result result, AttendanceRecord rec, boolean overwrite) throws SQLException {
        if (overwrite) {
            attendanceDAO.upsertImported(rec);
            result.inserted++;
            return;
        }

        if (attendanceDAO.insertIfAbsent(rec)) {
            result.inserted++;
        } else {
            result.skippedExisting++;
        }
    }

    private Map<Integer, Map<LocalDate, LeaveDay>> buildApprovedLeaveDays(
            LocalDate monthStart, LocalDate monthEnd, Set<Integer> attendanceEmployeeIds)
            throws SQLException {
        Map<Integer, Map<LocalDate, LeaveDay>> byEmployee = new HashMap<>();
        List<LeaveRequest> approved = leaveDAO.findApprovedOverlappingForAttendance(
                monthStart, monthEnd, null, null);

        for (LeaveRequest lr : approved) {
            if (!attendanceEmployeeIds.contains(lr.getEmployeeId())
                    || lr.getStartDate() == null
                    || lr.getEndDate() == null
                    || lr.getLeaveType() == null) {
                continue;
            }
            LocalDate start = lr.getStartDate().isBefore(monthStart) ? monthStart : lr.getStartDate();
            LocalDate end = lr.getEndDate().isAfter(monthEnd) ? monthEnd : lr.getEndDate();
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                LeaveDay candidate = new LeaveDay(lr);
                Map<LocalDate, LeaveDay> empDays =
                        byEmployee.computeIfAbsent(lr.getEmployeeId(), k -> new HashMap<>());
                LeaveDay existing = empDays.get(d);
                if (existing == null || leavePriority(candidate.type) > leavePriority(existing.type)) {
                    empDays.put(d, candidate);
                }
            }
        }
        return byEmployee;
    }

    private int leavePriority(LeaveType type) {
        if (type == LeaveType.MaternityLeave) return 3;
        if (type == LeaveType.UnpaidLeave) return 2;
        return 1;
    }

    private BigDecimal overtimeHours(LocalTime checkOut) {
        if (checkOut == null || !checkOut.isAfter(STANDARD_CHECK_OUT)) return ZERO;
        long minutes = Duration.between(STANDARD_CHECK_OUT, checkOut).toMinutes();
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal latePenaltyAmount(int lateMinutes) {
        if (lateMinutes < 5) return ZERO;
        if (lateMinutes <= 30) return new BigDecimal("50000");
        if (lateMinutes <= 60) return new BigDecimal("100000");
        return new BigDecimal("200000");
    }

    private Map<Integer, LocalDate> buildColumnDateMap(XlsxReader.Sheet sheet,
                                                       YearMonth month, int daysInMonth) {
        Map<Integer, LocalDate> map = new HashMap<>();
        int headerRow = HEADER_LABEL_ROW + 1;
        int cols = sheet.colCount(headerRow);
        for (int c = FIRST_DAY_COL; c < cols; c++) {
            String h = sheet.get(headerRow, c).trim();
            Integer day = asDay(h);
            if (day != null && day >= 1 && day <= daysInMonth) {
                map.put(c, month.atDay(day));
            }
        }
        if (!map.isEmpty()) return map;

        for (int d = 1; d <= daysInMonth; d++) {
            map.put(FIRST_DAY_COL + d - 1, month.atDay(d));
        }
        return map;
    }

    private XlsxReader.Sheet findDetailSheet(List<XlsxReader.Sheet> sheets) {
        for (XlsxReader.Sheet sheet : sheets) {
            if (normalizeLabel(sheet.getName()).equals(REQUIRED_DETAIL_SHEET_NAME)) {
                return sheet;
            }
        }
        return null;
    }

    private Set<String> findReferenceEmployeeCodes(List<XlsxReader.Sheet> sheets,
                                                   XlsxReader.Sheet detailSheet) {
        Set<String> codes = new java.util.LinkedHashSet<>();
        for (XlsxReader.Sheet sheet : sheets) {
            if (sheet == detailSheet) continue;
            int headerRow = findEmployeeListHeaderRow(sheet);
            if (headerRow < 0) continue;

            int codeCol = findEmployeeCodeColumn(sheet, headerRow);
            if (codeCol < 0) continue;

            for (int r = headerRow + 1; r < sheet.rowCount(); r++) {
                String first = normalizeLabel(sheet.get(r, 0));
                if (first.startsWith("TONG")
                        || first.startsWith("TOTAL")
                        || first.startsWith("COMPANY TOTAL")
                        || first.startsWith("DEPARTMENT TOTAL")) break;

                String code = sheet.get(r, codeCol).trim();
                if (code.isBlank()) continue;
                codes.add(normalizeEmployeeCode(code));
            }
        }
        return codes;
    }

    private int findEmployeeListHeaderRow(XlsxReader.Sheet sheet) {
        int rows = Math.min(sheet.rowCount(), 15);
        for (int r = 0; r < rows; r++) {
            boolean hasCode = false;
            boolean hasName = false;
            boolean hasDepartment = false;
            int cols = Math.max(sheet.colCount(r), 8);
            for (int c = 0; c < cols; c++) {
                String label = normalizeLabel(sheet.get(r, c));
                if (isEmployeeCodeLabel(label)) hasCode = true;
                if (isEmployeeNameLabel(label)) hasName = true;
                if (label.equals("DEPARTMENT") || label.equals("DEPARTMENT NAME") || label.equals("PHONG BAN")) hasDepartment = true;
            }
            if (hasCode && hasName && hasDepartment) return r;
        }
        return -1;
    }

    private int findEmployeeCodeColumn(XlsxReader.Sheet sheet, int headerRow) {
        int cols = Math.max(sheet.colCount(headerRow), 8);
        for (int c = 0; c < cols; c++) {
            String label = normalizeLabel(sheet.get(headerRow, c));
            if (isEmployeeCodeLabel(label)) return c;
        }
        return -1;
    }

    private List<Employee> scopedEmployees(List<Employee> allEmps,
                                           Set<String> referenceEmployeeCodes,
                                           Set<Integer> employeeIdsWithPunches,
                                           Result result) {
        if (referenceEmployeeCodes == null || referenceEmployeeCodes.isEmpty()) {
            return allEmps;
        }

        Map<String, Employee> byCode = buildEmployeeLookup(allEmps);
        Map<Integer, Employee> scoped = new LinkedHashMap<>();
        int matchedReferenceCodes = 0;

        for (String code : referenceEmployeeCodes) {
            Employee emp = byCode.get(code);
            if (emp != null) {
                scoped.put(emp.getEmployeeId(), emp);
                matchedReferenceCodes++;
            }
        }

        for (Employee emp : allEmps) {
            if (employeeIdsWithPunches.contains(emp.getEmployeeId())) {
                scoped.put(emp.getEmployeeId(), emp);
            }
        }

        if (scoped.isEmpty()) {
            result.warnings.add("Reference employee list did not match system codes; using all attendance employees.");
            return allEmps;
        }
        if (matchedReferenceCodes < referenceEmployeeCodes.size()) {
            result.warnings.add("Reference employee list has "
                    + (referenceEmployeeCodes.size() - matchedReferenceCodes)
                    + " code(s) not found in the system.");
        }
        return new ArrayList<>(scoped.values());
    }

    private DetailHeader findDetailHeader(XlsxReader.Sheet sheet) {
        int rows = Math.min(sheet.rowCount(), 15);
        for (int r = 0; r < rows; r++) {
            String noLabel = normalizeLabel(sheet.get(r, 0));
            String attendanceCodeLabel = normalizeLabel(sheet.get(r, 1));
            String employeeCodeLabel = normalizeLabel(sheet.get(r, 2));
            String timestampLabel = normalizeLabel(sheet.get(r, 3));
            if (noLabel.equals("NO")
                    && attendanceCodeLabel.equals("ATTENDANCE CODE")
                    && employeeCodeLabel.equals("EMPLOYEE CODE")
                    && timestampLabel.equals("TIMESTAMP")
                    && hasNoExtraDetailHeaderColumns(sheet, r)) {
                return new DetailHeader(r, 1, 2, 3);
            }
        }
        return null;
    }

    private boolean hasNoExtraDetailHeaderColumns(XlsxReader.Sheet sheet, int headerRow) {
        int cols = sheet.colCount(headerRow);
        for (int c = 4; c < cols; c++) {
            if (!sheet.get(headerRow, c).trim().isEmpty()) return false;
        }
        return true;
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

    private YearMonth detectSheetMonthBeforeRow(XlsxReader.Sheet sheet, int beforeRow) {
        int rows = Math.max(0, Math.min(sheet.rowCount(), beforeRow));
        for (int r = 0; r < rows; r++) {
            int cols = Math.max(sheet.colCount(r), 1);
            for (int c = 0; c < cols; c++) {
                YearMonth ym = parseMonthYear(sheet.get(r, c));
                if (ym != null) return ym;
            }
        }
        return null;
    }

    private boolean isEmployeeCodeLabel(String label) {
        return label.equals("EMPLOYEE CODE")
                || label.equals("EMPLOYEE ID")
                || label.equals("EMP CODE")
                || label.equals("MA NV")
                || label.equals("MA NHAN VIEN");
    }

    private boolean isEmployeeNameLabel(String label) {
        return label.equals("FULL NAME")
                || label.equals("EMPLOYEE NAME")
                || label.equals("NAME")
                || label.equals("HO VA TEN")
                || label.equals("HO TEN");
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

    private LocalDateTime parsePunchTime(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String text = raw.trim().replace('\u00A0', ' ').replaceAll("\\s+", " ");

        LocalDateTime fromSerial = parseExcelSerialDateTime(text);
        if (fromSerial != null) return fromSerial;

        for (DateTimeFormatter fmt : DETAIL_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(text, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private LocalDateTime parseExcelSerialDateTime(String text) {
        try {
            BigDecimal serial = new BigDecimal(text);
            if (serial.compareTo(new BigDecimal("30000")) < 0
                    || serial.compareTo(new BigDecimal("80000")) > 0) {
                return null;
            }

            int days = serial.setScale(0, RoundingMode.FLOOR).intValue();
            BigDecimal fraction = serial.subtract(BigDecimal.valueOf(days));
            long seconds = fraction.multiply(BigDecimal.valueOf(86400))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValue();

            LocalDate date = EXCEL_EPOCH.plusDays(days);
            if (seconds >= 86400) {
                date = date.plusDays(1);
                seconds -= 86400;
            }
            return date.atTime(LocalTime.ofSecondOfDay(seconds));
        } catch (NumberFormatException | ArithmeticException ex) {
            return null;
        }
    }

    private Map<String, Employee> buildEmployeeLookup(List<Employee> allEmps) {
        Map<String, Employee> byCode = new HashMap<>();
        for (Employee e : allEmps) {
            if (e.getEmployeeCode() == null) continue;
            registerEmployeeCode(byCode, e.getEmployeeCode(), e);
        }
        return byCode;
    }

    private void registerEmployeeCode(Map<String, Employee> byCode, String rawCode, Employee employee) {
        String compact = normalizeEmployeeCode(rawCode);
        if (compact.isBlank()) return;
        byCode.putIfAbsent(compact, employee);

        Matcher m = EMPLOYEE_CODE_PARTS.matcher(compact);
        if (m.matches()) {
            String noLeadingZeros = m.group(1) + m.group(2);
            byCode.putIfAbsent(noLeadingZeros, employee);
        }
    }

    private String normalizeEmployeeCode(String value) {
        if (value == null) return "";
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private String normalizeLabel(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('\u0110', 'D')
                .replace('\u0111', 'd')
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", " ")
                .trim();
        return normalized.replaceAll("\\s+", " ");
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) return first.trim();
        return second == null ? "" : second.trim();
    }

    private String limitNote(String note) {
        if (note == null) return null;
        return note.length() <= 255 ? note : note.substring(0, 255);
    }

    private static Integer asDay(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            if (s.contains(".")) return (int) Double.parseDouble(s);
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Parsed content of a single day cell in the old template. */
    static final class CellValue {
        final AttendanceStatus status;
        final BigDecimal overtimeHours;
        CellValue(AttendanceStatus status, BigDecimal ot) {
            this.status = status;
            this.overtimeHours = ot;
        }
    }

    static CellValue parseCell(String raw) {
        BigDecimal ot = parseOvertime(raw);
        if (ot != null) {
            return new CellValue(AttendanceStatus.Present, ot);
        }
        AttendanceStatus status = mapStatus(raw);
        if (status == null) return null;
        return new CellValue(status, ZERO);
    }

    private static BigDecimal parseOvertime(String raw) {
        try {
            BigDecimal v = new BigDecimal(raw.replace(",", "."));
            return v.signum() < 0 ? null : v;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    static AttendanceStatus mapStatus(String raw) {
        String normalized = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('\u0110', 'D')
                .replace('\u0111', 'd')
                .toUpperCase(Locale.ROOT);
        switch (normalized) {
            case "P": return AttendanceStatus.Present;
            case "A": return AttendanceStatus.Absent;
            case "L": return AttendanceStatus.Leave;
            case "U":
            case "UL":
            case "UNPAID":
            case "UNPAID LEAVE":
            case "NGHI KHONG LUONG":
                return AttendanceStatus.UnpaidLeave;
            case "T": return AttendanceStatus.Late;
            case "O": return AttendanceStatus.Present;
            case "H": return AttendanceStatus.Holiday;
            case "M":
            case "ML":
            case "MATERNITY":
            case "MATERNITY LEAVE":
            case "NGHI THAI SAN":
            case "THAI SAN":
                return AttendanceStatus.MaternityLeave;
            default:  return null;
        }
    }

    private static DateTimeFormatter formatter(String pattern) {
        return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(pattern)
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .toFormatter(Locale.ENGLISH);
    }

    private static final class DetailHeader {
        final int row;
        final int attendanceCodeCol;
        final int employeeCodeCol;
        final int timestampCol;

        DetailHeader(int row, int attendanceCodeCol, int employeeCodeCol, int timestampCol) {
            this.row = row;
            this.attendanceCodeCol = attendanceCodeCol;
            this.employeeCodeCol = employeeCodeCol;
            this.timestampCol = timestampCol;
        }
    }

    private static final class LeaveDay {
        final int leaveRequestId;
        final LeaveType type;

        LeaveDay(LeaveRequest request) {
            this.leaveRequestId = request.getLeaveRequestId();
            this.type = request.getLeaveType();
        }
    }
}
