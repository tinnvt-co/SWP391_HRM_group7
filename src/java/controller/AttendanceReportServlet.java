package controller;

import dao.AttendanceReportDAO;
import dao.AttendanceRecordDAO;
import dao.DepartmentDAO;
import dao.PayrollPeriodDAO;
import model.AttendanceReport;
import model.Department;
import model.PayrollTaskSummary;
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
import java.util.Collections;
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
    private final DepartmentDAO departmentDAO = new DepartmentDAO();
    private final PayrollPeriodDAO payrollPeriodDAO = new PayrollPeriodDAO();
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
            boolean departmentScope = hrStaffScope || hrManagerScope;
            List<Department> departments = departmentScope
                    ? departmentDAO.findAttendanceDepartments()
                    : Collections.emptyList();
            Department selectedDepartment = departmentScope
                    ? resolveDepartment(departments, parseIntOrNull(request.getParameter("deptId")))
                    : null;
            Integer selectedDeptId = selectedDepartment == null
                    ? null : selectedDepartment.getDepartmentId();
            boolean selectedHrDepartment = selectedDepartment != null
                    && "HR".equalsIgnoreCase(selectedDepartment.getDepartmentCode());
            PayrollTaskSummary payrollTaskSummary = hrStaffScope
                    ? payrollPeriodDAO.findHrStaffTaskSummary()
                    : (hrManagerScope ? payrollPeriodDAO.findHrManagerTaskSummary() : null);

            boolean hasDepartment = !departmentScope || selectedDeptId != null;
            int totalReports = hasDepartment
                    ? reportDAO.countSubmittedByMonth(year, month, selectedDeptId, managerUserId)
                    : 0;
            int readyToSubmitCount = hrStaffScope && selectedDeptId != null
                    ? reportDAO.countReadyForHrManagerSubmission(year, month, selectedDeptId)
                    : 0;
            int pendingDepartmentConfirmationCount = (hrStaffScope || hrManagerScope)
                    && selectedDeptId != null
                    ? attendanceDAO.countPendingByDepartmentMonth(year, month, selectedDeptId)
                    : 0;
            int pendingHrManagerApprovalCount = hrManagerScope && selectedDeptId != null
                    ? reportDAO.countPendingHrManagerByMonth(year, month, selectedDeptId)
                    : 0;
            DepartmentTask attendanceTask = findAttendanceWorkflowTask(
                    hrStaffScope, hrManagerScope);
            DepartmentTask hrDepartmentConfirmationTask = hrManagerScope
                    ? findHrDepartmentConfirmationTask(departments, year, month)
                    : null;
            if (hrManagerScope && selectedDeptId != null) {
                totalReports = reportDAO.countForHrManagerByMonth(year, month, selectedDeptId);
            }
            int totalPages = Math.max(1, (int) Math.ceil(totalReports / (double) PAGE_SIZE));
            int page = parsePageParam(request.getParameter("page"), totalPages);
            int offset = (page - 1) * PAGE_SIZE;

            List<AttendanceReport> reports;
            if (!hasDepartment) {
                reports = Collections.emptyList();
            } else if (hrManagerScope) {
                reports = reportDAO.findForHrManagerByMonthPage(
                        year, month, selectedDeptId, offset, PAGE_SIZE);
            } else {
                reports = reportDAO.findSubmittedByMonthPage(
                        year, month, selectedDeptId, managerUserId, offset, PAGE_SIZE);
            }
            request.setAttribute("reports", reports);
            request.setAttribute("departments", departments);
            request.setAttribute("selectedDeptId", selectedDeptId);
            request.setAttribute("selectedDeptName",
                    selectedDepartment == null ? null : selectedDepartment.getDepartmentName());
            request.setAttribute("departmentScope", departmentScope);
            request.setAttribute("selectedYear", year);
            request.setAttribute("selectedMonth", month);
            request.setAttribute("currentPage", page);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("totalReports", totalReports);
            request.setAttribute("managerScope", managerUserId != null);
            request.setAttribute("hrStaffScope", hrStaffScope);
            request.setAttribute("hrManagerScope", hrManagerScope);
            request.setAttribute("readyToSubmitCount", readyToSubmitCount);
            request.setAttribute("pendingDepartmentConfirmationCount", pendingDepartmentConfirmationCount);
            request.setAttribute("pendingHrManagerApprovalCount", pendingHrManagerApprovalCount);
            request.setAttribute("attendanceTask", attendanceTask);
            request.setAttribute("hrDepartmentConfirmationTask", hrDepartmentConfirmationTask);
            request.setAttribute("selectedHrDepartment", selectedHrDepartment);
            request.setAttribute("payrollTaskSummary", payrollTaskSummary);
            request.setAttribute("payrollTaskApproval", hrManagerScope);
            request.setAttribute("canSubmitToHrManager",
                    hrStaffScope && selectedDeptId != null
                            && hasPermission(request, "VIEW_ATTENDANCE_REPORT"));
            request.setAttribute("canSubmitSelectedDepartment",
                    readyToSubmitCount > 0
                            && pendingDepartmentConfirmationCount == 0);
            request.setAttribute("canConfirmHrDepartmentAttendance",
                    hrManagerScope && selectedHrDepartment
                            && hasPermission(request, "APPROVE_ATTENDANCE_REPORT"));
            request.setAttribute("canApproveAttendanceReport",
                    hrManagerScope && selectedDeptId != null
                            && hasPermission(request, "APPROVE_ATTENDANCE_REPORT"));
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
                case "confirmHrDepartmentAttendance" -> {
                    if (!"HR_MANAGER".equalsIgnoreCase(roleName)
                            || !hasPermission(request, "APPROVE_ATTENDANCE_REPORT")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleConfirmHrDepartmentAttendance(request, response);
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
        Department department = findAttendanceDepartment(
                parseIntOrNull(request.getParameter("deptId")));
        if (department == null) {
            session.setAttribute("attendanceReportError",
                    "Please select a valid attendance department.");
            response.sendRedirect(reportRedirect(ctx, year, month, null));
            return;
        }
        int departmentId = department.getDepartmentId();

        autoConfirmService.runDueAutoConfirm();
        int pendingConfirmation = attendanceDAO.countPendingByDepartmentMonth(
                year, month, departmentId);
        if (pendingConfirmation > 0) {
            session.setAttribute("attendanceReportError",
                    "This department still has " + pendingConfirmation
                            + " attendance record(s) waiting for confirmation. "
                            + "All records must be confirmed before the department can be submitted.");
            response.sendRedirect(reportRedirect(ctx, year, month, departmentId));
            return;
        }

        int submitted = reportDAO.submitReadyToHrManager(year, month, departmentId);
        if (submitted > 0) {
            session.setAttribute("attendanceReportMessage",
                    "Submitted " + submitted + " "
                            + department.getDepartmentName()
                            + " attendance report(s) to HR Manager.");
        } else {
            session.setAttribute("attendanceReportError",
                    "No " + department.getDepartmentName()
                            + " attendance reports are ready to submit for this period.");
        }
        response.sendRedirect(reportRedirect(ctx, year, month, departmentId));
    }

    private void handleConfirmHrDepartmentAttendance(HttpServletRequest request,
                                                      HttpServletResponse response)
            throws SQLException, IOException {
        HttpSession session = request.getSession(true);
        User currentUser = getCurrentUser(request);
        String ctx = request.getContextPath();
        int year = parseIntOr(request.getParameter("year"), YearMonth.now().getYear());
        int month = parseIntOr(request.getParameter("month"), YearMonth.now().getMonthValue());
        Department department = findAttendanceDepartment(
                parseIntOrNull(request.getParameter("deptId")));
        if (department == null) {
            session.setAttribute("attendanceReportError",
                    "Please select a valid attendance department.");
            response.sendRedirect(reportRedirect(ctx, year, month, null));
            return;
        }
        if (!"HR".equalsIgnoreCase(department.getDepartmentCode())) {
            session.setAttribute("attendanceReportError",
                    "HR Manager can perform initial attendance confirmation only for the Human Resources department.");
            response.sendRedirect(reportRedirect(ctx, year, month, department.getDepartmentId()));
            return;
        }

        int departmentId = department.getDepartmentId();
        if (reportDAO.hasHrManagerApprovedMonth(year, month, departmentId)
                || reportDAO.countPendingHrManagerByMonth(year, month, departmentId) > 0) {
            session.setAttribute("attendanceReportError",
                    "Attendance reports for this department are already in final approval or have been approved.");
            response.sendRedirect(reportRedirect(ctx, year, month, departmentId));
            return;
        }

        int pending = attendanceDAO.countPendingByDepartmentMonth(year, month, departmentId);
        if (pending <= 0) {
            session.setAttribute("attendanceReportError",
                    "No Human Resources attendance records are waiting for confirmation.");
            response.sendRedirect(reportRedirect(ctx, year, month, departmentId));
            return;
        }

        int verified = attendanceDAO.verifyPendingByDepartmentMonth(
                currentUser.getUserId(), year, month, departmentId);
        int prepared = createDepartmentReports(
                currentUser.getUserId(), year, month, departmentId);
        session.setAttribute("attendanceReportMessage",
                "Confirmed " + verified + " Human Resources attendance record(s) and prepared "
                        + prepared + " report(s) for HR Staff.");
        response.sendRedirect(reportRedirect(ctx, year, month, departmentId));
    }

    private int createDepartmentReports(int confirmingUserId, int year, int month,
                                        int departmentId) throws SQLException {
        List<AttendanceRecordDAO.MonthlySummary> summaries =
                attendanceDAO.aggregateMonthByDepartment(year, month, departmentId);
        int prepared = 0;
        for (AttendanceRecordDAO.MonthlySummary s : summaries) {
            AttendanceReport report = new AttendanceReport();
            report.setEmployeeId(s.employeeId);
            report.setManagerId(confirmingUserId);
            report.setDepartmentId(s.departmentId);
            report.setReportMonth(month);
            report.setReportYear(year);
            report.setActualWorkingDays(BigDecimal.valueOf(s.actualWorkingDays));
            report.setPaidLeaveDays(BigDecimal.valueOf(s.paidLeaveDays));
            report.setUnpaidLeaveDays(BigDecimal.valueOf(s.unpaidLeaveDays));
            report.setMaternityLeaveDays(BigDecimal.valueOf(s.maternityLeaveDays));
            report.setOvertimeHours(s.overtimeHours);
            report.setLatePenaltyAmount(s.latePenaltyAmount);
            if (reportDAO.upsertSubmitted(report)) prepared++;
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
        Department department = findAttendanceDepartment(
                parseIntOrNull(request.getParameter("deptId")));
        if (department == null) {
            session.setAttribute("attendanceReportError",
                    "Please select a valid attendance department.");
            response.sendRedirect(reportRedirect(ctx, year, month, null));
            return;
        }
        int departmentId = department.getDepartmentId();

        int approved = reportDAO.approvePendingHrManagerByMonth(
                year, month, departmentId, currentUser.getUserId());
        if (approved > 0) {
            session.setAttribute("attendanceReportMessage",
                    "Approved " + approved + " pending "
                            + department.getDepartmentName() + " attendance report(s).");
        } else {
            session.setAttribute("attendanceReportError",
                    "No pending " + department.getDepartmentName()
                            + " attendance reports are waiting for approval.");
        }
        response.sendRedirect(reportRedirect(ctx, year, month, departmentId));
    }

    private void handleDecision(HttpServletRequest request, HttpServletResponse response,
                                AttendanceReport.Status status)
            throws SQLException, IOException {
        HttpSession session = request.getSession(true);
        User currentUser = getCurrentUser(request);
        String ctx = request.getContextPath();
        int year = parseIntOr(request.getParameter("year"), YearMonth.now().getYear());
        int month = parseIntOr(request.getParameter("month"), YearMonth.now().getMonthValue());
        Department department = findAttendanceDepartment(
                parseIntOrNull(request.getParameter("deptId")));
        if (department == null) {
            session.setAttribute("attendanceReportError",
                    "Please select a valid attendance department.");
            response.sendRedirect(reportRedirect(ctx, year, month, null));
            return;
        }
        int departmentId = department.getDepartmentId();

        Integer reportId = parseIntOrNull(request.getParameter("reportId"));
        if (reportId == null) {
            session.setAttribute("attendanceReportError", "Invalid attendance report.");
            response.sendRedirect(reportRedirect(ctx, year, month, departmentId));
            return;
        }

        AttendanceReport existing = reportDAO.findById(reportId);
        if (existing == null || existing.getDepartmentId() != departmentId) {
            session.setAttribute("attendanceReportError",
                    "The selected attendance report does not belong to this department.");
            response.sendRedirect(reportRedirect(ctx, year, month, departmentId));
            return;
        }

        String note = trim(request.getParameter("note"));
        if (status == AttendanceReport.Status.RejectedByHrManager && note.isEmpty()) {
            session.setAttribute("attendanceReportError", "Please enter a rejection reason.");
            response.sendRedirect(reportRedirect(ctx, year, month, departmentId));
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
        response.sendRedirect(reportRedirect(ctx, year, month, departmentId));
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

    private DepartmentTask findAttendanceWorkflowTask(boolean hrStaffScope,
                                                        boolean hrManagerScope)
            throws SQLException {
        if (!hrStaffScope && !hrManagerScope) return null;

        List<AttendanceReportDAO.DepartmentWorkflowTask> candidates = hrStaffScope
                ? reportDAO.findReadyDepartmentTasksForHrStaff()
                : reportDAO.findPendingDepartmentTasksForHrManager();
        DepartmentTask firstTask = null;
        int actionableTaskCount = 0;
        String actionLabel = hrStaffScope ? "Submit Department" : "Approve Department";

        for (AttendanceReportDAO.DepartmentWorkflowTask candidate : candidates) {
            if (hrStaffScope && attendanceDAO.countPendingByDepartmentMonth(
                    candidate.getYear(), candidate.getMonth(), candidate.getDepartmentId()) > 0) {
                continue;
            }
            actionableTaskCount++;
            if (firstTask == null) {
                firstTask = new DepartmentTask(
                        candidate.getDepartmentId(), candidate.getDepartmentName(),
                        candidate.getYear(), candidate.getMonth(), 0, actionLabel);
            }
        }

        if (firstTask != null) firstTask.setCount(actionableTaskCount);
        return firstTask;
    }

    private DepartmentTask findHrDepartmentConfirmationTask(List<Department> departments,
                                                              int year, int month)
            throws SQLException {
        for (Department department : departments) {
            if (!"HR".equalsIgnoreCase(department.getDepartmentCode())) continue;
            int count = attendanceDAO.countPendingByDepartmentMonth(
                    year, month, department.getDepartmentId());
            return count <= 0 ? null : new DepartmentTask(
                    department.getDepartmentId(), department.getDepartmentName(),
                    year, month, count, "Confirm HR Department Attendance");
        }
        return null;
    }

    private Department resolveDepartment(List<Department> departments, Integer departmentId) {
        if (departments == null || departments.isEmpty()) return null;
        if (departmentId != null) {
            for (Department department : departments) {
                if (department.getDepartmentId() == departmentId) return department;
            }
        }
        return departments.get(0);
    }

    private Department findAttendanceDepartment(Integer departmentId) throws SQLException {
        if (departmentId == null) return null;
        for (Department department : departmentDAO.findAttendanceDepartments()) {
            if (department.getDepartmentId() == departmentId) return department;
        }
        return null;
    }

    private String reportRedirect(String ctx, int year, int month, Integer departmentId) {
        String url = ctx + "/attendance-report?year=" + year + "&month=" + month;
        return departmentId == null ? url : url + "&deptId=" + departmentId;
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

    public static final class DepartmentTask {
        private final int departmentId;
        private final String departmentName;
        private final int year;
        private final int month;
        private int count;
        private final String actionLabel;

        DepartmentTask(int departmentId, String departmentName, int year, int month,
                       int count, String actionLabel) {
            this.departmentId = departmentId;
            this.departmentName = departmentName;
            this.year = year;
            this.month = month;
            this.count = count;
            this.actionLabel = actionLabel;
        }

        public int getDepartmentId() { return departmentId; }
        public String getDepartmentName() { return departmentName; }
        public int getYear() { return year; }
        public int getMonth() { return month; }
        public int getCount() { return count; }
        public String getActionLabel() { return actionLabel; }
        void setCount(int count) { this.count = count; }
    }
}
