package controller;

import dao.AttendanceReportDAO;
import dao.AttendanceRecordDAO;
import model.AttendanceReport;
import model.User;
import service.AttendanceAutoConfirmService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * HR Staff screen: "View Attendance Report".
 * Lists the monthly attendance reports submitted by managers, for a chosen
 * month (defaults to the current month).
 */
@WebServlet(name = "AttendanceReportServlet", urlPatterns = {"/attendance-report"})
public class AttendanceReportServlet extends HttpServlet {

    private static final int PAGE_SIZE = 10;

    private final AttendanceReportDAO reportDAO = new AttendanceReportDAO();
    private final AttendanceRecordDAO attendanceDAO = new AttendanceRecordDAO();
    private final AttendanceAutoConfirmService autoConfirmService = new AttendanceAutoConfirmService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!hasPermission(request, "VIEW_ATTENDANCE_REPORT")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        YearMonth now = YearMonth.now();
        int year  = parseIntOr(request.getParameter("year"),  now.getYear());
        int month = parseIntOr(request.getParameter("month"), now.getMonthValue());
        if (month < 1 || month > 12) month = now.getMonthValue();

        try {
            autoConfirmService.runDueAutoConfirm();

            User currentUser = getCurrentUser(request);
            String roleName = currentUser != null && currentUser.getRole() != null
                    ? currentUser.getRole().getRoleName() : "";
            Integer managerUserId = "MANAGER".equalsIgnoreCase(roleName)
                    ? currentUser.getUserId() : null;
            boolean hrStaffScope = "HR_STAFF".equalsIgnoreCase(roleName);
            boolean hrManagerScope = "HR_MANAGER".equalsIgnoreCase(roleName);

            int totalReports = reportDAO.countSubmittedByMonth(year, month, null, managerUserId);
            int readyToSubmitCount = hrStaffScope
                    ? reportDAO.countReadyForHrManagerSubmission(year, month, null)
                        + attendanceDAO.countPendingHrManagedByMonth(year, month)
                    : 0;
            int pendingManagerConfirmationCount = hrStaffScope
                    ? attendanceDAO.countPendingManagerConfirmationByMonth(year, month)
                    : 0;
            int pendingHrManagerApprovalCount = hrManagerScope
                    ? reportDAO.countPendingHrManagerByMonth(year, month, null)
                    : 0;
            if (hrManagerScope) {
                totalReports = reportDAO.countForHrManagerByMonth(year, month, null);
            }
            int totalPages = Math.max(1, (int) Math.ceil(totalReports / (double) PAGE_SIZE));
            int page = parsePageParam(request.getParameter("page"), totalPages);
            int offset = (page - 1) * PAGE_SIZE;

            List<AttendanceReport> reports = hrManagerScope
                    ? reportDAO.findForHrManagerByMonthPage(year, month, null, offset, PAGE_SIZE)
                    : reportDAO.findSubmittedByMonthPage(year, month, null, managerUserId, offset, PAGE_SIZE);
            request.setAttribute("reports", reports);
            request.setAttribute("selectedYear", year);
            request.setAttribute("selectedMonth", month);
            request.setAttribute("currentPage", page);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("totalReports", totalReports);
            request.setAttribute("managerScope", managerUserId != null);
            request.setAttribute("hrStaffScope", hrStaffScope);
            request.setAttribute("hrManagerScope", hrManagerScope);
            request.setAttribute("readyToSubmitCount", readyToSubmitCount);
            request.setAttribute("pendingManagerConfirmationCount", pendingManagerConfirmationCount);
            request.setAttribute("pendingHrManagerApprovalCount", pendingHrManagerApprovalCount);
            request.setAttribute("canSubmitToHrManager",
                    hrStaffScope && hasPermission(request, "VIEW_ATTENDANCE_REPORT"));
            request.setAttribute("canApproveAttendanceReport",
                    hrManagerScope && hasPermission(request, "APPROVE_ATTENDANCE_REPORT"));
            request.setAttribute("monthLabel",
                    YearMonth.of(year, month).getMonth()
                            .getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year);
            readFlash(request);
            request.getRequestDispatcher("/views/attendance/attendance-report-list.jsp")
                   .forward(request, response);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User currentUser = getCurrentUser(request);
        String roleName = currentUser != null && currentUser.getRole() != null
                ? currentUser.getRole().getRoleName() : "";
        String action = request.getParameter("action");
        if (action == null) action = "";

        try {
            switch (action) {
                case "submitToHrManager" -> {
                    if (!"HR_STAFF".equalsIgnoreCase(roleName)
                            || !hasPermission(request, "VIEW_ATTENDANCE_REPORT")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleSubmitToHrManager(request, response);
                }
                case "approve" -> {
                    if (!"HR_MANAGER".equalsIgnoreCase(roleName)
                            || !hasPermission(request, "APPROVE_ATTENDANCE_REPORT")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleDecision(request, response, AttendanceReport.Status.ApprovedByHrManager);
                }
                case "approveAll" -> {
                    if (!"HR_MANAGER".equalsIgnoreCase(roleName)
                            || !hasPermission(request, "APPROVE_ATTENDANCE_REPORT")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleApproveAll(request, response);
                }
                case "reject" -> {
                    if (!"HR_MANAGER".equalsIgnoreCase(roleName)
                            || !hasPermission(request, "APPROVE_ATTENDANCE_REPORT")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleDecision(request, response, AttendanceReport.Status.RejectedByHrManager);
                }
                default -> response.sendRedirect(request.getContextPath() + "/attendance-report");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void handleSubmitToHrManager(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        HttpSession session = request.getSession(true);
        String ctx = request.getContextPath();
        int year = parseIntOr(request.getParameter("year"), YearMonth.now().getYear());
        int month = parseIntOr(request.getParameter("month"), YearMonth.now().getMonthValue());

        User currentUser = getCurrentUser(request);
        autoConfirmService.runDueAutoConfirm();
        int prepared = prepareHrManagedReports(currentUser.getUserId(), year, month);
        int submitted = reportDAO.submitReadyToHrManager(year, month, null);
        if (submitted + prepared > 0) {
            session.setAttribute("attendanceReportMessage",
                    "Submitted " + (submitted + prepared) + " attendance report(s) to HR Manager.");
        } else {
            session.setAttribute("attendanceReportError",
                    "No attendance reports are ready to submit to HR Manager for this period.");
        }
        response.sendRedirect(reportRedirect(ctx, year, month));
    }

    private int prepareHrManagedReports(int hrStaffUserId, int year, int month) throws SQLException {
        if (reportDAO.hasHrManagerApprovedMonth(year, month)) {
            return 0;
        }
        attendanceDAO.verifyPendingHrManagedByMonth(hrStaffUserId, year, month);
        List<AttendanceRecordDAO.MonthlySummary> summaries =
                attendanceDAO.aggregateMonthByHrManagedRoles(year, month);
        int prepared = 0;
        for (AttendanceRecordDAO.MonthlySummary s : summaries) {
            AttendanceReport report = new AttendanceReport();
            report.setEmployeeId(s.employeeId);
            report.setManagerId(hrStaffUserId);
            report.setDepartmentId(s.departmentId);
            report.setReportMonth(month);
            report.setReportYear(year);
            report.setActualWorkingDays(BigDecimal.valueOf(s.actualWorkingDays));
            report.setPaidLeaveDays(BigDecimal.valueOf(s.paidLeaveDays));
            report.setUnpaidLeaveDays(BigDecimal.valueOf(s.unpaidLeaveDays));
            report.setMaternityLeaveDays(BigDecimal.valueOf(s.maternityLeaveDays));
            report.setOvertimeHours(s.overtimeHours);
            report.setLatePenaltyAmount(s.latePenaltyAmount);
            if (reportDAO.upsertPendingHrManager(report)) prepared++;
        }
        return prepared;
    }

    private void handleApproveAll(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        HttpSession session = request.getSession(true);
        User currentUser = getCurrentUser(request);
        String ctx = request.getContextPath();
        int year = parseIntOr(request.getParameter("year"), YearMonth.now().getYear());
        int month = parseIntOr(request.getParameter("month"), YearMonth.now().getMonthValue());

        int approved = reportDAO.approvePendingHrManagerByMonth(
                year, month, null, currentUser.getUserId());
        if (approved > 0) {
            session.setAttribute("attendanceReportMessage",
                    "Approved " + approved + " pending attendance report(s).");
        } else {
            session.setAttribute("attendanceReportError",
                    "No pending attendance reports are waiting for HR Manager approval.");
        }
        response.sendRedirect(reportRedirect(ctx, year, month));
    }

    private void handleDecision(HttpServletRequest request, HttpServletResponse response,
                                AttendanceReport.Status status)
            throws SQLException, IOException {
        HttpSession session = request.getSession(true);
        User currentUser = getCurrentUser(request);
        String ctx = request.getContextPath();
        int year = parseIntOr(request.getParameter("year"), YearMonth.now().getYear());
        int month = parseIntOr(request.getParameter("month"), YearMonth.now().getMonthValue());

        Integer reportId = parseIntOrNull(request.getParameter("reportId"));
        if (reportId == null) {
            session.setAttribute("attendanceReportError", "Invalid attendance report.");
            response.sendRedirect(reportRedirect(ctx, year, month));
            return;
        }

        String note = trim(request.getParameter("note"));
        if (status == AttendanceReport.Status.RejectedByHrManager && note.isEmpty()) {
            session.setAttribute("attendanceReportError", "Please enter a rejection reason.");
            response.sendRedirect(reportRedirect(ctx, year, month));
            return;
        }
        if (note.length() > 500) note = note.substring(0, 500);

        boolean updated = reportDAO.updateHrManagerDecision(
                reportId, status, currentUser.getUserId(), note.isEmpty() ? null : note);
        if (updated) {
            session.setAttribute("attendanceReportMessage",
                    status == AttendanceReport.Status.ApprovedByHrManager
                            ? "Attendance report approved."
                            : "Attendance report rejected.");
        } else {
            session.setAttribute("attendanceReportError",
                    "This attendance report is no longer pending HR Manager approval.");
        }
        response.sendRedirect(reportRedirect(ctx, year, month));
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

    private int parseIntOr(String s, int dflt) {
        if (s == null || s.isBlank()) return dflt;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ex) { return dflt; }
    }

    private Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ex) { return null; }
    }

    private int parsePageParam(String pageParam, int totalPages) {
        int page = parseIntOr(pageParam, 1);
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        return page;
    }

    private String reportRedirect(String ctx, int year, int month) {
        return ctx + "/attendance-report?year=" + year + "&month=" + month;
    }

    private void readFlash(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return;
        Object msg = session.getAttribute("attendanceReportMessage");
        Object err = session.getAttribute("attendanceReportError");
        if (msg != null) {
            request.setAttribute("attendanceReportMessage", msg);
            session.removeAttribute("attendanceReportMessage");
        }
        if (err != null) {
            request.setAttribute("attendanceReportError", err);
            session.removeAttribute("attendanceReportError");
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
