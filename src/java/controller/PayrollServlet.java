package controller;

import dao.AttendanceReportDAO;
import dao.DepartmentDAO;
import dao.PayrollDAO;
import dao.PayrollPeriodDAO;
import model.Department;
import model.AttendanceReport;
import model.Payroll;
import model.PayrollPeriod;
import model.PayrollTaskSummary;
import model.User;
import service.AttendanceAutoConfirmService;
import service.PayrollCalculationService;

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
 * Payroll workflow.
 *
 * Status flow on a payroll_period (one department per month):
 *   Generate (HR Staff)            -> Draft
 *   Submit for Approval (HR Staff) -> Pending Approval   (only from Draft/Rejected)
 *   Approve (HR Manager)           -> Approved           (only from Pending Approval)
 *   Reject  (HR Manager, +reason)  -> Rejected           (only from Pending Approval)
 *   Confirm Payment (HR Staff)     -> Paid               (only from Approved)
 *
 * Editing a single payroll line (kpi_bonus + advance):
 *   HR Staff   when period is Draft or Rejected
 *   HR Manager when period is Pending Approval
 *   Locked once Approved or Paid.
 */
@WebServlet(name = "PayrollServlet", urlPatterns = {"/payroll"})
public class PayrollServlet extends HttpServlet {

    private static final int PAGE_SIZE = 10;

    private final PayrollPeriodDAO periodDAO = new PayrollPeriodDAO();
    private final PayrollDAO payrollDAO = new PayrollDAO();
    private final AttendanceReportDAO reportDAO = new AttendanceReportDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();
    private final PayrollCalculationService calc = new PayrollCalculationService();
    private final AttendanceAutoConfirmService autoConfirmService = new AttendanceAutoConfirmService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if (action == null) action = "list";
        try {
            switch (action) {
                case "approval" -> {
                    if (!hasPerm(request, "APPROVE_REJECT_PAYROLL")) { forbid(response); return; }
                    handleApprovalList(request, response);
                }
                default -> {
                    if (!hasPerm(request, "VIEW_PAYROLL")) { forbid(response); return; }
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
                case "generate" -> {
                    if (!hasPerm(request, "GENERATE_PAYROLL")) { forbid(response); return; }
                    handleGenerate(request, response);
                }
                case "submit" -> {
                    if (!hasPerm(request, "SUBMIT_PAYROLL_FOR_APPROVAL")) { forbid(response); return; }
                    handleSubmit(request, response);
                }
                case "approve" -> {
                    if (!hasPerm(request, "APPROVE_REJECT_PAYROLL")) { forbid(response); return; }
                    handleApprove(request, response);
                }
                case "reject" -> {
                    if (!hasPerm(request, "APPROVE_REJECT_PAYROLL")) { forbid(response); return; }
                    handleReject(request, response);
                }
                case "confirmPayment" -> {
                    if (!hasPerm(request, "GENERATE_PAYROLL")) { forbid(response); return; }
                    handleConfirmPayment(request, response);
                }
                default -> response.sendRedirect(request.getContextPath() + "/payroll");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    // ---------- HR Staff: list + generate ----------

    private void handleList(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        autoConfirmService.runDueAutoConfirm();

        YearMonth now = YearMonth.now();
        int year  = parseIntOr(request.getParameter("year"),  now.getYear());
        int month = parseIntOr(request.getParameter("month"), now.getMonthValue());
        if (month < 1 || month > 12) month = now.getMonthValue();
        List<Department> departments = departmentDAO.findAttendanceDepartments();
        int totalPayrolls = payrollDAO.countByMonth(year, month);
        int totalPages = Math.max(1, (int) Math.ceil(totalPayrolls / (double) PAGE_SIZE));
        int page = parsePageParam(request.getParameter("page"), totalPages);
        int offset = (page - 1) * PAGE_SIZE;
        List<Payroll> payrolls = payrollDAO.findByMonthPage(year, month, offset, PAGE_SIZE);
        BigDecimal monthlySalaryTotal = payrollDAO.sumNetSalaryByMonth(year, month);
        BigDecimal yearlySalaryTotal = payrollDAO.sumNetSalaryByYear(year);
        PayrollTaskSummary payrollTaskSummary = periodDAO.findHrStaffTaskSummary();

        boolean hasReports = hasReadyPayrollReports(year, month, departments);
        int submittablePayrollBatchCount = periodDAO.findByMonthAndStatuses(
                year, month, PayrollPeriod.Status.Draft, PayrollPeriod.Status.Rejected).size();
        int payablePayrollBatchCount = periodDAO.findByMonthAndStatuses(
                year, month, PayrollPeriod.Status.Approved).size();

        request.setAttribute("period", null);
        request.setAttribute("payrolls", payrolls);
        request.setAttribute("selectedDeptName", "All departments");
        request.setAttribute("allDepartmentsScope", true);
        request.setAttribute("hasReports", hasReports);
        request.setAttribute("submittablePayrollBatchCount", submittablePayrollBatchCount);
        request.setAttribute("payablePayrollBatchCount", payablePayrollBatchCount);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalPayrolls", totalPayrolls);
        request.setAttribute("selectedYear", year);
        request.setAttribute("selectedMonth", month);
        request.setAttribute("monthLabel", monthLabel(year, month));
        request.setAttribute("monthlySalaryTotal", monthlySalaryTotal);
        request.setAttribute("yearlySalaryTotal", yearlySalaryTotal);
        request.setAttribute("payrollTaskSummary", payrollTaskSummary);
        readFlash(request);
        request.getRequestDispatcher("/views/payroll/payroll-list.jsp")
               .forward(request, response);
    }

    private void handleGenerate(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        autoConfirmService.runDueAutoConfirm();

        User user = currentUser(request);
        HttpSession session = request.getSession(true);
        String ctx = request.getContextPath();

        YearMonth now = YearMonth.now();
        int year  = parseIntOr(request.getParameter("year"),  now.getYear());
        int month = parseIntOr(request.getParameter("month"), now.getMonthValue());
        List<Department> departments = departmentDAO.findAttendanceDepartments();
        handleGenerateAllReady(user, session, ctx, year, month, departments, response);
    }

    private void handleGenerateAllReady(User user, HttpSession session, String ctx,
                                        int year, int month, List<Department> departments,
                                        HttpServletResponse response)
            throws SQLException, IOException {
        int departmentsGenerated = 0;
        int employeesGenerated = 0;
        StringBuilder skipped = new StringBuilder();

        for (Department dept : departments) {
            int deptId = dept.getDepartmentId();
            if (periodDAO.findByMonthAndDepartment(year, month, deptId) != null) {
                continue;
            }
            List<AttendanceReport> reports =
                    reportDAO.findApprovedForPayrollByMonth(year, month, deptId);
            if (reports.isEmpty()) {
                continue;
            }
            int created = createPayrollForDepartment(user, year, month, dept, reports, skipped);
            if (created > 0) {
                departmentsGenerated++;
                employeesGenerated += created;
            }
        }

        if (employeesGenerated > 0) {
            String msg = "Generated payroll for " + monthLabel(year, month)
                    + ": " + employeesGenerated + " employee(s) across "
                    + departmentsGenerated + " department(s).";
            if (skipped.length() > 0) msg += " Skipped: " + skipped;
            flashMessage(session, msg);
        } else {
            flashError(session,
                    "No HR Manager-approved attendance reports are waiting for payroll generation.");
        }
        response.sendRedirect(ctx + "/payroll?year=" + year + "&month=" + month);
    }

    private int createPayrollForDepartment(User user, int year, int month, Department dept,
                                           List<AttendanceReport> reports, StringBuilder skipped)
            throws SQLException {
        PayrollPeriod p = new PayrollPeriod();
        p.setPeriodName("Payroll " + monthLabel(year, month) + " - " + dept.getDepartmentName());
        p.setPayrollMonth(month);
        p.setPayrollYear(year);
        p.setDepartmentId(dept.getDepartmentId());
        p.setCreatedBy(user.getUserId());
        int periodId = periodDAO.createDraft(p);
        if (periodId < 0) {
            return -1;
        }

        int created = 0;
        for (AttendanceReport r : reports) {
            PayrollCalculationService.BuildResult res = calc.build(periodId, r);
            if (res.payroll != null) {
                payrollDAO.insert(res.payroll);
                created++;
            } else {
                skipped.append(res.skipReason).append("; ");
            }
        }
        return created;
    }

    // ---------- HR Staff: submit / confirm payment ----------

    private void handleSubmit(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        HttpSession session = request.getSession(true);
        String ctx = request.getContextPath();
        int periodId = parseIntOr(request.getParameter("periodId"), -1);
        if (periodId <= 0) {
            int year = parseIntOr(request.getParameter("year"), YearMonth.now().getYear());
            int month = parseIntOr(request.getParameter("month"), YearMonth.now().getMonthValue());
            List<PayrollPeriod> periods = periodDAO.findByMonthAndStatuses(
                    year, month, PayrollPeriod.Status.Draft, PayrollPeriod.Status.Rejected);
            int submitted = updatePayrollPeriods(periods,
                    PayrollPeriod.Status.PendingApproval, Payroll.Status.PendingApproval,
                    null, null);
            if (submitted > 0) {
                flashMessage(session,
                        "Submitted " + submitted + " payroll batch(es) to HR Manager for approval.");
            } else {
                flashError(session, "No Draft or Rejected payroll batches are ready to submit.");
            }
            response.sendRedirect(ctx + "/payroll?year=" + year + "&month=" + month);
            return;
        }

        PayrollPeriod period = loadPeriod(request);
        if (period == null) { flashError(session, "Payroll not found."); response.sendRedirect(ctx + "/payroll"); return; }

        if (period.getStatus() != PayrollPeriod.Status.Draft
                && period.getStatus() != PayrollPeriod.Status.Rejected) {
            flashError(session, "Only a Draft or Rejected payroll can be submitted for approval.");
            response.sendRedirect(redirectTo(ctx, period));
            return;
        }
        periodDAO.updateStatus(period.getPayrollPeriodId(),
                PayrollPeriod.Status.PendingApproval, null, null);
        payrollDAO.updateStatusByPeriod(period.getPayrollPeriodId(), Payroll.Status.PendingApproval);
        flashMessage(session, "Payroll submitted to HR Manager for approval.");
        response.sendRedirect(redirectTo(ctx, period));
    }

    private void handleConfirmPayment(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        User user = currentUser(request);
        HttpSession session = request.getSession(true);
        String ctx = request.getContextPath();
        int periodId = parseIntOr(request.getParameter("periodId"), -1);
        if (periodId <= 0) {
            int year = parseIntOr(request.getParameter("year"), YearMonth.now().getYear());
            int month = parseIntOr(request.getParameter("month"), YearMonth.now().getMonthValue());
            List<PayrollPeriod> periods = periodDAO.findByMonthAndStatuses(
                    year, month, PayrollPeriod.Status.Approved);
            int paid = updatePayrollPeriods(periods,
                    PayrollPeriod.Status.Paid, Payroll.Status.Paid,
                    user.getUserId(), null);
            if (paid > 0) {
                flashMessage(session, "Payment confirmed for payroll.");
            } else {
                flashError(session, "No Approved payroll batches are ready for payment confirmation.");
            }
            response.sendRedirect(ctx + "/payroll?year=" + year + "&month=" + month);
            return;
        }

        PayrollPeriod period = loadPeriod(request);
        if (period == null) { flashError(session, "Payroll not found."); response.sendRedirect(ctx + "/payroll"); return; }

        if (period.getStatus() != PayrollPeriod.Status.Approved) {
            flashError(session, "Only an Approved payroll can be marked as paid.");
            response.sendRedirect(redirectTo(ctx, period));
            return;
        }
        periodDAO.updateStatus(period.getPayrollPeriodId(),
                PayrollPeriod.Status.Paid, user.getUserId(), null);
        payrollDAO.updateStatusByPeriod(period.getPayrollPeriodId(), Payroll.Status.Paid);
        flashMessage(session, "Payment confirmed for payroll.");
        response.sendRedirect(redirectTo(ctx, period));
    }

    // ---------- HR Manager: approval list / approve / reject ----------

    private void handleApprovalList(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        YearMonth now = YearMonth.now();
        int year  = parseIntOr(request.getParameter("year"),  now.getYear());
        int month = parseIntOr(request.getParameter("month"), now.getMonthValue());
        if (month < 1 || month > 12) month = now.getMonthValue();
        int totalPayrolls = payrollDAO.countByMonth(year, month);
        int totalPages = Math.max(1, (int) Math.ceil(totalPayrolls / (double) PAGE_SIZE));
        int page = parsePageParam(request.getParameter("page"), totalPages);
        int offset = (page - 1) * PAGE_SIZE;
        List<Payroll> payrolls = payrollDAO.findByMonthPage(year, month, offset, PAGE_SIZE);
        BigDecimal monthlySalaryTotal = payrollDAO.sumNetSalaryByMonth(year, month);
        BigDecimal yearlySalaryTotal = payrollDAO.sumNetSalaryByYear(year);
        PayrollTaskSummary payrollTaskSummary = periodDAO.findHrManagerTaskSummary();
        int pendingApprovalBatchCount = periodDAO.findByMonthAndStatuses(
                year, month, PayrollPeriod.Status.PendingApproval).size();

        request.setAttribute("period", null);
        request.setAttribute("payrolls", payrolls);
        request.setAttribute("selectedDeptName", "All departments");
        request.setAttribute("allDepartmentsScope", true);
        request.setAttribute("pendingApprovalBatchCount", pendingApprovalBatchCount);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalPayrolls", totalPayrolls);
        request.setAttribute("selectedYear", year);
        request.setAttribute("selectedMonth", month);
        request.setAttribute("monthLabel", monthLabel(year, month));
        request.setAttribute("monthlySalaryTotal", monthlySalaryTotal);
        request.setAttribute("yearlySalaryTotal", yearlySalaryTotal);
        request.setAttribute("payrollTaskSummary", payrollTaskSummary);
        readFlash(request);
        request.getRequestDispatcher("/views/payroll/payroll-approval.jsp")
               .forward(request, response);
    }

    private void handleApprove(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        User user = currentUser(request);
        HttpSession session = request.getSession(true);
        String ctx = request.getContextPath();
        int periodId = parseIntOr(request.getParameter("periodId"), -1);
        if (periodId <= 0) {
            int year = parseIntOr(request.getParameter("year"), YearMonth.now().getYear());
            int month = parseIntOr(request.getParameter("month"), YearMonth.now().getMonthValue());
            List<PayrollPeriod> periods = periodDAO.findByMonthAndStatuses(
                    year, month, PayrollPeriod.Status.PendingApproval);
            int approved = updatePayrollPeriods(periods,
                    PayrollPeriod.Status.Approved, Payroll.Status.Approved,
                    user.getUserId(), null);
            if (approved > 0) {
                flashMessage(session, "Approved " + approved + " payroll batch(es).");
            } else {
                flashError(session, "No Pending Approval payroll batches are ready to approve.");
            }
            response.sendRedirect(ctx + "/payroll?action=approval&year=" + year + "&month=" + month);
            return;
        }

        PayrollPeriod period = loadPeriod(request);
        if (period == null) { flashError(session, "Payroll not found."); response.sendRedirect(ctx + "/payroll?action=approval"); return; }

        if (period.getStatus() != PayrollPeriod.Status.PendingApproval) {
            flashError(session, "Only a payroll Pending Approval can be approved.");
            response.sendRedirect(redirectApproval(ctx, period));
            return;
        }
        periodDAO.updateStatus(period.getPayrollPeriodId(),
                PayrollPeriod.Status.Approved, user.getUserId(), null);
        payrollDAO.updateStatusByPeriod(period.getPayrollPeriodId(), Payroll.Status.Approved);
        flashMessage(session, "Payroll approved.");
        response.sendRedirect(redirectApproval(ctx, period));
    }

    private void handleReject(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        User user = currentUser(request);
        HttpSession session = request.getSession(true);
        String ctx = request.getContextPath();
        int year = parseIntOr(request.getParameter("year"), YearMonth.now().getYear());
        int month = parseIntOr(request.getParameter("month"), YearMonth.now().getMonthValue());
        String reason = trim(request.getParameter("rejectReason"));
        if (reason.isEmpty()) {
            flashError(session, "A reject reason is required.");
            response.sendRedirect(ctx + "/payroll?action=approval&year=" + year + "&month=" + month);
            return;
        }

        int periodId = parseIntOr(request.getParameter("periodId"), -1);
        if (periodId <= 0) {
            List<PayrollPeriod> periods = periodDAO.findByMonthAndStatuses(
                    year, month, PayrollPeriod.Status.PendingApproval);
            int rejected = updatePayrollPeriods(periods,
                    PayrollPeriod.Status.Rejected, Payroll.Status.Rejected,
                    user.getUserId(), reason);
            if (rejected > 0) {
                flashMessage(session, "Rejected " + rejected + " payroll batch(es) and sent them back to HR Staff.");
            } else {
                flashError(session, "No Pending Approval payroll batches are ready to reject.");
            }
            response.sendRedirect(ctx + "/payroll?action=approval&year=" + year + "&month=" + month);
            return;
        }

        PayrollPeriod period = loadPeriod(request);
        if (period == null) { flashError(session, "Payroll not found."); response.sendRedirect(ctx + "/payroll?action=approval"); return; }

        if (period.getStatus() != PayrollPeriod.Status.PendingApproval) {
            flashError(session, "Only a payroll Pending Approval can be rejected.");
            response.sendRedirect(redirectApproval(ctx, period));
            return;
        }
        periodDAO.updateStatus(period.getPayrollPeriodId(),
                PayrollPeriod.Status.Rejected, user.getUserId(), reason);
        payrollDAO.updateStatusByPeriod(period.getPayrollPeriodId(), Payroll.Status.Rejected);
        flashMessage(session, "Payroll rejected and sent back to HR Staff.");
        response.sendRedirect(redirectApproval(ctx, period));
    }

    // ---------- helpers ----------

    private PayrollPeriod loadPeriod(HttpServletRequest request) throws SQLException {
        int id = parseIntOr(request.getParameter("periodId"), -1);
        return id > 0 ? periodDAO.findById(id) : null;
    }

    private int updatePayrollPeriods(List<PayrollPeriod> periods,
                                     PayrollPeriod.Status periodStatus,
                                     Payroll.Status payrollStatus,
                                     Integer actorUserId,
                                     String reason) throws SQLException {
        int updated = 0;
        if (periods == null) return updated;
        for (PayrollPeriod period : periods) {
            if (periodDAO.updateStatus(period.getPayrollPeriodId(), periodStatus, actorUserId, reason)) {
                payrollDAO.updateStatusByPeriod(period.getPayrollPeriodId(), payrollStatus);
                updated++;
            }
        }
        return updated;
    }

    private String redirectTo(String ctx, PayrollPeriod p) {
        return ctx + "/payroll?year=" + p.getPayrollYear() + "&month=" + p.getPayrollMonth();
    }
    private String redirectApproval(String ctx, PayrollPeriod p) {
        return ctx + "/payroll?action=approval&year=" + p.getPayrollYear()
                + "&month=" + p.getPayrollMonth();
    }

    private boolean hasReadyPayrollReports(int year, int month, List<Department> departments)
            throws SQLException {
        if (departments == null) return false;
        for (Department dept : departments) {
            int deptId = dept.getDepartmentId();
            if (periodDAO.findByMonthAndDepartment(year, month, deptId) == null
                    && !reportDAO.findApprovedForPayrollByMonth(year, month, deptId).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private String monthLabel(int year, int month) {
        return YearMonth.of(year, month).getMonth()
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year;
    }

    private User currentUser(HttpServletRequest request) {
        HttpSession s = request.getSession(false);
        return s == null ? null : (User) s.getAttribute("currentUser");
    }

    private boolean hasPerm(HttpServletRequest request, String code) {
        HttpSession s = request.getSession(false);
        if (s == null) return false;
        List<?> perms = (List<?>) s.getAttribute("permissions");
        return perms != null && perms.contains(code);
    }

    private void forbid(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    private void flashMessage(HttpSession s, String m) { s.setAttribute("payrollMessage", m); }
    private void flashError(HttpSession s, String m)   { s.setAttribute("payrollError", m); }

    private void readFlash(HttpServletRequest request) {
        HttpSession s = request.getSession(false);
        if (s == null) return;
        Object ok = s.getAttribute("payrollMessage");
        Object err = s.getAttribute("payrollError");
        if (ok != null)  { request.setAttribute("payrollMessage", ok);  s.removeAttribute("payrollMessage"); }
        if (err != null) { request.setAttribute("payrollError", err);   s.removeAttribute("payrollError"); }
    }

    private int parseIntOr(String s, int dflt) {
        if (s == null || s.isBlank()) return dflt;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ex) { return dflt; }
    }

    private int parsePageParam(String pageParam, int totalPages) {
        int page = parseIntOr(pageParam, 1);
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        return page;
    }

    private Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ex) { return null; }
    }

    private String trim(String s) { return s == null ? "" : s.trim(); }
}
