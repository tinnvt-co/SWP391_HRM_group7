package controller;

import dao.EmployeeDAO;
import model.Employee;
import model.Employee.EmploymentStatus;
import model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet(name = "HrEmployeeServlet", urlPatterns = {"/hr/employees"})
public class HrEmployeeServlet extends HttpServlet {

    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!hasPermission(request, "VIEW_EMPLOYEE_LIST")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = request.getParameter("action");
        if (action == null) action = "list";

        try {
            switch (action) {
                case "view" -> handleView(request, response);
                default     -> handleList(request, response);
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
            if ("updateStatus".equals(action)) {
                handleUpdateStatus(request, response);
            } else {
                response.sendRedirect(request.getContextPath() + "/hr/employees");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void handleList(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        request.setAttribute("employees", employeeDAO.findByRoleName("EMPLOYEE"));
        request.getRequestDispatcher("/views/hr/employee-list.jsp").forward(request, response);
    }

    private void handleView(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        if (!hasPermission(request, "VIEW_EMPLOYEE_INFORMATION")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/hr/employees");
            return;
        }

        Employee employee = employeeDAO.findDetailById(Integer.parseInt(idParam));
        if (employee == null) {
            response.sendRedirect(request.getContextPath() + "/hr/employees");
            return;
        }

        request.setAttribute("employee", employee);
        request.setAttribute("statuses", EmploymentStatus.values());
        request.getRequestDispatcher("/views/hr/employee-detail.jsp").forward(request, response);
    }

    private void handleUpdateStatus(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {

        if (!hasPermission(request, "UPDATE_EMPLOYMENT_STATUS")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String idParam   = request.getParameter("employeeId");
        String statusStr = request.getParameter("employmentStatus");

        if (idParam == null || idParam.isBlank() || statusStr == null || statusStr.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/hr/employees");
            return;
        }

        int employeeId = Integer.parseInt(idParam);

        EmploymentStatus status;
        try {
            status = EmploymentStatus.valueOf(statusStr);
        } catch (IllegalArgumentException ex) {
            response.sendRedirect(request.getContextPath()
                    + "/hr/employees?action=view&id=" + employeeId + "&error=invalid-status");
            return;
        }

        User currentUser = getCurrentUser(request);
        employeeDAO.updateEmploymentStatus(employeeId, status, currentUser.getUserId());
        response.sendRedirect(request.getContextPath()
                + "/hr/employees?action=view&id=" + employeeId + "&statusUpdated=success");
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
}
