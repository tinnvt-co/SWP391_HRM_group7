package controller;

import dao.ContractDAO;
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

@WebServlet(name = "AllowanceServlet", urlPatterns = {"/allowances"})
public class AllowanceServlet extends HttpServlet {

    private final ContractDAO contractDAO = new ContractDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!canManageAllowance(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            request.setAttribute("settings", contractDAO.findGlobalAllowanceSettings());
            readFlash(request);
            request.getRequestDispatcher("/views/allowance/manage-allowance.jsp")
                   .forward(request, response);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!canManageAllowance(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        BigDecimal lunch = parseNonNegativeMoney(request.getParameter("lunchAllowance"));
        BigDecimal transport = parseNonNegativeMoney(request.getParameter("transportationAllowance"));
        BigDecimal phone = parseNonNegativeMoney(request.getParameter("phoneAllowance"));
        BigDecimal responsibility = parseNonNegativeMoney(request.getParameter("responsibilityAllowance"));

        if (lunch == null || transport == null || phone == null || responsibility == null) {
            forwardWithError(request, response, "Allowance values must be valid non-negative numbers.");
            return;
        }

        try {
            User currentUser = currentUser(request);
            int updated = contractDAO.updateGlobalAllowances(
                    lunch, transport, phone, responsibility, currentUser.getUserId());
            HttpSession session = request.getSession(true);
            session.setAttribute("allowanceMessage",
                    "Global allowance updated for " + updated + " active contract(s).");
            response.sendRedirect(request.getContextPath() + "/allowances");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void forwardWithError(HttpServletRequest request, HttpServletResponse response, String error)
            throws ServletException, IOException {
        try {
            request.setAttribute("error", error);
            request.setAttribute("settings", contractDAO.findGlobalAllowanceSettings());
            request.getRequestDispatcher("/views/allowance/manage-allowance.jsp")
                   .forward(request, response);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private BigDecimal parseNonNegativeMoney(String value) {
        if (value == null || value.isBlank()) return BigDecimal.ZERO;
        try {
            BigDecimal amount = new BigDecimal(value.trim());
            return amount.signum() < 0 ? null : amount;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void readFlash(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return;
        Object msg = session.getAttribute("allowanceMessage");
        if (msg != null) {
            request.setAttribute("allowanceMessage", msg);
            session.removeAttribute("allowanceMessage");
        }
    }

    private boolean canManageAllowance(HttpServletRequest request) {
        User user = currentUser(request);
        if (user == null || user.getRole() == null) return false;
        String roleName = user.getRole().getRoleName();
        return "HR_MANAGER".equalsIgnoreCase(roleName)
                || "ADMIN".equalsIgnoreCase(roleName);
    }

    private User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("currentUser");
    }
}
