package controller;

import dao.DepartmentDAO;
import dao.EmployeeDAO;
import dao.UserDAO;
import model.Department;
import model.Employee;
import model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet(name = "ProfileServlet", urlPatterns = {"/profile"})
public class ProfileServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User sessionUser = (User) session.getAttribute("currentUser");

        try {
            User user = userDAO.findById(sessionUser.getUserId());
            request.setAttribute("user", user);
            Employee employee = employeeDAO.findByUserId(sessionUser.getUserId());
            request.setAttribute("employee", employee);
            request.setAttribute("canManageBankAccount",
                    canManageBankAccount(session, sessionUser, employee));
            request.getRequestDispatcher("/views/profile/view-profile.jsp").forward(request, response);
        } catch (SQLException e) {
            request.setAttribute("user", sessionUser);
            request.setAttribute("canManageBankAccount", false);
            request.getRequestDispatcher("/views/profile/view-profile.jsp").forward(request, response);
        }
    }

    private boolean canManageBankAccount(HttpSession session, User user, Employee employee)
            throws SQLException {
        if (session == null || user == null || user.getRole() == null || employee == null) {
            return false;
        }
        if ("IT".equalsIgnoreCase(user.getRole().getRoleName())) {
            return false;
        }
        Department department = departmentDAO.findById(employee.getDepartmentId());
        return department == null || !"IT".equalsIgnoreCase(department.getDepartmentCode());
    }
}
