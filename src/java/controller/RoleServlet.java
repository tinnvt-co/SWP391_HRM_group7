package controller;

import dao.PermissionDAO;
import dao.RoleDAO;
import model.Role;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet(name = "RoleServlet", urlPatterns = {"/roles"})
public class RoleServlet extends HttpServlet {

    private final RoleDAO roleDAO = new RoleDAO();
    private final PermissionDAO permissionDAO = new PermissionDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!hasPermission(request, "VIEW_ROLE_LIST")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = request.getParameter("action");
        if (action == null) action = "list";

        try {
            switch (action) {
                case "permissions" -> handlePermissions(request, response);
                case "edit"        -> handleEditForm(request, response);
                default            -> handleList(request, response);
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
                case "edit" -> handleEdit(request, response);
                default     -> response.sendRedirect(request.getContextPath() + "/roles");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void handleList(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        request.setAttribute("roles", roleDAO.findAll());
        request.getRequestDispatcher("/views/role/role-list.jsp").forward(request, response);
    }

    private void handlePermissions(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        if (!hasPermission(request, "VIEW_ROLE_PERMISSIONS")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/roles");
            return;
        }

        Role role = roleDAO.findById(Integer.parseInt(idParam));
        if (role == null) {
            response.sendRedirect(request.getContextPath() + "/roles");
            return;
        }

        request.setAttribute("role", role);
        request.setAttribute("permissions", permissionDAO.findByRoleId(role.getRoleId()));
        request.getRequestDispatcher("/views/role/role-permissions.jsp").forward(request, response);
    }

    private void handleEditForm(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        if (!hasPermission(request, "UPDATE_ROLE_INFORMATION")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/roles");
            return;
        }

        Role role = roleDAO.findById(Integer.parseInt(idParam));
        if (role == null) {
            response.sendRedirect(request.getContextPath() + "/roles");
            return;
        }

        request.setAttribute("role", role);
        request.getRequestDispatcher("/views/role/edit-role.jsp").forward(request, response);
    }

    private void handleEdit(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        if (!hasPermission(request, "UPDATE_ROLE_INFORMATION")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String idParam     = request.getParameter("roleId");
        String roleName    = trim(request.getParameter("roleName"));
        String description = trim(request.getParameter("description"));

        if (idParam == null || roleName.isEmpty()) {
            Role role = new Role();
            role.setRoleId(idParam != null ? Integer.parseInt(idParam) : 0);
            role.setRoleName(roleName);
            role.setDescription(description);
            request.setAttribute("role", role);
            request.setAttribute("error", "Role name is required.");
            request.getRequestDispatcher("/views/role/edit-role.jsp").forward(request, response);
            return;
        }

        Role role = new Role();
        role.setRoleId(Integer.parseInt(idParam));
        role.setRoleName(roleName);
        role.setDescription(description);
        roleDAO.update(role);

        response.sendRedirect(request.getContextPath() + "/roles?updated=success");
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
