package controller;

import dao.AttendanceRecordDAO;
import dao.AttendanceReportDAO;
import dao.EmployeeDAO;
import model.AttendanceRecord;
import model.AttendanceRecord.AttendanceStatus;
import model.AttendanceRecord.VerificationStatus;
import model.AttendanceReport;
import model.Employee;
import model.User;
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
                    if (!hasPermission(request, "VERIFY_STAFF_ATTENDANCE")) {
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
                case "sendToHr" -> {
                    if (!hasPermission(request, "VERIFY_STAFF_ATTENDANCE")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleSendToHr(request, response);
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

        boolean managerScope = "MANAGER".equalsIgnoreCase(roleName);
        boolean orgWide      = "ADMIN".equalsIgnoreCase(roleName)
                            || "HR_STAFF".equalsIgnoreCase(roleName)
                            || "HR_MANAGER".equalsIgnoreCase(roleName);

        Integer employeeIdFilter = parseIntOrNull(request.getParameter("employeeId"));
        LocalDate fromDate = parseDateOrNull(request.getParameter("fromDate"));
        LocalDate toDate   = parseDateOrNull(request.getParameter("toDate"));

        // Attendance never exists in the future: clamp filter dates to today.
        LocalDate today = LocalDate.now();
        if (fromDate != null && fromDate.isAfter(today)) fromDate = today;
        if (toDate != null && toDate.isAfter(today)) toDate = today;

        List<Employee> scopeEmployees;
        List<AttendanceRecord> records;

        int totalRecords;
        int totalPages;
        int page = 1;
        String pageParam = request.getParameter("page");
        if (pageParam != null && !pageParam.isBlank()) {
            try { page = Integer.parseInt(pageParam); } catch (NumberFormatException ignored) {}
        }

        if (managerScope) {
            scopeEmployees = employeeDAO.findByManagerUserId(currentUser.getUserId());
            totalRecords = attendanceDAO.countByManagerScope(currentUser.getUserId(), employeeIdFilter, fromDate, toDate);
            totalPages = Math.max(1, (int) Math.ceil(totalRecords / (double) PAGE_SIZE));
            if (page < 1) page = 1;
            if (page > totalPages) page = totalPages;
            records = attendanceDAO.findByManagerScope(currentUser.getUserId(), employeeIdFilter,
                    fromDate, toDate, (page - 1) * PAGE_SIZE, PAGE_SIZE);
        } else if (orgWide) {
            scopeEmployees = employeeDAO.findAllActive();
            totalRecords = attendanceDAO.countAll(employeeIdFilter, fromDate, toDate);
            totalPages = Math.max(1, (int) Math.ceil(totalRecords / (double) PAGE_SIZE));
            if (page < 1) page = 1;
            if (page > totalPages) page = totalPages;
            records = attendanceDAO.findAll(employeeIdFilter, fromDate, toDate,
                    (page - 1) * PAGE_SIZE, PAGE_SIZE);
        } else {
            Employee me = employeeDAO.findByUserId(currentUser.getUserId());
            scopeEmployees = java.util.Collections.emptyList();
            if (me != null) {
                totalRecords = attendanceDAO.countByEmployeeId(me.getEmployeeId(), fromDate, toDate);
                totalPages = Math.max(1, (int) Math.ceil(totalRecords / (double) PAGE_SIZE));
                if (page < 1) page = 1;
                if (page > totalPages) page = totalPages;
                records = attendanceDAO.findByEmployeeId(me.getEmployeeId(), fromDate, toDate,
                        (page - 1) * PAGE_SIZE, PAGE_SIZE);
            } else {
                totalRecords = 0;
                totalPages = 1;
                page = 1;
                records = java.util.Collections.emptyList();
            }
        }

        request.setAttribute("records", records);
        request.setAttribute("scopeEmployees", scopeEmployees);
        request.setAttribute("managerScope", managerScope);
        request.setAttribute("orgWide", orgWide);
        request.setAttribute("employeeIdFilter", employeeIdFilter);
        request.setAttribute("fromDate", fromDate);
        request.setAttribute("toDate", toDate);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalRecords", totalRecords);

        // Import always targets the current month; show it (and the manager's
        // department) in the import dialog.
        YearMonth thisMonth = YearMonth.now();
        request.setAttribute("importYear", thisMonth.getYear());
        request.setAttribute("importMonth", thisMonth.getMonthValue());
        request.setAttribute("importMonthLabel",
                thisMonth.getMonth().getDisplayName(java.time.format.TextStyle.FULL,
                        java.util.Locale.ENGLISH) + " " + thisMonth.getYear());
        if (managerScope && !scopeEmployees.isEmpty()) {
            // 1 manager : 1 department, so any team member's department is the dept.
            request.setAttribute("managerDeptName", scopeEmployees.get(0).getDepartmentName());
        }
        if (managerScope) {
            request.setAttribute("pendingCount",
                    attendanceDAO.countPendingByManager(currentUser.getUserId()));
        }

        // Flash messages from a preceding import (PRG pattern).
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
     * Import a monthly attendance sheet (.xlsx) uploaded by a manager.
     * Only managers may import, and only for employees they manage.
     */
    private void handleImport(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        User currentUser = getCurrentUser(request);
        HttpSession session = request.getSession(true);
        String ctx = request.getContextPath();

        // Import always targets the CURRENT month (no past/future selection).
        YearMonth target = YearMonth.now();

        Part filePart = request.getPart("sheet");
        if (filePart == null || filePart.getSize() == 0) {
            session.setAttribute("importError", "Please choose a file to import.");
            response.sendRedirect(ctx + "/attendance");
            return;
        }
        String fileName = filePart.getSubmittedFileName();
        if (fileName == null || !fileName.toLowerCase().endsWith(".xlsx")) {
            session.setAttribute("importError", "Only .xlsx files are accepted.");
            response.sendRedirect(ctx + "/attendance");
            return;
        }

        // Only the manager's own team is writable.
        List<Employee> scope = employeeDAO.findByManagerUserId(currentUser.getUserId());
        if (scope.isEmpty()) {
            session.setAttribute("importError",
                    "You do not manage any employees to import attendance for.");
            response.sendRedirect(ctx + "/attendance");
            return;
        }

        AttendanceImportService.Result result;
        try (InputStream in = filePart.getInputStream()) {
            XlsxReader.Sheet sheet = XlsxReader.readFirstSheet(in);
            AttendanceImportService importer = new AttendanceImportService();
            result = importer.importSheet(sheet, scope, target,
                    currentUser.getUserId(), /*overwrite*/ false);
        } catch (IOException ex) {
            session.setAttribute("importError",
                    "Could not read the Excel file: " + ex.getMessage());
            response.sendRedirect(ctx + "/attendance");
            return;
        }

        StringBuilder msg = new StringBuilder();
        msg.append("Import ").append(target.getMonthValue()).append("/").append(target.getYear())
           .append(": added ").append(result.inserted).append(" record(s)");
        if (result.skippedExisting > 0)
            msg.append(", skipped ").append(result.skippedExisting).append(" existing record(s)");
        msg.append(".");
        if (!result.warnings.isEmpty())
            msg.append(" Warnings: ").append(result.warnings.size()).append(" cell(s).");

        if (result.hasErrors()) {
            StringBuilder err = new StringBuilder(msg).append(" Errors: ");
            int show = Math.min(5, result.errors.size());
            for (int i = 0; i < show; i++) err.append(result.errors.get(i)).append(" ");
            if (result.errors.size() > show)
                err.append("(+").append(result.errors.size() - show).append(" more)");
            session.setAttribute("importError", err.toString());
        } else {
            session.setAttribute("importMessage", msg.toString());
        }
        response.sendRedirect(ctx + "/attendance");
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

        BigDecimal overtimeHours = BigDecimal.ZERO;
        if (!overtimeStr.isEmpty()) {
            try {
                overtimeHours = new BigDecimal(overtimeStr);
                if (overtimeHours.signum() < 0) {
                    forwardEditForm(request, response, existing,
                            "Overtime hours cannot be negative.");
                    return;
                }
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
        existing.setNote(note);

        attendanceDAO.update(existing);
        response.sendRedirect(request.getContextPath() + "/attendance?updated=success");
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
        response.sendRedirect(request.getContextPath() + "/attendance?verified=success");
    }

    /**
     * "Send to HR Staff": bulk-verify every Pending record of the manager's team,
     * then build one monthly attendance_report per employee (current month) and
     * submit it to HR Staff. Managers only.
     */
    private void handleSendToHr(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {

        User currentUser = getCurrentUser(request);
        String ctx = request.getContextPath();
        HttpSession session = request.getSession(true);

        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "";
        if (!"MANAGER".equalsIgnoreCase(roleName)) {
            session.setAttribute("importError", "Only managers can send attendance to HR Staff.");
            response.sendRedirect(ctx + "/attendance");
            return;
        }

        int mgrId = currentUser.getUserId();
        // 1) Verify any still-pending records.
        attendanceDAO.verifyAllPendingByManager(mgrId, mgrId, null, null);

        // 2) Aggregate the CURRENT month and submit one report per employee.
        YearMonth month = YearMonth.now();
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
            rpt.setOvertimeHours(s.overtimeHours);
            if (reportDAO.upsertSubmitted(rpt)) reports++;
        }

        session.setAttribute("importMessage",
                "Sent to HR Staff: " + reports + " attendance report(s) submitted for "
                        + month.getMonth().getDisplayName(java.time.format.TextStyle.FULL,
                                java.util.Locale.ENGLISH) + " " + month.getYear() + ".");
        response.sendRedirect(ctx + "/attendance");
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

        if (!ownsThis) {
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

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
