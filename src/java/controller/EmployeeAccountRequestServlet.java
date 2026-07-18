package controller;

import dao.DepartmentDAO;
import dao.EmployeeAccountRequestDAO;
import dao.EmployeeDAO;
import dao.RoleDAO;
import dao.UserDAO;
import model.Department;
import model.Employee;
import model.EmployeeAccountRequest;
import model.Role;
import model.User;
import service.MailService;
import util.PasswordUtil;

import jakarta.mail.MessagingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

@WebServlet(name = "EmployeeAccountRequestServlet", urlPatterns = {"/employee-account-requests"})
public class EmployeeAccountRequestServlet extends HttpServlet {

    private static final int PAGE_SIZE = 10;
    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private final EmployeeAccountRequestDAO requestDAO = new EmployeeAccountRequestDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final UserDAO userDAO = new UserDAO();
    private final RoleDAO roleDAO = new RoleDAO();
    private final MailService mailService = new MailService();
    private final SecureRandom random = new SecureRandom();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User currentUser = currentUser(request);
        boolean adminScope = isRole(currentUser, "ADMIN")
                && hasPermission(request, "APPROVE_EMPLOYEE_ACCOUNT_REQUEST");
        boolean hrStaffScope = isRole(currentUser, "HR_STAFF")
                && hasPermission(request, "REQUEST_EMPLOYEE_ACCOUNT");

        if (!adminScope && !hrStaffScope) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            int totalRequests = requestDAO.countForUser(adminScope, currentUser.getUserId());
            int totalPages = Math.max(1, (int) Math.ceil(totalRequests / (double) PAGE_SIZE));
            int page = parsePageParam(request.getParameter("page"), totalPages);
            int offset = (page - 1) * PAGE_SIZE;

            request.setAttribute("requests", requestDAO.findPageForUser(
                    adminScope, currentUser.getUserId(), offset, PAGE_SIZE));
            request.setAttribute("departments", departmentDAO.findEmployeeAssignable());
            request.setAttribute("genders", User.Gender.values());
            request.setAttribute("adminScope", adminScope);
            request.setAttribute("hrStaffScope", hrStaffScope);
            request.setAttribute("currentPage", page);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("totalRequests", totalRequests);
            request.setAttribute("today", LocalDate.now());
            readFlash(request);
            request.getRequestDispatcher("/views/user/employee-account-requests.jsp")
                   .forward(request, response);
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
                case "createRequest" -> handleCreateRequest(request, response);
                case "approve"       -> handleApprove(request, response);
                case "reject"        -> handleReject(request, response);
                default              -> response.sendRedirect(request.getContextPath() + "/employee-account-requests");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void handleCreateRequest(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        User currentUser = currentUser(request);
        if (!isRole(currentUser, "HR_STAFF") || !hasPermission(request, "REQUEST_EMPLOYEE_ACCOUNT")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        EmployeeAccountRequest accountRequest = readRequestForm(request);
        accountRequest.setRequestedBy(currentUser.getUserId());

        String error = validateRequestForm(accountRequest);
        if (error != null) {
            flashError(request, error);
            response.sendRedirect(request.getContextPath() + "/employee-account-requests");
            return;
        }

        if (userDAO.findByEmail(accountRequest.getEmail()) != null) {
            flashError(request, "This email is already linked to an existing account.");
            response.sendRedirect(request.getContextPath() + "/employee-account-requests");
            return;
        }
        if (requestDAO.hasPendingByEmail(accountRequest.getEmail())) {
            flashError(request, "There is already a pending account request for this email.");
            response.sendRedirect(request.getContextPath() + "/employee-account-requests");
            return;
        }
        if (!isBlank(accountRequest.getEmployeeCode())
                && employeeDAO.existsByEmployeeCode(accountRequest.getEmployeeCode())) {
            flashError(request, "Employee code '" + accountRequest.getEmployeeCode() + "' already exists.");
            response.sendRedirect(request.getContextPath() + "/employee-account-requests");
            return;
        }

        requestDAO.insert(accountRequest);
        flash(request, "Employee account request submitted to Admin.");
        response.sendRedirect(request.getContextPath() + "/employee-account-requests");
    }

    private void handleApprove(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        User admin = currentUser(request);
        if (!isRole(admin, "ADMIN") || !hasPermission(request, "APPROVE_EMPLOYEE_ACCOUNT_REQUEST")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Integer requestId = parseIntOrNull(request.getParameter("requestId"));
        EmployeeAccountRequest accountRequest = requestId == null ? null : requestDAO.findById(requestId);
        if (accountRequest == null || accountRequest.getStatus() != EmployeeAccountRequest.Status.Pending) {
            flashError(request, "This account request is no longer pending.");
            response.sendRedirect(request.getContextPath() + "/employee-account-requests");
            return;
        }

        if (userDAO.findByEmail(accountRequest.getEmail()) != null) {
            flashError(request, "This email is already linked to an existing account.");
            response.sendRedirect(request.getContextPath() + "/employee-account-requests");
            return;
        }

        Department dept = departmentDAO.findById(accountRequest.getDepartmentId());
        if (dept == null || dept.getManagerId() == null) {
            flashError(request, "The selected department is missing or has no manager.");
            response.sendRedirect(request.getContextPath() + "/employee-account-requests");
            return;
        }

        Role employeeRole = roleDAO.findByName("EMPLOYEE");
        if (employeeRole == null) {
            flashError(request, "Employee role is not configured.");
            response.sendRedirect(request.getContextPath() + "/employee-account-requests");
            return;
        }

        String requestedCode = trim(accountRequest.getEmployeeCode());
        if (!requestedCode.isEmpty() && employeeDAO.existsByEmployeeCode(requestedCode)) {
            flashError(request, "Employee code '" + requestedCode + "' already exists.");
            response.sendRedirect(request.getContextPath() + "/employee-account-requests");
            return;
        }

        String username = uniqueUsername(accountRequest.getEmail());
        String temporaryPassword = temporaryPassword(10);

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPasswordHash(PasswordUtil.hash(temporaryPassword));
        newUser.setFullName(accountRequest.getFullName());
        newUser.setEmail(accountRequest.getEmail());
        newUser.setPhone(accountRequest.getPhone());
        newUser.setGender(accountRequest.getGender() == null ? User.Gender.Other : accountRequest.getGender());
        newUser.setDateOfBirth(accountRequest.getDateOfBirth());
        newUser.setAddress(accountRequest.getAddress());
        newUser.setRoleId(employeeRole.getRoleId());
        newUser.setManagerId(dept.getManagerId());
        int newUserId = userDAO.insert(newUser);

        Employee employee = new Employee();
        employee.setUserId(newUserId);
        employee.setEmployeeCode(requestedCode.isEmpty() ? generatedEmployeeCode(newUserId) : requestedCode);
        employee.setDepartmentId(accountRequest.getDepartmentId());
        employee.setHireDate(accountRequest.getHireDate());
        employee.setEmploymentStatus(Employee.EmploymentStatus.Working);
        employeeDAO.upsertBasicProfile(employee, admin.getUserId());
        Employee createdEmployee = employeeDAO.findByUserId(newUserId);

        requestDAO.markCreated(accountRequest.getRequestId(), admin.getUserId(), newUserId,
                createdEmployee == null ? null : createdEmployee.getEmployeeId(),
                "Created account " + username);

        String loginLink = buildLoginLink(request);
        try {
            mailService.sendAccountCreatedEmail(accountRequest.getEmail(), accountRequest.getFullName(),
                    username, temporaryPassword, loginLink);
            flash(request, "Account created and notification email sent to " + accountRequest.getEmail() + ".");
        } catch (MessagingException | UnsupportedEncodingException ex) {
            flashError(request, "Account created, but email could not be sent: " + ex.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/employee-account-requests");
    }

    private void handleReject(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        User admin = currentUser(request);
        if (!isRole(admin, "ADMIN") || !hasPermission(request, "APPROVE_EMPLOYEE_ACCOUNT_REQUEST")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Integer requestId = parseIntOrNull(request.getParameter("requestId"));
        String note = trim(request.getParameter("note"));
        if (requestId == null || note.isEmpty()) {
            flashError(request, "Please enter a rejection reason.");
            response.sendRedirect(request.getContextPath() + "/employee-account-requests");
            return;
        }
        boolean updated = requestDAO.markRejected(requestId, admin.getUserId(), limit(note, 500));
        if (updated) {
            flash(request, "Account request rejected.");
        } else {
            flashError(request, "This account request is no longer pending.");
        }
        response.sendRedirect(request.getContextPath() + "/employee-account-requests");
    }

    private EmployeeAccountRequest readRequestForm(HttpServletRequest request) {
        EmployeeAccountRequest r = new EmployeeAccountRequest();
        r.setFullName(trim(request.getParameter("fullName")));
        r.setEmail(trim(request.getParameter("email")).toLowerCase(Locale.ROOT));
        r.setPhone(trim(request.getParameter("phone")));
        r.setAddress(trim(request.getParameter("address")));
        r.setEmployeeCode(trim(request.getParameter("employeeCode")).toUpperCase(Locale.ROOT));

        String gender = request.getParameter("gender");
        if (!isBlank(gender)) {
            try { r.setGender(User.Gender.valueOf(gender)); } catch (IllegalArgumentException ignored) {}
        }

        String dob = trim(request.getParameter("dateOfBirth"));
        if (!dob.isEmpty()) {
            try { r.setDateOfBirth(LocalDate.parse(dob)); } catch (DateTimeParseException ignored) {}
        }

        Integer deptId = parseIntOrNull(request.getParameter("departmentId"));
        if (deptId != null) r.setDepartmentId(deptId);

        String hireDate = trim(request.getParameter("hireDate"));
        try {
            r.setHireDate(hireDate.isEmpty() ? LocalDate.now() : LocalDate.parse(hireDate));
        } catch (DateTimeParseException ex) {
            r.setHireDate(null);
        }
        return r;
    }

    private String validateRequestForm(EmployeeAccountRequest r) throws SQLException {
        if (r.getFullName().isEmpty() || r.getEmail().isEmpty()
                || r.getDepartmentId() <= 0 || r.getHireDate() == null) {
            return "Please fill in all required fields.";
        }
        if (!r.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return "Please enter a valid email address.";
        }
        if (!isBlank(r.getPhone()) && !r.getPhone().matches("^[0-9]{10,15}$")) {
            return "Phone number must contain digits only and be 10 to 15 characters long.";
        }
        if (r.getDateOfBirth() != null && r.getDateOfBirth().isAfter(LocalDate.now())) {
            return "Date of birth cannot be in the future.";
        }
        if (r.getHireDate().isAfter(LocalDate.now())) {
            return "Hire date cannot be in the future.";
        }
        if (!isBlank(r.getEmployeeCode()) && !r.getEmployeeCode().matches("^[A-Z0-9_-]{2,20}$")) {
            return "Employee code must be 2 to 20 characters and contain letters, digits, dash, or underscore only.";
        }
        Department dept = departmentDAO.findById(r.getDepartmentId());
        if (dept == null || !dept.isActive()) {
            return "Please select an active department.";
        }
        if ("ADMIN_DEPT".equalsIgnoreCase(dept.getDepartmentCode())
                || "HR".equalsIgnoreCase(dept.getDepartmentCode())) {
            return "Employee cannot be assigned to Administration or HR department.";
        }
        if (dept.getManagerId() == null) {
            return "Selected department has no manager assigned.";
        }
        return null;
    }

    private String uniqueUsername(String email) throws SQLException {
        String localPart = email == null ? "" : email.split("@", 2)[0];
        String base = localPart.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "");
        if (base.isBlank()) base = "employee";

        String candidate = base;
        int i = 1;
        while (userDAO.findByUsername(candidate) != null) {
            candidate = base + i;
            i++;
        }
        return candidate;
    }

    private String temporaryPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(random.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    private String generatedEmployeeCode(int userId) {
        return String.format("MP-U%05d", userId);
    }

    private String buildLoginLink(HttpServletRequest request) {
        int port = request.getServerPort();
        String portPart = (port == 80 || port == 443) ? "" : ":" + port;
        return request.getScheme() + "://" + request.getServerName() + portPart
                + request.getContextPath() + "/login";
    }

    private User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("currentUser");
    }

    private boolean hasPermission(HttpServletRequest request, String permCode) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        List<?> permissions = (List<?>) session.getAttribute("permissions");
        return permissions != null && permissions.contains(permCode);
    }

    private boolean isRole(User user, String roleName) {
        return user != null && user.getRole() != null
                && roleName.equalsIgnoreCase(user.getRole().getRoleName());
    }

    private int parsePageParam(String pageParam, int totalPages) {
        int page = 1;
        if (!isBlank(pageParam)) {
            try { page = Integer.parseInt(pageParam.trim()); } catch (NumberFormatException ignored) {}
        }
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        return page;
    }

    private Integer parseIntOrNull(String value) {
        if (isBlank(value)) return null;
        try { return Integer.parseInt(value.trim()); } catch (NumberFormatException ex) { return null; }
    }

    private void readFlash(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return;
        Object msg = session.getAttribute("accountRequestMessage");
        Object err = session.getAttribute("accountRequestError");
        if (msg != null) {
            request.setAttribute("accountRequestMessage", msg);
            session.removeAttribute("accountRequestMessage");
        }
        if (err != null) {
            request.setAttribute("accountRequestError", err);
            session.removeAttribute("accountRequestError");
        }
    }

    private void flash(HttpServletRequest request, String message) {
        request.getSession(true).setAttribute("accountRequestMessage", message);
    }

    private void flashError(HttpServletRequest request, String message) {
        request.getSession(true).setAttribute("accountRequestError", message);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String limit(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
