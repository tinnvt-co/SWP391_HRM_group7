package controller;

import dao.DepartmentDAO;
import dao.EmployeeDAO;
import dao.RoleDAO;
import model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet(name = "HomeServlet", urlPatterns = {"/home"})
public class HomeServlet extends HttpServlet {

    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();
    private final RoleDAO roleDAO = new RoleDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("currentUser") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        User currentUser = (User) session.getAttribute("currentUser");
        request.setAttribute("currentUser", currentUser);

        try {
            loadDashboardStats(request, currentUser);
        } catch (SQLException e) {
            throw new ServletException(e);
        }

        request.getRequestDispatcher("/views/home.jsp").forward(request, response);
    }

    private void loadDashboardStats(HttpServletRequest request, User currentUser) throws SQLException {
        String roleName = currentUser != null && currentUser.getRole() != null
                ? currentUser.getRole().getRoleName()
                : "";

        if ("MANAGER".equalsIgnoreCase(roleName)) {
            request.setAttribute("dashboardScope", "manager");
            request.setAttribute("employeeCardLabel", "Department Employees");
            request.setAttribute("employeeCardValue",
                    employeeDAO.countEmployeesInManagedDepartments(currentUser.getUserId()));
            request.setAttribute("departmentCardLabel", "Managed Departments");
            request.setAttribute("departmentCardValue",
                    departmentDAO.countActiveByManagerId(currentUser.getUserId()));
            return;
        }

        if (isOrgWideRole(roleName)) {
            request.setAttribute("dashboardScope", "org");
            request.setAttribute("employeeCardLabel", "Employees");
            request.setAttribute("employeeCardValue", employeeDAO.countByRoleName("EMPLOYEE"));
            request.setAttribute("departmentCardLabel", "Departments");
            request.setAttribute("departmentCardValue", departmentDAO.countAll());
            request.setAttribute("roleCardLabel", "Roles");
            request.setAttribute("roleCardValue", roleDAO.countAll());
        }
    }

    private boolean isOrgWideRole(String roleName) {
        return "ADMIN".equalsIgnoreCase(roleName)
                || "HR_MANAGER".equalsIgnoreCase(roleName)
                || "HR_STAFF".equalsIgnoreCase(roleName);
    }
}
