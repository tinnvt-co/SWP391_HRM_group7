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

        String action = request.getParameter("action");
        if (action == null) action = "submit";

        try {
            switch (action) {
                case "submit" -> {
                    if (!hasPermission(request, "SUBMIT_LEAVE_REQUEST")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleSubmitForm(request, response);
                }
                case "status" -> {
                    if (!hasPermission(request, "VIEW_LEAVE_REQUEST_STATUS")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleMyStatus(request, response);
                }
                case "list" -> {
                    if (!hasPermission(request, "VIEW_LEAVE_REQUEST_LIST")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleList(request, response);
                }
                case "detail" -> {
                    if (!hasPermission(request, "VIEW_LEAVE_REQUEST_DETAIL")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleDetail(request, response);
                }
                case "approve", "reject" -> {
                    response.sendRedirect(request.getContextPath() + "/leave-requests?action=list");
                }
                default -> {
                    if (!hasPermission(request, "SUBMIT_LEAVE_REQUEST")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleSubmitForm(request, response);
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
                case "submit" -> {
                    if (!hasPermission(request, "SUBMIT_LEAVE_REQUEST")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleSubmit(request, response);
                }
                case "approve" -> {
                    if (!hasPermission(request, "APPROVE_REJECT_LEAVE_REQUEST")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleApproveReject(request, response, true);
                }
                case "reject" -> {
                    if (!hasPermission(request, "APPROVE_REJECT_LEAVE_REQUEST")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleApproveReject(request, response, false);
                }
                default -> response.sendRedirect(request.getContextPath() + "/leave-requests");
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

    private void handleMyStatus(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        User currentUser = getCurrentUser(request);
        Employee employee = employeeDAO.findByUserId(currentUser.getUserId());

        if (employee == null) {
            request.setAttribute("error",
                    "Your account is not linked to an employee record. Please contact HR.");
            request.setAttribute("requests", java.util.Collections.emptyList());
            request.getRequestDispatcher("/views/leave/my-leave-requests.jsp")
                   .forward(request, response);
            return;
        }

        java.util.List<LeaveRequest> requests = leaveDAO.findByEmployeeId(employee.getEmployeeId());

        int pending = 0, approved = 0, rejected = 0, cancelled = 0;
        for (LeaveRequest lr : requests) {
            if (lr.getStatus() == null) continue;
            switch (lr.getStatus()) {
                case Pending   -> pending++;
                case Approved  -> approved++;
                case Rejected  -> rejected++;
                case Cancelled -> cancelled++;
            }
        }

        request.setAttribute("employee", employee);
        request.setAttribute("requests", requests);
        request.setAttribute("countPending", pending);
        request.setAttribute("countApproved", approved);
        request.setAttribute("countRejected", rejected);
        request.setAttribute("countCancelled", cancelled);
        request.getRequestDispatcher("/views/leave/my-leave-requests.jsp")
               .forward(request, response);
    }

    private void handleList(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        User currentUser = getCurrentUser(request);

        String statusParam = request.getParameter("status");
        if (statusParam == null) statusParam = "Pending";

        Status statusFilter = null;
        if (!"all".equalsIgnoreCase(statusParam)) {
            try {
                statusFilter = Status.valueOf(statusParam);
            } catch (IllegalArgumentException ex) {
                statusFilter = Status.Pending;
                statusParam = "Pending";
            }
        }

        String roleName = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "";
        boolean managerScope = "MANAGER".equalsIgnoreCase(roleName);

        java.util.List<LeaveRequest> requests;
        if (managerScope) {
            requests = leaveDAO.findByManagerUserId(currentUser.getUserId(), statusFilter);
        } else {
            requests = leaveDAO.findAll(statusFilter);
        }

        java.util.List<LeaveRequest> allForCount = managerScope
                ? leaveDAO.findByManagerUserId(currentUser.getUserId(), null)
                : leaveDAO.findAll(null);

        int pending = 0, approved = 0, rejected = 0, cancelled = 0;
        for (LeaveRequest lr : allForCount) {
            if (lr.getStatus() == null) continue;
            switch (lr.getStatus()) {
                case Pending   -> pending++;
                case Approved  -> approved++;
                case Rejected  -> rejected++;
                case Cancelled -> cancelled++;
            }
        }

        request.setAttribute("requests", requests);
        request.setAttribute("statusFilter", statusParam);
        request.setAttribute("managerScope", managerScope);
        request.setAttribute("countPending", pending);
        request.setAttribute("countApproved", approved);
        request.setAttribute("countRejected", rejected);
        request.setAttribute("countCancelled", cancelled);
        request.setAttribute("countTotal", allForCount.size());
        request.getRequestDispatcher("/views/leave/leave-request-list.jsp")
               .forward(request, response);
    }

    private void handleDetail(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/leave-requests?action=list");
            return;
        }

        int leaveRequestId;
        try {
            leaveRequestId = Integer.parseInt(idParam);
        } catch (NumberFormatException ex) {
            response.sendRedirect(request.getContextPath() + "/leave-requests?action=list");
            return;
        }

        LeaveRequest lr = leaveDAO.findById(leaveRequestId);
        if (lr == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        User currentUser = getCurrentUser(request);
        String roleName  = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "";

        boolean isOwner = lr.getEmployeeUserId() != null
                && lr.getEmployeeUserId() == currentUser.getUserId();
        boolean isManagerOfTarget = lr.getEmployeeManagerUserId() != null
                && lr.getEmployeeManagerUserId() == currentUser.getUserId();
        boolean isOrgWide = "ADMIN".equalsIgnoreCase(roleName)
                || "HR_STAFF".equalsIgnoreCase(roleName)
                || "HR_MANAGER".equalsIgnoreCase(roleName);

        boolean canSee = (isOwner    && hasPermission(request, "VIEW_LEAVE_REQUEST_STATUS"))
                      || (isManagerOfTarget && hasPermission(request, "VIEW_LEAVE_REQUEST_LIST"))
                      || (isOrgWide  && hasPermission(request, "VIEW_LEAVE_REQUEST_LIST"));

        if (!canSee) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String backTo = "list";
        if (isOwner && !isManagerOfTarget && !isOrgWide) backTo = "status";

        request.setAttribute("lr", lr);
        request.setAttribute("backTo", backTo);
        request.getRequestDispatcher("/views/leave/leave-request-detail.jsp")
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

    private void handleApproveReject(HttpServletRequest request, HttpServletResponse response,
                                      boolean approve)
            throws SQLException, IOException {

        if (!hasPermission(request, "APPROVE_REJECT_LEAVE_REQUEST")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/leave-requests?action=list");
            return;
        }

        int leaveRequestId;
        try {
            leaveRequestId = Integer.parseInt(idParam);
        } catch (NumberFormatException ex) {
            response.sendRedirect(request.getContextPath() + "/leave-requests?action=list");
            return;
        }

        LeaveRequest lr = leaveDAO.findById(leaveRequestId);
        if (lr == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        User currentUser = getCurrentUser(request);
        boolean isManagerOfTarget = lr.getEmployeeManagerUserId() != null
                && lr.getEmployeeManagerUserId() == currentUser.getUserId();

        if (!isManagerOfTarget) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (lr.getStatus() != Status.Pending) {
            response.sendRedirect(request.getContextPath()
                    + "/leave-requests?action=detail&id=" + leaveRequestId + "&error=not-pending");
            return;
        }

        String managerNote = trim(request.getParameter("managerNote"));
        if (managerNote.length() > 500) {
            managerNote = managerNote.substring(0, 500);
        }

        if (!approve && managerNote.isEmpty()) {
            response.sendRedirect(request.getContextPath()
                    + "/leave-requests?action=detail&id=" + leaveRequestId + "&error=reject-note-required");
            return;
        }

        boolean ok = approve
                ? leaveDAO.approve(leaveRequestId, currentUser.getUserId(), managerNote)
                : leaveDAO.reject(leaveRequestId, currentUser.getUserId(), managerNote);

        String result = ok ? (approve ? "approved" : "rejected") : "not-pending";
        response.sendRedirect(request.getContextPath()
                + "/leave-requests?action=detail&id=" + leaveRequestId + "&result=" + result);
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
