package controller;

import dao.AttendanceRecordDAO;
import dao.AttendanceReportDAO;
import dao.DepartmentDAO;
import dao.EmployeeDAO;
import dao.PayrollPeriodDAO;
import model.AttendanceRecord;
import model.AttendanceRecord.AttendanceStatus;
import model.AttendanceRecord.VerificationStatus;
import model.AttendanceReport;
import model.Department;
import model.Employee;
import model.PayrollTaskSummary;
import model.User;
import service.AttendanceAutoConfirmService;
import service.AttendanceImportService;
import util.XlsxReader;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "AttendanceServlet", urlPatterns = {"/attendance"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,        // 1 MB
        maxFileSize       = 10L * 1024 * 1024,  // 10 MB per file
        maxRequestSize    = 12L * 1024 * 1024   // 12 MB total
)
public class AttendanceServlet extends HttpServlet {

    private static final int PAGE_SIZE = 10;

    private final AttendanceRecordDAO attendanceDAO = new AttendanceRecordDAO();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final AttendanceReportDAO reportDAO = new AttendanceReportDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();
    private final PayrollPeriodDAO payrollPeriodDAO = new PayrollPeriodDAO();
    private final AttendanceAutoConfirmService autoConfirmService = new AttendanceAutoConfirmService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        if (action == null) action = "list";

        try {
            switch (action) {
                case "edit" -> {
                    if (!hasPermission(request, "VERIFY_STAFF_ATTENDANCE")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleEditForm(request, response);
                }
                case "employeeDetail" -> {
                    if (!hasPermission(request, "VIEW_ATTENDANCE")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleEmployeeDetail(request, response);
                }
                default -> {
                    if (!hasPermission(request, "VIEW_ATTENDANCE")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleList(request, response);
                }
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        if (action == null) action = "";

        try {
            switch (action) {
                case "import" -> {
                    if (!hasPermission(request, "IMPORT_ATTENDANCE")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleImport(request, response);
                }
                case "edit" -> {
                    if (!hasPermission(request, "VERIFY_STAFF_ATTENDANCE")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleEdit(request, response);
                }
                case "verify" -> {
                    if (!hasPermission(request, "VERIFY_STAFF_ATTENDANCE")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleVerify(request, response);
                }
                case "confirmToHr" -> {
                    if (!hasPermission(request, "VERIFY_STAFF_ATTENDANCE")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleConfirmToHr(request, response);
                }
                case "confirmHrDepartment" -> {
                    if (!hasPermission(request, "VERIFY_STAFF_ATTENDANCE")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleConfirmHrDepartment(request, response);
                }
                default -> response.sendRedirect(request.getContextPath() + "/attendance");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void handleList(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        User currentUser = getCurrentUser(request);
        String roleName  = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "";

        boolean managerScope = "MANAGER".equalsIgnoreCase(roleName)
                || "HR_MANAGER".equalsIgnoreCase(roleName);
        boolean hrScope      = "HR_STAFF".equalsIgnoreCase(roleName)
                            || "ADMIN".equalsIgnoreCase(roleName);

        if (managerScope || hrScope) {
            autoConfirmService.runDueAutoConfirm();
        }

        Integer deptId = parseIntOrNull(request.getParameter("deptId"));
        List<Department> attendanceDepartments = null;
        Department selectedDepartment = null;
        Department managedDepartment = managerScope
                ? findAttendanceDepartmentManagedBy(currentUser.getUserId()) : null;
        if (hrScope) {
            attendanceDepartments = departmentDAO.findAttendanceDepartments();
            if (!containsDepartment(attendanceDepartments, deptId)) {
                deptId = attendanceDepartments.isEmpty() ? null : attendanceDepartments.get(0).getDepartmentId();
            }
            selectedDepartment = findDepartment(attendanceDepartments, deptId);
        }
        boolean hrDepartmentReviewerScope = "HR_MANAGER".equalsIgnoreCase(roleName)
                && managedDepartment != null
                && "HR".equalsIgnoreCase(managedDepartment.getDepartmentCode());

        YearMonth selectedMonth = parseYearMonth(request.getParameter("year"), request.getParameter("month"));
        if (selectedMonth == null) {
            YearMonth latest = null;
            if (managerScope) {
                latest = attendanceDAO.findLatestMonth(currentUser.getUserId(), null);
            } else if (hrScope) {
                latest = attendanceDAO.findLatestMonth(null, deptId);
            }
            selectedMonth = latest != null ? latest : YearMonth.now();
        }

        LocalDate monthStart = selectedMonth.atDay(1);
        LocalDate monthEnd   = selectedMonth.atEndOfMonth();

        request.setAttribute("managerScope", managerScope);
        request.setAttribute("hrScope", hrScope);
        request.setAttribute("departmentReviewerScope", managerScope || hrDepartmentReviewerScope);
        request.setAttribute("hrDepartmentReviewerScope", hrDepartmentReviewerScope);
        request.setAttribute("confirmAttendanceAction",
                hrDepartmentReviewerScope ? "confirmHrDepartment" : "confirmToHr");
        request.setAttribute("selectedYear", selectedMonth.getYear());
        request.setAttribute("selectedMonth", selectedMonth.getMonthValue());
        request.setAttribute("yearOptions", buildYearOptions(selectedMonth.getYear()));
        request.setAttribute("monthStart", monthStart);
        request.setAttribute("monthEnd", monthEnd);
        request.setAttribute("importMonthLabel", monthLabel(selectedMonth));
        request.setAttribute("canImportAttendance", hasPermission(request, "IMPORT_ATTENDANCE"));
        attachPayrollTaskSummary(request, roleName);

        if (managerScope) {
            // Manager: show their department, including the manager's own attendance.
            List<AttendanceRecordDAO.EmployeeAttSummary> cards =
                    attendanceDAO.summaryByManager(currentUser.getUserId(), monthStart, monthEnd);
            request.setAttribute("employeeCards", cards);
            request.setAttribute("pendingCount",
                    attendanceDAO.countPendingByManager(currentUser.getUserId(), monthStart, monthEnd));
            boolean confirmEnabled = hrDepartmentReviewerScope
                    ? !cards.isEmpty() && !reportDAO.hasHrManagerApprovedMonth(
                            selectedMonth.getYear(), selectedMonth.getMonthValue(),
                            managedDepartment.getDepartmentId())
                    : attendanceDAO.countPendingByManager(
                            currentUser.getUserId(), monthStart, monthEnd) > 0;
            request.setAttribute("confirmAttendanceEnabled", confirmEnabled);
            if (managedDepartment != null) {
                request.setAttribute("selectedDeptId", managedDepartment.getDepartmentId());
            }

        } else if (hrScope) {
            // HR Staff / HR Manager: show department list first, then cards if dept selected
            request.setAttribute("departments", attendanceDepartments);

            if (deptId != null) {
                List<AttendanceRecordDAO.EmployeeAttSummary> cards =
                        attendanceDAO.summaryByDepartment(deptId, monthStart, monthEnd);
                request.setAttribute("employeeCards", cards);
                request.setAttribute("selectedDeptId", deptId);
                if (hrDepartmentReviewerScope) {
                    int pendingCount = attendanceDAO.countPendingByDepartmentMonth(
                            selectedMonth.getYear(), selectedMonth.getMonthValue(), deptId);
                    request.setAttribute("pendingCount", pendingCount);
                    request.setAttribute("confirmAttendanceEnabled",
                            !cards.isEmpty() && !reportDAO.hasHrManagerApprovedMonth(
                                    selectedMonth.getYear(), selectedMonth.getMonthValue(), deptId));
                }
            }

        } else {
            // Regular employee: go straight to their own detail
            Employee me = employeeDAO.findByUserId(currentUser.getUserId());
            if (me != null) {
                response.sendRedirect(request.getContextPath()
                        + "/attendance?action=employeeDetail&employeeId=" + me.getEmployeeId());
                return;
            }
        }

        // Flash messages from a preceding import/confirm (PRG pattern).
        HttpSession flashSession = request.getSession(false);
        if (flashSession != null) {
            Object ok = flashSession.getAttribute("importMessage");
            Object err = flashSession.getAttribute("importError");
            if (ok != null)  { request.setAttribute("importMessage", ok);  flashSession.removeAttribute("importMessage"); }
            if (err != null) { request.setAttribute("importError", err);   flashSession.removeAttribute("importError"); }
        }

        request.getRequestDispatcher("/views/attendance/attendance-list.jsp")
               .forward(request, response);
    }

    /**
     * Show attendance records for a single employee.
     * Used when clicking an employee card (Manager or HR Staff drills down).
     */
    private void handleEmployeeDetail(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        User currentUser = getCurrentUser(request);
        String roleName  = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "";

        Integer employeeId = parseIntOrNull(request.getParameter("employeeId"));
        if (employeeId == null) {
            response.sendRedirect(request.getContextPath() + "/attendance");
            return;
        }

        // Regular employee can only see their own records.
        boolean managerScope = "MANAGER".equalsIgnoreCase(roleName)
                || "HR_MANAGER".equalsIgnoreCase(roleName);
        boolean hrScope      = "HR_STAFF".equalsIgnoreCase(roleName)
                            || "ADMIN".equalsIgnoreCase(roleName);
        Employee emp = employeeDAO.findById(employeeId);
        boolean managesEmployee = managerScope && emp != null
                && managesAttendanceDepartment(currentUser.getUserId(), emp.getDepartmentId());
        if (managerScope && !managesEmployee) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!managerScope && !hrScope) {
            Employee me = employeeDAO.findByUserId(currentUser.getUserId());
            if (me == null || me.getEmployeeId() != employeeId) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }

        LocalDate fromDate = parseDateOrNull(request.getParameter("fromDate"));
        LocalDate toDate   = parseDateOrNull(request.getParameter("toDate"));
        LocalDate today = LocalDate.now();
        if (fromDate != null && fromDate.isAfter(today)) fromDate = today;
        if (toDate != null && toDate.isAfter(today)) toDate = today;

        int totalRecords = attendanceDAO.countByEmployeeId(employeeId, fromDate, toDate);
        int totalPages = Math.max(1, (int) Math.ceil(totalRecords / (double) PAGE_SIZE));
        int page = parseIntOr(request.getParameter("page"), 1);
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        List<AttendanceRecord> records = attendanceDAO.findByEmployeeId(
                employeeId, fromDate, toDate, (page - 1) * PAGE_SIZE, PAGE_SIZE);

        boolean canManageAttendance = managesEmployee;

        request.setAttribute("records", records);
        request.setAttribute("detailEmployee", emp);
        request.setAttribute("employeeId", employeeId);
        request.setAttribute("fromDate", fromDate);
        request.setAttribute("toDate", toDate);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalRecords", totalRecords);
        request.setAttribute("managerScope", managerScope);
        request.setAttribute("hrScope", hrScope);
        request.setAttribute("canManageAttendance", canManageAttendance);

        request.getRequestDispatcher("/views/attendance/attendance-detail.jsp")
               .forward(request, response);
    }

    /**
     * Import a monthly attendance workbook uploaded by HR Staff.
     * Only the "Attendance Detail" sheet is used; reference/summary sheets are ignored.
     * Records start as Pending Manager Confirmation.
     */
    private void handleImport(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        User currentUser = getCurrentUser(request);
        HttpSession session = request.getSession(true);
        String ctx = request.getContextPath();

        YearMonth target = YearMonth.now();

        try {
            YearMonth requestedTarget = parseYearMonth(
                    request.getParameter("year"), request.getParameter("month"));
            if (requestedTarget != null) target = requestedTarget;

            if (currentUser == null) {
                handleImportFailure(session, response, ctx, target,
                        "Your session has expired. Please sign in and import again.");
                return;
            }

            Part filePart = request.getPart("sheet");
            if (filePart == null || filePart.getSize() == 0) {
                handleImportFailure(session, response, ctx, target,
                        "Please choose a .xlsx attendance file.");
                return;
            }
            String fileName = filePart.getSubmittedFileName();
            if (fileName == null || !fileName.toLowerCase().endsWith(".xlsx")) {
                handleImportFailure(session, response, ctx, target,
                        "Only .xlsx files are accepted.");
                return;
            }

            if (reportDAO.isMonthLockedForImport(target.getYear(), target.getMonthValue())) {
                handleImportFailure(session, response, ctx, target,
                        "Attendance for " + monthLabel(target)
                                + " has already been submitted for approval and is locked. "
                                + "You cannot import this period again.");
                return;
            }

            List<Employee> allEmps = employeeDAO.findAttendanceActive();
            if (allEmps.isEmpty()) {
                handleImportFailure(session, response, ctx, target,
                        "No active attendance employees were found in the system.");
                return;
            }

            AttendanceImportService.Result result;
            try (InputStream in = filePart.getInputStream()) {
                java.util.List<XlsxReader.Sheet> sheets = XlsxReader.readAllSheets(in);
                AttendanceImportService importer = new AttendanceImportService();
                result = importer.importAllSheets(sheets, allEmps, target,
                        currentUser.getUserId(), /*overwrite*/ false);
            }

            if (result.hasErrors()) {
                handleImportFailure(session, response, ctx, target,
                        importValidationMessage(result));
                return;
            }

            session.setAttribute("importMessage", importSuccessMessage(target, result));
            response.sendRedirect(importRedirect(ctx, target));
        } catch (IllegalStateException ex) {
            handleImportException(session, response, ctx, target,
                    "The uploaded file is too large. Please use a .xlsx file under 10 MB.", ex);
        } catch (ServletException ex) {
            handleImportException(session, response, ctx, target,
                    "The uploaded form could not be parsed.", ex);
        } catch (IOException ex) {
            handleImportException(session, response, ctx, target,
                    "The Excel file could not be read.", ex);
        } catch (SQLException ex) {
            handleImportException(session, response, ctx, target,
                    "The attendance data could not be saved to the database.", ex);
        } catch (RuntimeException ex) {
            handleImportException(session, response, ctx, target,
                    "An unexpected import error occurred.", ex);
        }
    }

    private String importSuccessMessage(YearMonth target, AttendanceImportService.Result result) {
        StringBuilder msg = new StringBuilder();
        msg.append("Import ").append(target.getMonthValue()).append("/").append(target.getYear())
           .append(": added ").append(result.inserted).append(" record(s)");
        if (result.skippedExisting > 0) {
            msg.append(", skipped ").append(result.skippedExisting).append(" existing record(s)");
        }
        msg.append(".");
        appendWarnings(msg, result);
        return msg.toString();
    }

    private String importValidationMessage(AttendanceImportService.Result result) {
        StringBuilder msg = new StringBuilder("Validation failed: ");
        appendLimitedMessages(msg, result.errors, 5);
        appendWarnings(msg, result);
        return msg.toString();
    }

    private void appendWarnings(StringBuilder msg, AttendanceImportService.Result result) {
        if (!result.warnings.isEmpty()) {
            msg.append(" Warnings: ");
            appendLimitedMessages(msg, result.warnings, 3);
        }
    }

    private void appendLimitedMessages(StringBuilder msg, List<String> messages, int limit) {
        int show = Math.min(limit, messages.size());
        for (int i = 0; i < show; i++) {
            if (i > 0) msg.append(" ");
            msg.append(messages.get(i));
        }
        if (messages.size() > show) {
            msg.append(" (+").append(messages.size() - show).append(" more)");
        }
    }

    private void handleImportException(HttpSession session, HttpServletResponse response,
                                       String ctx, YearMonth target, String userMessage,
                                       Exception ex) throws IOException {
        log("Attendance import failed for " + target + ": " + userMessage, ex);
        String detail = rootCauseMessage(ex);
        handleImportFailure(session, response, ctx, target,
                detail.isBlank() ? userMessage : userMessage + " Reason: " + detail);
    }

    private void handleImportFailure(HttpSession session, HttpServletResponse response,
                                     String ctx, YearMonth target, String reason)
            throws IOException {
        session.setAttribute("importError", "Import failed for "
                + monthLabel(target) + ". " + reason);
        response.sendRedirect(importRedirect(ctx, target));
    }

    private String importRedirect(String ctx, YearMonth target) {
        return ctx + "/attendance?year=" + target.getYear()
                + "&month=" + target.getMonthValue();
    }

    private String rootCauseMessage(Throwable ex) {
        Throwable cur = ex;
        while (cur.getCause() != null) cur = cur.getCause();
        String msg = cur.getMessage();
        return msg == null ? cur.getClass().getSimpleName() : msg.trim();
    }

    private void handleEditForm(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        AttendanceRecord existing = loadOwnedRecordOrError(request, response);
        if (existing == null) return;

        if (existing.getVerificationStatus() == VerificationStatus.Verified) {
            response.sendRedirect(request.getContextPath()
                    + "/attendance?error=already-verified");
            return;
        }

        request.setAttribute("record", existing);
        request.setAttribute("statuses", AttendanceStatus.values());
        request.getRequestDispatcher("/views/attendance/edit-attendance.jsp")
               .forward(request, response);
    }

    private void handleEdit(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        AttendanceRecord existing = loadOwnedRecordOrError(request, response);
        if (existing == null) return;

        if (existing.getVerificationStatus() == VerificationStatus.Verified) {
            response.sendRedirect(request.getContextPath()
                    + "/attendance?error=already-verified");
            return;
        }

        String workDateStr  = trim(request.getParameter("workDate"));
        String statusStr    = request.getParameter("attendanceStatus");
        String overtimeStr  = trim(request.getParameter("overtimeHours"));
        String lateMinutesStr = trim(request.getParameter("lateMinutes"));
        String note         = trim(request.getParameter("note"));

        if (workDateStr.isEmpty() || statusStr == null || statusStr.isBlank()) {
            forwardEditForm(request, response, existing,
                    "Please fill in all required fields (date, status).");
            return;
        }

        LocalDate workDate;
        try { workDate = LocalDate.parse(workDateStr); }
        catch (DateTimeParseException ex) {
            forwardEditForm(request, response, existing, "Invalid work date.");
            return;
        }

        if (workDate.isAfter(LocalDate.now())) {
            forwardEditForm(request, response, existing, "Work date cannot be in the future.");
            return;
        }

        AttendanceStatus status;
        try { status = AttendanceStatus.valueOf(statusStr); }
        catch (IllegalArgumentException ex) {
            forwardEditForm(request, response, existing, "Invalid attendance status.");
            return;
        }
        existing.setAttendanceStatus(status);

        int lateMinutes = 0;
        BigDecimal latePenaltyAmount = BigDecimal.ZERO;
        if (status == AttendanceStatus.Late) {
            try {
                lateMinutes = Integer.parseInt(lateMinutesStr);
                if (lateMinutes <= 0 || lateMinutes > 1440) {
                    forwardEditForm(request, response, existing,
                            "Late minutes must be between 1 and 1440.");
                    return;
                }
                latePenaltyAmount = calculateLatePenalty(lateMinutes);
                existing.setLateMinutes(lateMinutes);
                existing.setLatePenaltyAmount(latePenaltyAmount);
            } catch (NumberFormatException ex) {
                forwardEditForm(request, response, existing,
                        "Please enter the number of late minutes.");
                return;
            }
        }

        BigDecimal overtimeHours = BigDecimal.ZERO;
        if (!overtimeStr.isEmpty()) {
            try {
                overtimeHours = new BigDecimal(overtimeStr);
                if (overtimeHours.signum() < 0) {
                    forwardEditForm(request, response, existing,
                            "Overtime hours cannot be negative.");
                    return;
                }
                existing.setOvertimeHours(overtimeHours);
            } catch (NumberFormatException ex) {
                forwardEditForm(request, response, existing, "Invalid overtime hours.");
                return;
            }
        }

        if (note.length() > 255) {
            forwardEditForm(request, response, existing,
                    "Note must be 255 characters or fewer.");
            return;
        }

        if (!workDate.isEqual(existing.getWorkDate())
                && attendanceDAO.existsByEmployeeAndDate(existing.getEmployeeId(), workDate)) {
            forwardEditForm(request, response, existing,
                    "Another attendance record for this employee on this date already exists.");
            return;
        }

        existing.setWorkDate(workDate);
        existing.setOvertimeHours(overtimeHours);
        existing.setAttendanceStatus(status);
        existing.setLateMinutes(lateMinutes);
        existing.setLatePenaltyAmount(latePenaltyAmount);
        existing.setNote(note);

        attendanceDAO.update(existing);
        response.sendRedirect(request.getContextPath()
                + "/attendance?action=employeeDetail&employeeId=" + existing.getEmployeeId());
    }

    private void forwardEditForm(HttpServletRequest request, HttpServletResponse response,
                                 AttendanceRecord record, String error)
            throws ServletException, IOException {
        request.setAttribute("error", error);
        request.setAttribute("record", record);
        request.setAttribute("statuses", AttendanceStatus.values());
        request.getRequestDispatcher("/views/attendance/edit-attendance.jsp")
               .forward(request, response);
    }

    private BigDecimal calculateLatePenalty(int lateMinutes) {
        if (lateMinutes < 5) return BigDecimal.ZERO;
        if (lateMinutes <= 30) return new BigDecimal("50000");
        if (lateMinutes <= 60) return new BigDecimal("100000");
        return new BigDecimal("200000");
    }

    private void handleVerify(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {

        AttendanceRecord existing = loadOwnedRecordOrError(request, response);
        if (existing == null) return;

        if (existing.getVerificationStatus() == VerificationStatus.Verified) {
            response.sendRedirect(request.getContextPath()
                    + "/attendance?error=already-verified");
            return;
        }

        User currentUser = getCurrentUser(request);
        attendanceDAO.verify(existing.getAttendanceId(), currentUser.getUserId());
        response.sendRedirect(request.getContextPath()
                + "/attendance?action=employeeDetail&employeeId=" + existing.getEmployeeId());
    }

    /**
     * A manager confirms every Pending employee record in their department,
     * including their own attendance, and prepares monthly reports for HR Staff.
     */
    private void handleConfirmToHr(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {

        User currentUser = getCurrentUser(request);
        String ctx = request.getContextPath();
        HttpSession session = request.getSession(true);

        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "";
        if (!"MANAGER".equalsIgnoreCase(roleName)) {
            session.setAttribute("importError", "Only department managers can confirm this attendance.");
            response.sendRedirect(ctx + "/attendance");
            return;
        }

        YearMonth month = parseYearMonth(request.getParameter("year"), request.getParameter("month"));
        if (month == null) month = YearMonth.now();
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        int mgrId = currentUser.getUserId();
        if (reportDAO.hasHrManagerApprovedMonthForManager(
                month.getYear(), month.getMonthValue(), mgrId)) {
            session.setAttribute("importError",
                    "Attendance for " + monthLabel(month)
                            + " for your department has already been approved by HR Manager and is closed.");
            response.sendRedirect(ctx + "/attendance?year=" + month.getYear()
                    + "&month=" + month.getMonthValue());
            return;
        }

        int pending = attendanceDAO.countPendingByManager(mgrId, monthStart, monthEnd);
        if (pending <= 0) {
            session.setAttribute("importError",
                    "There are no pending attendance records for " + monthLabel(month) + " to confirm.");
            response.sendRedirect(ctx + "/attendance?year=" + month.getYear()
                    + "&month=" + month.getMonthValue());
            return;
        }

        // 1) Verify any still-pending records for the selected month.
        attendanceDAO.verifyAllPendingByManager(mgrId, mgrId, monthStart, monthEnd);

        // 2) Aggregate the selected month for employees and the manager.
        List<AttendanceRecordDAO.MonthlySummary> summaries =
                attendanceDAO.aggregateMonthByManager(mgrId, month.getYear(), month.getMonthValue());

        if (summaries.isEmpty()) {
            session.setAttribute("importError",
                    "There is no verified attendance for " + month + " to send.");
            response.sendRedirect(ctx + "/attendance");
            return;
        }

        int reports = 0;
        for (AttendanceRecordDAO.MonthlySummary s : summaries) {
            AttendanceReport rpt = new AttendanceReport();
            rpt.setEmployeeId(s.employeeId);
            rpt.setManagerId(mgrId);
            rpt.setDepartmentId(s.departmentId);
            rpt.setReportMonth(month.getMonthValue());
            rpt.setReportYear(month.getYear());
            rpt.setActualWorkingDays(java.math.BigDecimal.valueOf(s.actualWorkingDays));
            rpt.setPaidLeaveDays(java.math.BigDecimal.valueOf(s.paidLeaveDays));
            rpt.setUnpaidLeaveDays(java.math.BigDecimal.valueOf(s.unpaidLeaveDays));
            rpt.setMaternityLeaveDays(java.math.BigDecimal.valueOf(s.maternityLeaveDays));
            rpt.setOvertimeHours(s.overtimeHours);
            rpt.setLatePenaltyAmount(s.latePenaltyAmount);
            if (reportDAO.upsertSubmitted(rpt)) reports++;
        }

        session.setAttribute("importMessage",
                "Manager confirmed " + reports + " department attendance report(s) for "
                        + monthLabel(month) + ". HR Staff can now submit them to HR Manager.");
        response.sendRedirect(ctx + "/attendance");
    }

    private void handleConfirmHrDepartment(HttpServletRequest request,
                                           HttpServletResponse response)
            throws SQLException, IOException {
        User currentUser = getCurrentUser(request);
        HttpSession session = request.getSession(true);
        String ctx = request.getContextPath();
        String roleName = currentUser != null && currentUser.getRole() != null
                ? currentUser.getRole().getRoleName() : "";
        if (!"HR_MANAGER".equalsIgnoreCase(roleName)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Department hrDepartment = findDepartmentByCode("HR");
        if (hrDepartment == null) {
            session.setAttribute("importError", "Human Resources department was not found.");
            response.sendRedirect(ctx + "/attendance");
            return;
        }
        if (hrDepartment.getManagerId() == null
                || hrDepartment.getManagerId() != currentUser.getUserId()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        YearMonth month = parseYearMonth(request.getParameter("year"), request.getParameter("month"));
        if (month == null) month = YearMonth.now();
        int departmentId = hrDepartment.getDepartmentId();
        String redirect = ctx + "/attendance?deptId=" + departmentId
                + "&year=" + month.getYear() + "&month=" + month.getMonthValue();

        if (reportDAO.hasHrManagerApprovedMonth(
                month.getYear(), month.getMonthValue(), departmentId)) {
            session.setAttribute("importError",
                    "Human Resources attendance for " + monthLabel(month)
                            + " has already been confirmed and is closed.");
            response.sendRedirect(redirect);
            return;
        }

        int verified = attendanceDAO.verifyPendingByDepartmentMonth(
                currentUser.getUserId(), month.getYear(), month.getMonthValue(), departmentId);
        List<AttendanceRecordDAO.MonthlySummary> summaries =
                attendanceDAO.aggregateMonthByDepartment(
                        month.getYear(), month.getMonthValue(), departmentId);
        if (summaries.isEmpty()) {
            session.setAttribute("importError",
                    "No Human Resources attendance records are available for " + monthLabel(month) + ".");
            response.sendRedirect(redirect);
            return;
        }

        int reports = 0;
        for (AttendanceRecordDAO.MonthlySummary summary : summaries) {
            AttendanceReport report = buildReport(
                    summary, currentUser.getUserId(), month.getYear(), month.getMonthValue());
            if (reportDAO.upsertApprovedByHrManager(
                    report, currentUser.getUserId(),
                    "HR attendance reviewed and confirmed by HR Manager")) {
                reports++;
            }
        }

        session.setAttribute("importMessage",
                "Confirmed " + verified + " pending Human Resources attendance record(s) and approved "
                        + reports + " monthly report(s). HR Staff can now calculate payroll.");
        response.sendRedirect(redirect);
    }

    private AttendanceReport buildReport(AttendanceRecordDAO.MonthlySummary summary,
                                         int managerUserId, int year, int month) {
        AttendanceReport report = new AttendanceReport();
        report.setEmployeeId(summary.employeeId);
        report.setManagerId(managerUserId);
        report.setDepartmentId(summary.departmentId);
        report.setReportMonth(month);
        report.setReportYear(year);
        report.setActualWorkingDays(BigDecimal.valueOf(summary.actualWorkingDays));
        report.setPaidLeaveDays(BigDecimal.valueOf(summary.paidLeaveDays));
        report.setUnpaidLeaveDays(BigDecimal.valueOf(summary.unpaidLeaveDays));
        report.setMaternityLeaveDays(BigDecimal.valueOf(summary.maternityLeaveDays));
        report.setOvertimeHours(summary.overtimeHours);
        report.setLatePenaltyAmount(summary.latePenaltyAmount);
        return report;
    }

    private AttendanceRecord loadOwnedRecordOrError(HttpServletRequest request,
                                                    HttpServletResponse response)
            throws SQLException, IOException {

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/attendance");
            return null;
        }

        int attendanceId;
        try { attendanceId = Integer.parseInt(idParam); }
        catch (NumberFormatException ex) {
            response.sendRedirect(request.getContextPath() + "/attendance");
            return null;
        }

        AttendanceRecord existing = attendanceDAO.findById(attendanceId);
        if (existing == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        User currentUser = getCurrentUser(request);
        boolean ownsThis = existing.getEmployeeManagerUserId() != null
                && existing.getEmployeeManagerUserId() == currentUser.getUserId();
        Employee attendanceEmployee = employeeDAO.findById(existing.getEmployeeId());
        boolean isOwnAttendance = attendanceEmployee != null
                && attendanceEmployee.getUserId() == currentUser.getUserId();
        String roleName = currentUser.getRole() == null ? "" : currentUser.getRole().getRoleName();
        boolean managesDepartment = ("MANAGER".equalsIgnoreCase(roleName)
                || "HR_MANAGER".equalsIgnoreCase(roleName))
                && attendanceEmployee != null
                && managesAttendanceDepartment(
                        currentUser.getUserId(), attendanceEmployee.getDepartmentId());

        if (!ownsThis && !isOwnAttendance && !managesDepartment) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        return existing;
    }

    private User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("currentUser");
    }

    private boolean hasPermission(HttpServletRequest request, String permCode) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        List<?> perms = (List<?>) session.getAttribute("permissions");
        return perms != null && perms.contains(permCode);
    }

    private Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s); } catch (NumberFormatException ex) { return null; }
    }

    private int parseIntOr(String s, int dflt) {
        if (s == null || s.isBlank()) return dflt;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ex) { return dflt; }
    }

    private LocalDate parseDateOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s); } catch (DateTimeParseException ex) { return null; }
    }

    private YearMonth parseYearMonth(String yearStr, String monthStr) {
        int year = parseIntOr(yearStr, -1);
        int month = parseIntOr(monthStr, -1);
        if (year < 2000 || year > 2100 || month < 1 || month > 12) return null;
        return YearMonth.of(year, month);
    }

    private String monthLabel(YearMonth month) {
        return month.getMonth().getDisplayName(java.time.format.TextStyle.FULL,
                java.util.Locale.ENGLISH) + " " + month.getYear();
    }

    private boolean containsDepartment(List<Department> departments, Integer departmentId) {
        if (departmentId == null || departments == null) return false;
        for (Department d : departments) {
            if (d.getDepartmentId() == departmentId) return true;
        }
        return false;
    }

    private Department findDepartment(List<Department> departments, Integer departmentId) {
        if (departmentId == null || departments == null) return null;
        for (Department department : departments) {
            if (department.getDepartmentId() == departmentId) return department;
        }
        return null;
    }

    private Department findDepartmentByCode(String departmentCode) throws SQLException {
        for (Department department : departmentDAO.findAttendanceDepartments()) {
            if (departmentCode.equalsIgnoreCase(department.getDepartmentCode())) return department;
        }
        return null;
    }

    private Department findAttendanceDepartmentManagedBy(int managerUserId) throws SQLException {
        for (Department department : departmentDAO.findAttendanceDepartments()) {
            if (department.getManagerId() != null
                    && department.getManagerId() == managerUserId) {
                return department;
            }
        }
        return null;
    }

    private boolean managesAttendanceDepartment(int managerUserId, int departmentId)
            throws SQLException {
        Department department = departmentDAO.findById(departmentId);
        return department != null
                && department.isActive()
                && department.getManagerId() != null
                && department.getManagerId() == managerUserId
                && !"IT".equalsIgnoreCase(department.getDepartmentCode());
    }

    private void attachPayrollTaskSummary(HttpServletRequest request, String roleName)
            throws SQLException {
        PayrollTaskSummary summary = null;
        boolean approvalTask = false;
        if ("HR_STAFF".equalsIgnoreCase(roleName)) {
            summary = payrollPeriodDAO.findHrStaffTaskSummary();
        } else if ("HR_MANAGER".equalsIgnoreCase(roleName)) {
            summary = payrollPeriodDAO.findHrManagerTaskSummary();
            approvalTask = true;
        }
        request.setAttribute("payrollTaskSummary", summary);
        request.setAttribute("payrollTaskApproval", approvalTask);
    }

    private List<Integer> buildYearOptions(int selectedYear) {
        List<Integer> years = new ArrayList<>();
        for (int y = selectedYear - 2; y <= selectedYear + 1; y++) years.add(y);
        return years;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
