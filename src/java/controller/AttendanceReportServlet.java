package controller;

import dao.AttendanceReportDAO;
import model.AttendanceReport;
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
 * HR Staff screen: "View Attendance Report".
 * Lists the monthly attendance reports submitted by managers, for a chosen
 * month (defaults to the current month).
 */
@WebServlet(name = "AttendanceReportServlet", urlPatterns = {"/attendance-report"})
public class AttendanceReportServlet extends HttpServlet {

    private final AttendanceReportDAO reportDAO = new AttendanceReportDAO();

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
            List<AttendanceReport> reports =
                    reportDAO.findSubmittedByMonth(year, month, null);
            request.setAttribute("reports", reports);
            request.setAttribute("selectedYear", year);
            request.setAttribute("selectedMonth", month);
            request.setAttribute("monthLabel",
                    YearMonth.of(year, month).getMonth()
                            .getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year);
            request.getRequestDispatcher("/views/attendance/attendance-report-list.jsp")
                   .forward(request, response);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
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
}
