package controller;

import dao.DepartmentDAO;
import dao.UserDAO;
import model.Department;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet(name = "DepartmentServlet", urlPatterns = {"/departments"})
public class DepartmentServlet extends HttpServlet {

    private final DepartmentDAO departmentDAO = new DepartmentDAO();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!hasPermission(request, "VIEW_DEPARTMENT_LIST")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = request.getParameter("action");
        if (action == null) action = "list";

        try {
            switch (action) {
                case "add"  -> handleAddForm(request, response);
                case "edit" -> handleEditForm(request, response);
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
            switch (action) {
                case "add"    -> handleAdd(request, response);
                case "edit"   -> handleEdit(request, response);
                case "toggle" -> handleToggle(request, response);
                default       -> response.sendRedirect(request.getContextPath() + "/departments");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void handleList(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        request.setAttribute("departments", departmentDAO.findAll());
        request.getRequestDispatcher("/views/department/department-list.jsp").forward(request, response);
    }

    private void handleAddForm(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        if (!hasPermission(request, "CREATE_DEPARTMENT")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        request.setAttribute("managers", userDAO.findActiveByRoleName("MANAGER"));
        request.getRequestDispatcher("/views/department/add-department.jsp").forward(request, response);
    }

    private void handleAdd(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        if (!hasPermission(request, "CREATE_DEPARTMENT")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String code        = trim(request.getParameter("departmentCode"));
        String name        = trim(request.getParameter("departmentName"));
        String description  = trim(request.getParameter("description"));
        String managerIdStr = request.getParameter("managerId");

        if (code.isEmpty() || name.isEmpty()) {
            forwardAddForm(request, response, "Department code and name are required.");
            return;
        }

        if (!code.matches("^[A-Za-z0-9_]{2,50}$")) {
            forwardAddForm(request, response,
                    "Department code must be 2-50 characters (letters, digits, underscore).");
            return;
        }

        String upperCode = code.toUpperCase();
        if (departmentDAO.existsByCode(upperCode)) {
            forwardAddForm(request, response, "Department code '" + upperCode + "' already exists.");
            return;
        }

        if (name.length() > 150) {
            forwardAddForm(request, response, "Department name must be 150 characters or fewer.");
            return;
        }
        if (description.length() > 255) {
            forwardAddForm(request, response, "Description must be 255 characters or fewer.");
            return;
        }

        Department d = new Department();
        d.setDepartmentCode(upperCode);
        d.setDepartmentName(name);
        d.setDescription(description.isEmpty() ? null : description);
        d.setManagerId(parseManagerId(managerIdStr));

        departmentDAO.insert(d);
        response.sendRedirect(request.getContextPath() + "/departments?added=success");
    }

    private void handleEditForm(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        if (!hasPermission(request, "UPDATE_DEPARTMENT")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/departments");
            return;
        }

        Department d = departmentDAO.findById(Integer.parseInt(idParam));
        if (d == null) {
            response.sendRedirect(request.getContextPath() + "/departments");
            return;
        }

        request.setAttribute("department", d);
        request.setAttribute("managers", userDAO.findActiveByRoleName("MANAGER"));
        request.getRequestDispatcher("/views/department/edit-department.jsp").forward(request, response);
    }

    private void handleEdit(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        if (!hasPermission(request, "UPDATE_DEPARTMENT")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String idParam      = request.getParameter("departmentId");
        String name         = trim(request.getParameter("departmentName"));
        String description  = trim(request.getParameter("description"));
        String managerIdStr = request.getParameter("managerId");

        if (idParam == null || idParam.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/departments");
            return;
        }

        int departmentId = Integer.parseInt(idParam);

        if (name.isEmpty()) {
            forwardEditForm(request, response, departmentId, "Department name is required.");
            return;
        }
        if (name.length() > 150) {
            forwardEditForm(request, response, departmentId, "Department name must be 150 characters or fewer.");
            return;
        }
        if (description.length() > 255) {
            forwardEditForm(request, response, departmentId, "Description must be 255 characters or fewer.");
            return;
        }

        Department d = new Department();
        d.setDepartmentId(departmentId);
        d.setDepartmentName(name);
        d.setDescription(description.isEmpty() ? null : description);
        d.setManagerId(parseManagerId(managerIdStr));

        departmentDAO.update(d);
        response.sendRedirect(request.getContextPath() + "/departments?updated=success");
    }

    private void handleToggle(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {

        if (!hasPermission(request, "ACTIVE_DEACTIVE_DEPARTMENT")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String idParam       = request.getParameter("departmentId");
        String currentStatus = request.getParameter("currentStatus");

        if (idParam == null || currentStatus == null) {
            response.sendRedirect(request.getContextPath() + "/departments");
            return;
        }

        boolean newStatus = !"true".equals(currentStatus);
        departmentDAO.setActiveStatus(Integer.parseInt(idParam), newStatus);
        response.sendRedirect(request.getContextPath() + "/departments?toggled=success");
    }

    private void forwardAddForm(HttpServletRequest request, HttpServletResponse response, String error)
            throws SQLException, ServletException, IOException {
        request.setAttribute("error", error);
        request.setAttribute("managers", userDAO.findActiveByRoleName("MANAGER"));
        request.getRequestDispatcher("/views/department/add-department.jsp").forward(request, response);
    }

    private void forwardEditForm(HttpServletRequest request, HttpServletResponse response,
                                 int departmentId, String error)
            throws SQLException, ServletException, IOException {
        request.setAttribute("error", error);
        request.setAttribute("department", departmentDAO.findById(departmentId));
        request.setAttribute("managers", userDAO.findActiveByRoleName("MANAGER"));
        request.getRequestDispatcher("/views/department/edit-department.jsp").forward(request, response);
    }

    private Integer parseManagerId(String managerIdStr) {
        if (managerIdStr == null || managerIdStr.isBlank()) return null;
        try { return Integer.parseInt(managerIdStr); }
        catch (NumberFormatException ex) { return null; }
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
