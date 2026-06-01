package controller;

import dao.EmployeeDAO;
import dao.LeaveRequestDAO;
import model.Employee;
import model.LeaveRequest;
import model.LeaveRequest.LeaveType;
import model.LeaveRequest.Status;
import model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;

@WebServlet(name = "LeaveRequestServlet", urlPatterns = {"/leave-requests"})
public class LeaveRequestServlet extends HttpServlet {

    private final LeaveRequestDAO leaveDAO   = new LeaveRequestDAO();
    private final EmployeeDAO     employeeDAO = new EmployeeDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!hasPermission(request, "SUBMIT_LEAVE_REQUEST")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = request.getParameter("action");
        if (action == null) action = "submit";

        try {
            switch (action) {
                case "submit" -> handleSubmitForm(request, response);
                default       -> handleSubmitForm(request, response);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!hasPermission(request, "SUBMIT_LEAVE_REQUEST")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = request.getParameter("action");
        if (action == null) action = "";

        try {
            switch (action) {
                case "submit" -> handleSubmit(request, response);
                default       -> response.sendRedirect(request.getContextPath() + "/leave-requests");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void handleSubmitForm(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        User currentUser = getCurrentUser(request);
        Employee employee = employeeDAO.findByUserId(currentUser.getUserId());

        if (employee == null) {
            request.setAttribute("error",
                    "Your account is not linked to an employee record. Please contact HR.");
            request.getRequestDispatcher("/views/leave/submit-leave-request.jsp")
                   .forward(request, response);
            return;
        }

        request.setAttribute("employee", employee);
        request.setAttribute("leaveTypes", LeaveType.values());
        request.getRequestDispatcher("/views/leave/submit-leave-request.jsp")
               .forward(request, response);
    }

    private void handleSubmit(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        User currentUser = getCurrentUser(request);
        Employee employee = employeeDAO.findByUserId(currentUser.getUserId());

        if (employee == null) {
            forwardForm(request, response, employee,
                    "Your account is not linked to an employee record. Please contact HR.");
            return;
        }

        String leaveTypeStr = request.getParameter("leaveType");
        String startStr     = trim(request.getParameter("startDate"));
        String endStr       = trim(request.getParameter("endDate"));
        String reason       = trim(request.getParameter("reason"));

        if (leaveTypeStr == null || leaveTypeStr.isBlank()
                || startStr.isEmpty() || endStr.isEmpty() || reason.isEmpty()) {
            forwardForm(request, response, employee, "Please fill in all required fields.");
            return;
        }

        LeaveType leaveType;
        try {
            leaveType = LeaveType.valueOf(leaveTypeStr);
        } catch (IllegalArgumentException ex) {
            forwardForm(request, response, employee, "Invalid leave type.");
            return;
        }

        LocalDate startDate;
        LocalDate endDate;
        try {
            startDate = LocalDate.parse(startStr);
            endDate   = LocalDate.parse(endStr);
        } catch (DateTimeParseException ex) {
            forwardForm(request, response, employee, "Invalid date format.");
            return;
        }

        if (endDate.isBefore(startDate)) {
            forwardForm(request, response, employee, "End date must be on or after start date.");
            return;
        }

        if (startDate.isBefore(LocalDate.now())) {
            forwardForm(request, response, employee, "Start date cannot be in the past.");
            return;
        }

        if (reason.length() > 500) {
            forwardForm(request, response, employee, "Reason must be 500 characters or fewer.");
            return;
        }

        if (leaveDAO.hasOverlapping(employee.getEmployeeId(), startDate, endDate)) {
            forwardForm(request, response, employee,
                    "You already have a pending or approved leave request that overlaps with this date range.");
            return;
        }

        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        LeaveRequest lr = new LeaveRequest();
        lr.setEmployeeId(employee.getEmployeeId());
        lr.setLeaveType(leaveType);
        lr.setStartDate(startDate);
        lr.setEndDate(endDate);
        lr.setTotalDays(BigDecimal.valueOf(days));
        lr.setReason(reason);
        lr.setStatus(Status.Pending);

        leaveDAO.insert(lr);
        response.sendRedirect(request.getContextPath() + "/leave-requests?submitted=success");
    }

    private void forwardForm(HttpServletRequest request, HttpServletResponse response,
                             Employee employee, String error)
            throws ServletException, IOException {
        request.setAttribute("error", error);
        request.setAttribute("employee", employee);
        request.setAttribute("leaveTypes", LeaveType.values());
        request.getRequestDispatcher("/views/leave/submit-leave-request.jsp")
               .forward(request, response);
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

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
