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

    private static final int PAGE_SIZE = 10;

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
            User currentUser = getCurrentUser(request);
            String roleName = currentUser != null && currentUser.getRole() != null
                    ? currentUser.getRole().getRoleName() : "";
            Integer managerUserId = "MANAGER".equalsIgnoreCase(roleName)
                    ? currentUser.getUserId() : null;

            int totalReports = reportDAO.countSubmittedByMonth(year, month, null, managerUserId);
            int totalPages = Math.max(1, (int) Math.ceil(totalReports / (double) PAGE_SIZE));
            int page = parsePageParam(request.getParameter("page"), totalPages);
            int offset = (page - 1) * PAGE_SIZE;

            List<AttendanceReport> reports =
                    reportDAO.findSubmittedByMonthPage(year, month, null, managerUserId, offset, PAGE_SIZE);
            request.setAttribute("reports", reports);
            request.setAttribute("selectedYear", year);
            request.setAttribute("selectedMonth", month);
            request.setAttribute("currentPage", page);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("totalReports", totalReports);
            request.setAttribute("managerScope", managerUserId != null);
            request.setAttribute("monthLabel",
                    YearMonth.of(year, month).getMonth()
                            .getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year);
            request.getRequestDispatcher("/views/attendance/attendance-report-list.jsp")
                   .forward(request, response);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
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

    private int parsePageParam(String pageParam, int totalPages) {
        int page = parseIntOr(pageParam, 1);
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        return page;
    }
}
