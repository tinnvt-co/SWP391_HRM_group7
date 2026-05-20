package controller;

import dao.RoleDAO;
import dao.UserDAO;
import model.User;
import model.User.Gender;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@WebServlet(name = "UserServlet", urlPatterns = {"/users"})
public class UserServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final RoleDAO roleDAO = new RoleDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!hasPermission(request, "VIEW_USER_LIST")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = request.getParameter("action");
        if (action == null) action = "list";

        try {
            switch (action) {
                case "view" -> handleView(request, response);
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
                default       -> response.sendRedirect(request.getContextPath() + "/users");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void handleList(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        request.setAttribute("users", userDAO.findAll());
        request.getRequestDispatcher("/views/user/user-list.jsp").forward(request, response);
    }

    private void handleView(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        if (!hasPermission(request, "VIEW_USER_INFORMATION")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/users");
            return;
        }

        User user = userDAO.findById(Integer.parseInt(idParam));
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/users");
            return;
        }

        request.setAttribute("user", user);
        request.getRequestDispatcher("/views/user/view-user.jsp").forward(request, response);
    }

    private void handleAddForm(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        if (!hasPermission(request, "ADD_NEW_USER")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        request.setAttribute("roles", roleDAO.findAllActive());
        request.setAttribute("genders", Gender.values());
        request.getRequestDispatcher("/views/user/add-user.jsp").forward(request, response);
    }

    private void handleAdd(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        if (!hasPermission(request, "ADD_NEW_USER")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String username  = trim(request.getParameter("username"));
        String password  = request.getParameter("password");
        String fullName  = trim(request.getParameter("fullName"));
        String email     = trim(request.getParameter("email"));
        String phone     = trim(request.getParameter("phone"));
        String genderStr = request.getParameter("gender");
        String dobStr    = trim(request.getParameter("dateOfBirth"));
        String address   = trim(request.getParameter("address"));
        String roleIdStr = request.getParameter("roleId");

        if (username.isEmpty() || password == null || password.isBlank()
                || fullName.isEmpty() || email.isEmpty() || roleIdStr == null) {
            forwardAddForm(request, response, "Please fill in all required fields.");
            return;
        }

        if (userDAO.findByUsername(username) != null) {
            forwardAddForm(request, response, "Username '" + username + "' is already taken.");
            return;
        }

        if (userDAO.findByEmail(email) != null) {
            forwardAddForm(request, response, "Email '" + email + "' is already in use.");
            return;
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(password);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setAddress(address);
        user.setRoleId(Integer.parseInt(roleIdStr));

        if (genderStr != null && !genderStr.isBlank()) {
            try { user.setGender(Gender.valueOf(genderStr)); } catch (IllegalArgumentException ignored) {}
        }

        if (!dobStr.isEmpty()) {
            try { user.setDateOfBirth(LocalDate.parse(dobStr)); } catch (DateTimeParseException ignored) {}
        }

        userDAO.insert(user);
        response.sendRedirect(request.getContextPath() + "/users?added=success");
    }

    private void handleEditForm(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        if (!hasPermission(request, "UPDATE_USER_INFORMATION")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/users");
            return;
        }

        User user = userDAO.findById(Integer.parseInt(idParam));
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/users");
            return;
        }

        request.setAttribute("user", user);
        request.setAttribute("roles", roleDAO.findAllActive());
        request.setAttribute("genders", Gender.values());
        request.getRequestDispatcher("/views/user/edit-user.jsp").forward(request, response);
    }

    private void handleEdit(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        if (!hasPermission(request, "UPDATE_USER_INFORMATION")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String idParam   = request.getParameter("userId");
        String fullName  = trim(request.getParameter("fullName"));
        String email     = trim(request.getParameter("email"));
        String phone     = trim(request.getParameter("phone"));
        String genderStr = request.getParameter("gender");
        String dobStr    = trim(request.getParameter("dateOfBirth"));
        String address   = trim(request.getParameter("address"));
        String roleIdStr = request.getParameter("roleId");

        if (idParam == null || fullName.isEmpty() || email.isEmpty() || roleIdStr == null) {
            forwardEditForm(request, response, Integer.parseInt(idParam), "Please fill in all required fields.");
            return;
        }

        int userId = Integer.parseInt(idParam);
        User existing = userDAO.findByEmail(email);
        if (existing != null && existing.getUserId() != userId) {
            forwardEditForm(request, response, userId, "Email '" + email + "' is already in use.");
            return;
        }

        User user = new User();
        user.setUserId(userId);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setAddress(address);
        user.setRoleId(Integer.parseInt(roleIdStr));

        if (genderStr != null && !genderStr.isBlank()) {
            try { user.setGender(Gender.valueOf(genderStr)); } catch (IllegalArgumentException ignored) {}
        }
        if (!dobStr.isEmpty()) {
            try { user.setDateOfBirth(LocalDate.parse(dobStr)); } catch (DateTimeParseException ignored) {}
        }

        userDAO.update(user);
        response.sendRedirect(request.getContextPath() + "/users?updated=success");
    }

    private void handleToggle(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {

        if (!hasPermission(request, "ACTIVE_DEACTIVE_USER")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String idParam       = request.getParameter("userId");
        String currentStatus = request.getParameter("currentStatus");

        if (idParam == null || currentStatus == null) {
            response.sendRedirect(request.getContextPath() + "/users");
            return;
        }

        HttpSession session  = request.getSession(false);
        User currentUser     = (User) session.getAttribute("currentUser");
        int targetId         = Integer.parseInt(idParam);

        if (currentUser.getUserId() == targetId) {
            response.sendRedirect(request.getContextPath() + "/users?toggleError=self");
            return;
        }

        boolean newStatus = !"true".equals(currentStatus);
        userDAO.setActiveStatus(targetId, newStatus);
        response.sendRedirect(request.getContextPath() + "/users?toggled=success");
    }

    private void forwardEditForm(HttpServletRequest request, HttpServletResponse response,
                                  int userId, String error)
            throws SQLException, ServletException, IOException {
        request.setAttribute("error", error);
        request.setAttribute("user", userDAO.findById(userId));
        request.setAttribute("roles", roleDAO.findAllActive());
        request.setAttribute("genders", Gender.values());
        request.getRequestDispatcher("/views/user/edit-user.jsp").forward(request, response);
    }

    private void forwardAddForm(HttpServletRequest request, HttpServletResponse response, String error)
            throws SQLException, ServletException, IOException {
        request.setAttribute("error", error);
        request.setAttribute("roles", roleDAO.findAllActive());
        request.setAttribute("genders", Gender.values());
        request.getRequestDispatcher("/views/user/add-user.jsp").forward(request, response);
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
