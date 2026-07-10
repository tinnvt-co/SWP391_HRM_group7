package controller;

import dao.AllowanceTypeDAO;
import model.AllowanceType;
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
import java.util.List;
import java.util.Locale;

@WebServlet(name = "AllowanceServlet", urlPatterns = {"/allowances"})
public class AllowanceServlet extends HttpServlet {

    private final AllowanceTypeDAO allowanceDAO = new AllowanceTypeDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!hasPermission(request, "MANAGE_ALLOWANCE")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            loadListAttributes(request);
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
        if (!hasPermission(request, "MANAGE_ALLOWANCE")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = trim(request.getParameter("action"));
        try {
            switch (action) {
                case "create" -> handleCreate(request, response);
                case "edit" -> handleEdit(request, response);
                case "toggle" -> handleToggle(request, response);
                default -> response.sendRedirect(request.getContextPath() + "/allowances");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void handleCreate(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        AllowanceType allowance = readAllowanceForm(request, true);
        String error = validateCreate(allowance);
        if (error != null) {
            forwardWithError(request, response, error);
            return;
        }
        if (allowanceDAO.existsByCode(allowance.getAllowanceCode(), null)) {
            forwardWithError(request, response,
                    "Allowance code '" + allowance.getAllowanceCode() + "' already exists.");
            return;
        }

        User currentUser = currentUser(request);
        allowance.setActive(true);
        allowance.setCreatedBy(currentUser.getUserId());
        allowance.setUpdatedBy(currentUser.getUserId());
        allowanceDAO.insert(allowance);
        flash(request, "Allowance type created.");
        response.sendRedirect(request.getContextPath() + "/allowances");
    }

    private void handleEdit(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        Integer id = parseIntOrNull(request.getParameter("allowanceTypeId"));
        if (id == null) {
            forwardWithError(request, response, "Invalid allowance type.");
            return;
        }

        AllowanceType existing = allowanceDAO.findById(id);
        if (existing == null) {
            forwardWithError(request, response, "Allowance type not found.");
            return;
        }

        AllowanceType updates = readAllowanceForm(request, false);
        updates.setAllowanceTypeId(id);
        updates.setAllowanceCode(existing.getAllowanceCode());
        updates.setActive(existing.isActive());
        String error = validateEdit(updates);
        if (error != null) {
            forwardWithError(request, response, error);
            return;
        }

        updates.setUpdatedBy(currentUser(request).getUserId());
        allowanceDAO.update(updates);
        flash(request, "Allowance type updated.");
        response.sendRedirect(request.getContextPath() + "/allowances");
    }

    private void handleToggle(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        Integer id = parseIntOrNull(request.getParameter("allowanceTypeId"));
        if (id == null) {
            flashError(request, "Invalid allowance type.");
            response.sendRedirect(request.getContextPath() + "/allowances");
            return;
        }
        boolean active = Boolean.parseBoolean(request.getParameter("active"));
        boolean updated = allowanceDAO.setActiveStatus(id, active, currentUser(request).getUserId());
        flash(request, updated
                ? (active ? "Allowance type activated." : "Allowance type deactivated.")
                : "Allowance type not found.");
        response.sendRedirect(request.getContextPath() + "/allowances");
    }

    private AllowanceType readAllowanceForm(HttpServletRequest request, boolean includeCode) {
        AllowanceType a = new AllowanceType();
        if (includeCode) {
            String code = trim(request.getParameter("allowanceCode"))
                    .toUpperCase(Locale.ROOT)
                    .replaceAll("\\s+", "_");
            a.setAllowanceCode(code);
        }
        a.setAllowanceName(trim(request.getParameter("allowanceName")));
        a.setAmount(parseMoney(request.getParameter("amount")));
        String description = trim(request.getParameter("description"));
        a.setDescription(description.isEmpty() ? null : description);
        return a;
    }

    private String validateCreate(AllowanceType a) {
        if (a.getAllowanceCode() == null
                || !a.getAllowanceCode().matches("^[A-Z0-9_]{2,50}$")) {
            return "Allowance code must be 2-50 characters and use only letters, numbers, or underscores.";
        }
        return validateEdit(a);
    }

    private String validateEdit(AllowanceType a) {
        if (a.getAllowanceName() == null || a.getAllowanceName().isBlank()) {
            return "Allowance name is required.";
        }
        if (a.getAllowanceName().length() > 100) {
            return "Allowance name must be 100 characters or fewer.";
        }
        if (a.getAmount() == null || a.getAmount().signum() < 0) {
            return "Allowance amount must be a valid non-negative number.";
        }
        if (a.getDescription() != null && a.getDescription().length() > 255) {
            return "Description must be 255 characters or fewer.";
        }
        return null;
    }

    private void forwardWithError(HttpServletRequest request, HttpServletResponse response, String error)
            throws SQLException, ServletException, IOException {
        request.setAttribute("error", error);
        loadListAttributes(request);
        request.getRequestDispatcher("/views/allowance/manage-allowance.jsp")
               .forward(request, response);
    }

    private void loadListAttributes(HttpServletRequest request) throws SQLException {
        request.setAttribute("allowanceTypes", allowanceDAO.findAll());
        request.setAttribute("totalActiveAllowance", allowanceDAO.sumActiveAllowances());
    }

    private BigDecimal parseMoney(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseIntOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void flash(HttpServletRequest request, String message) {
        request.getSession(true).setAttribute("allowanceMessage", message);
    }

    private void flashError(HttpServletRequest request, String message) {
        request.getSession(true).setAttribute("allowanceError", message);
    }

    private void readFlash(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return;
        Object msg = session.getAttribute("allowanceMessage");
        Object err = session.getAttribute("allowanceError");
        if (msg != null) {
            request.setAttribute("allowanceMessage", msg);
            session.removeAttribute("allowanceMessage");
        }
        if (err != null) {
            request.setAttribute("error", err);
            session.removeAttribute("allowanceError");
        }
    }

    private boolean hasPermission(HttpServletRequest request, String permCode) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        List<?> perms = (List<?>) session.getAttribute("permissions");
        return perms != null && perms.contains(permCode);
    }

    private User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("currentUser");
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
