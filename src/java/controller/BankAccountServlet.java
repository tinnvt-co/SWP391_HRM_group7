package controller;

import dao.EmployeeDAO;
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

@WebServlet(name = "BankAccountServlet", urlPatterns = {"/bank-account"})
public class BankAccountServlet extends HttpServlet {

    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            Employee employee = employeeDAO.findByUserId(currentUser.getUserId());
            if (employee == null) {
                request.setAttribute("error",
                        "Your account is not linked to an employee record. Please contact HR.");
            }
            request.setAttribute("employee", employee);
            request.getRequestDispatcher("/views/profile/bank-account.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        User currentUser = getCurrentUser(request);
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String bankName    = trim(request.getParameter("bankName"));
        String bankAccount = trim(request.getParameter("bankAccountNumber"));
        String bankBranch  = trim(request.getParameter("bankBranch"));

        try {
            Employee employee = employeeDAO.findByUserId(currentUser.getUserId());
            if (employee == null) {
                request.setAttribute("error",
                        "Your account is not linked to an employee record. Please contact HR.");
                request.getRequestDispatcher("/views/profile/bank-account.jsp").forward(request, response);
                return;
            }

            if (bankName.isEmpty() || bankAccount.isEmpty()) {
                forwardForm(request, response, employee, bankName, bankAccount, bankBranch,
                        "Bank name and account number are required.");
                return;
            }

            if (!bankAccount.matches("^[0-9]{6,30}$")) {
                forwardForm(request, response, employee, bankName, bankAccount, bankBranch,
                        "Account number must contain digits only (6 to 30 digits).");
                return;
            }

            if (bankName.length() > 150 || bankBranch.length() > 150) {
                forwardForm(request, response, employee, bankName, bankAccount, bankBranch,
                        "Bank name and branch must be 150 characters or fewer.");
                return;
            }

            employeeDAO.updateBankInfo(currentUser.getUserId(), bankName, bankAccount,
                    bankBranch, currentUser.getUserId());
            response.sendRedirect(request.getContextPath() + "/bank-account?saved=success");

        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void forwardForm(HttpServletRequest request, HttpServletResponse response,
                             Employee employee, String bankName, String bankAccount,
                             String bankBranch, String error)
            throws ServletException, IOException {
        employee.setBankName(bankName);
        employee.setBankAccountNumber(bankAccount);
        employee.setBankBranch(bankBranch);
        request.setAttribute("employee", employee);
        request.setAttribute("error", error);
        request.getRequestDispatcher("/views/profile/bank-account.jsp").forward(request, response);
    }

    private User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("currentUser");
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
