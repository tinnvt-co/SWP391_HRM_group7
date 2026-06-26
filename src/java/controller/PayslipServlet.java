package controller;

import dao.EmployeeDAO;
import dao.PayrollDAO;
import model.Employee;
import model.Payroll;
import model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Employee self-service payslip. Shows the employee's own salary for a chosen
 * month, but only once the payroll has been Paid (released by HR Staff).
 */
@WebServlet(name = "PayslipServlet", urlPatterns = {"/payslip"})
public class PayslipServlet extends HttpServlet {

    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final PayrollDAO payrollDAO = new PayrollDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!hasPerm(request, "VIEW_PAYSLIP")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        User user = currentUser(request);
        YearMonth now = YearMonth.now();
        int year  = parseIntOr(request.getParameter("year"),  now.getYear());
        int month = parseIntOr(request.getParameter("month"), now.getMonthValue());
        if (month < 1 || month > 12) month = now.getMonthValue();

        try {
            Employee me = employeeDAO.findByUserId(user.getUserId());
            Payroll payslip = null;
            if (me != null) {
                payslip = payrollDAO.findPaidByEmployeeAndMonth(
                        me.getEmployeeId(), year, month);
            }
            request.setAttribute("employee", me);
            request.setAttribute("payslip", payslip);
            request.setAttribute("selectedYear", year);
            request.setAttribute("selectedMonth", month);
            request.setAttribute("monthLabel",
                    YearMonth.of(year, month).getMonth()
                            .getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year);
            request.getRequestDispatcher("/views/payroll/payslip.jsp")
                   .forward(request, response);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
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

    private int parseIntOr(String s, int dflt) {
        if (s == null || s.isBlank()) return dflt;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ex) { return dflt; }
    }
}
