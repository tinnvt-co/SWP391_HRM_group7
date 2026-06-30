package controller;

import dao.DepartmentDAO;
import dao.EmployeeDAO;
import dao.RoleDAO;
import dao.UserDAO;
import model.Department;
import model.Employee;
import model.Employee.EmploymentStatus;
import model.Role;
import model.User;
import model.User.Gender;
import util.PasswordUtil;

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
    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();

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

    private static final int PAGE_SIZE = 10;

    private void handleList(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        int totalUsers = userDAO.countAll();
        int totalPages = Math.max(1, (int) Math.ceil(totalUsers / (double) PAGE_SIZE));

        int page = parsePageParam(request.getParameter("page"), totalPages);
        int offset = (page - 1) * PAGE_SIZE;

        request.setAttribute("users", userDAO.findPage(offset, PAGE_SIZE));
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalUsers", totalUsers);
        request.getRequestDispatcher("/views/user/user-list.jsp").forward(request, response);
    }

    private int parsePageParam(String pageParam, int totalPages) {
        int page = 1;
        if (pageParam != null && !pageParam.isBlank()) {
            try { page = Integer.parseInt(pageParam); } catch (NumberFormatException ignored) {}
        }
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        return page;
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

        setUserFormLookups(request);
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
        String deptIdStr = request.getParameter("departmentId");

        if (username.isEmpty() || password == null || password.isBlank()
                || fullName.isEmpty() || email.isEmpty() || roleIdStr == null) {
            forwardAddForm(request, response, "Please fill in all required fields.");
            return;
        }

        String validationError = validateUserInput(email, phone, dobStr);
        if (validationError != null) {
            forwardAddForm(request, response, validationError);
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

        int roleId = Integer.parseInt(roleIdStr);
        Department assignedDepartment;
        try {
            assignedDepartment = resolveDepartmentForRole(roleId, deptIdStr);
        } catch (IllegalArgumentException ex) {
            forwardAddForm(request, response, ex.getMessage());
            return;
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setAddress(address);
        user.setRoleId(roleId);
        user.setManagerId(managerIdForRole(roleId, assignedDepartment));

        if (genderStr != null && !genderStr.isBlank()) {
            try { user.setGender(Gender.valueOf(genderStr)); } catch (IllegalArgumentException ignored) {}
        }

        if (!dobStr.isEmpty()) {
            try { user.setDateOfBirth(LocalDate.parse(dobStr)); } catch (DateTimeParseException ignored) {}
        }

        int newUserId = userDAO.insert(user);
        upsertEmployeeProfileIfNeeded(newUserId, roleId, assignedDepartment, getCurrentUser(request).getUserId());
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
        request.setAttribute("employee", employeeDAO.findByUserId(user.getUserId()));
        setUserFormLookups(request);
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
        String deptIdStr = request.getParameter("departmentId");

        if (idParam == null || fullName.isEmpty() || email.isEmpty() || roleIdStr == null) {
            forwardEditForm(request, response, Integer.parseInt(idParam), "Please fill in all required fields.");
            return;
        }

        String validationError = validateUserInput(email, phone, dobStr);
        if (validationError != null) {
            forwardEditForm(request, response, Integer.parseInt(idParam), validationError);
            return;
        }

        int userId = Integer.parseInt(idParam);
        User existing = userDAO.findByEmail(email);
        if (existing != null && existing.getUserId() != userId) {
            forwardEditForm(request, response, userId, "Email '" + email + "' is already in use.");
            return;
        }

        int roleId = Integer.parseInt(roleIdStr);
        Department assignedDepartment;
        try {
            assignedDepartment = resolveDepartmentForRole(roleId, deptIdStr);
        } catch (IllegalArgumentException ex) {
            forwardEditForm(request, response, userId, ex.getMessage());
            return;
        }

        User user = new User();
        user.setUserId(userId);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setAddress(address);
        user.setRoleId(roleId);
        user.setManagerId(managerIdForRole(roleId, assignedDepartment));

        if (genderStr != null && !genderStr.isBlank()) {
            try { user.setGender(Gender.valueOf(genderStr)); } catch (IllegalArgumentException ignored) {}
        }
        if (!dobStr.isEmpty()) {
            try { user.setDateOfBirth(LocalDate.parse(dobStr)); } catch (DateTimeParseException ignored) {}
        }

        userDAO.update(user);
        upsertEmployeeProfileIfNeeded(userId, roleId, assignedDepartment, getCurrentUser(request).getUserId());
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
        request.setAttribute("employee", employeeDAO.findByUserId(userId));
        setUserFormLookups(request);
        request.getRequestDispatcher("/views/user/edit-user.jsp").forward(request, response);
    }

    private void forwardAddForm(HttpServletRequest request, HttpServletResponse response, String error)
            throws SQLException, ServletException, IOException {
        request.setAttribute("error", error);
        setUserFormLookups(request);
        request.getRequestDispatcher("/views/user/add-user.jsp").forward(request, response);
    }

    private void setUserFormLookups(HttpServletRequest request) throws SQLException {
        request.setAttribute("roles", roleDAO.findAllActive());
        request.setAttribute("genders", Gender.values());
        request.setAttribute("employeeDepartments", departmentDAO.findEmployeeAssignable());
    }

    private void upsertEmployeeProfileIfNeeded(int userId, int roleId,
                                               Department assignedDepartment,
                                               int actorUserId) throws SQLException {
        Role role = roleDAO.findById(roleId);
        if (role == null || !requiresEmployeeProfile(role.getRoleName())) return;
        if (assignedDepartment == null) return;

        Employee employee = new Employee();
        employee.setUserId(userId);
        employee.setEmployeeCode(generateEmployeeCode(userId));
        employee.setDepartmentId(assignedDepartment.getDepartmentId());
        employee.setHireDate(LocalDate.now());
        employee.setEmploymentStatus(EmploymentStatus.Working);
        employeeDAO.upsertBasicProfile(employee, actorUserId);
    }

    private Department resolveDepartmentForRole(int roleId, String deptIdStr) throws SQLException {
        Role role = roleDAO.findById(roleId);
        if (role == null) throw new IllegalArgumentException("Invalid role.");

        String roleName = role.getRoleName();
        if ("EMPLOYEE".equalsIgnoreCase(roleName)) {
            if (deptIdStr == null || deptIdStr.isBlank()) {
                throw new IllegalArgumentException("Please select a department for employee.");
            }
            int deptId;
            try { deptId = Integer.parseInt(deptIdStr); }
            catch (NumberFormatException ex) { throw new IllegalArgumentException("Invalid department."); }

            Department dept = departmentDAO.findById(deptId);
            if (dept == null) throw new IllegalArgumentException("Invalid department.");
            if ("ADMIN_DEPT".equalsIgnoreCase(dept.getDepartmentCode())
                    || "HR".equalsIgnoreCase(dept.getDepartmentCode())) {
                throw new IllegalArgumentException("Employee cannot be assigned to Administration or HR department.");
            }
            if (dept.getManagerId() == null) {
                throw new IllegalArgumentException("Selected department has no manager assigned.");
            }
            return dept;
        }

        if ("HR_STAFF".equalsIgnoreCase(roleName)) {
            Department hr = departmentDAO.findByCode("HR");
            if (hr == null) throw new IllegalArgumentException("HR department is not configured.");
            if (hr.getManagerId() == null) throw new IllegalArgumentException("HR department has no HR Manager assigned.");
            return hr;
        }

        if ("MANAGER".equalsIgnoreCase(roleName) || "HR_MANAGER".equalsIgnoreCase(roleName)) {
            Department dept = "HR_MANAGER".equalsIgnoreCase(roleName)
                    ? departmentDAO.findByCode("HR")
                    : departmentDAO.findByCode("ADMIN_DEPT");
            if (dept == null) throw new IllegalArgumentException("Default department is not configured.");
            return dept;
        }

        return null;
    }

    private Integer managerIdForRole(int roleId, Department assignedDepartment) throws SQLException {
        Role role = roleDAO.findById(roleId);
        if (role == null || assignedDepartment == null) return null;
        String roleName = role.getRoleName();
        if ("EMPLOYEE".equalsIgnoreCase(roleName) || "HR_STAFF".equalsIgnoreCase(roleName)) {
            return assignedDepartment.getManagerId();
        }
        return null;
    }

    private boolean requiresEmployeeProfile(String roleName) {
        return "EMPLOYEE".equalsIgnoreCase(roleName)
                || "MANAGER".equalsIgnoreCase(roleName)
                || "HR_STAFF".equalsIgnoreCase(roleName)
                || "HR_MANAGER".equalsIgnoreCase(roleName);
    }

    private String generateEmployeeCode(int userId) {
        return String.format("MP-U%05d", userId);
    }

    private String validateUserInput(String email, String phone, String dobStr) {
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return "Please enter a valid email address (e.g., user@company.com).";
        }

        if (phone != null && !phone.isBlank() && !phone.matches("^[0-9]{10,15}$")) {
            return "Phone number must contain digits only and be 10 to 15 characters long.";
        }

        if (dobStr != null && !dobStr.isBlank()) {
            try {
                LocalDate dob = LocalDate.parse(dobStr);
                if (dob.isAfter(LocalDate.now())) {
                    return "Date of birth cannot be in the future.";
                }
            } catch (DateTimeParseException ex) {
                return "Invalid date of birth.";
            }
        }

        return null;
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

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
