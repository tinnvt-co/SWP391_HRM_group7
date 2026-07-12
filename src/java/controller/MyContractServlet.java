package controller;

import dao.AllowanceTypeDAO;
import dao.ContractDAO;
import dao.EmployeeDAO;
import model.Contract;
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
import java.util.List;

@WebServlet(name = "MyContractServlet", urlPatterns = {"/my-contract"})
public class MyContractServlet extends HttpServlet {

    private final ContractDAO contractDAO = new ContractDAO();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final AllowanceTypeDAO allowanceDAO = new AllowanceTypeDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!hasPermission(request, "VIEW_MY_CONTRACT")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        User currentUser = getCurrentUser(request);

        try {
            Employee employee = employeeDAO.findByUserId(currentUser.getUserId());
            if (employee == null) {
                request.setAttribute("error",
                        "Your account is not linked to an employee record. Please contact HR.");
                request.setAttribute("contracts", java.util.Collections.emptyList());
                request.setAttribute("activeAllowanceTypes", allowanceDAO.findActive());
                request.setAttribute("totalActiveAllowance", allowanceDAO.sumActiveAllowances());
                request.getRequestDispatcher("/views/contract/my-contract.jsp").forward(request, response);
                return;
            }

            List<Contract> contracts = contractDAO.findByEmployeeId(employee.getEmployeeId());
            request.setAttribute("employee", employee);
            request.setAttribute("contracts", contracts);
            request.setAttribute("activeAllowanceTypes", allowanceDAO.findActive());
            request.setAttribute("totalActiveAllowance", allowanceDAO.sumActiveAllowances());
            request.getRequestDispatcher("/views/contract/my-contract.jsp").forward(request, response);
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
}
