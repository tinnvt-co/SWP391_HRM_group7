package controller;

import config.DBContext;
import dao.ContractDocumentDAO;
import dao.DepartmentDAO;
import dao.EmployeeAccountRequestDAO;
import dao.EmployeeDAO;
import dao.ContractDAO;
import dao.RoleDAO;
import dao.UserDAO;
import model.Contract;
import model.ContractDocument;
import model.Department;
import model.Employee;
import model.EmployeeAccountRequest;
import model.Role;
import model.User;
import service.MailService;
import service.ContractDocumentStorage;
import util.PasswordUtil;

import jakarta.mail.MessagingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@WebServlet(name = "EmployeeAccountRequestServlet", urlPatterns = {"/employee-account-requests"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 10L * 1024 * 1024,
        maxRequestSize = 12L * 1024 * 1024
)
public class EmployeeAccountRequestServlet extends HttpServlet {

    private static final int PAGE_SIZE = 10;
    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private final EmployeeAccountRequestDAO requestDAO = new EmployeeAccountRequestDAO();
    private final ContractDocumentDAO documentDAO = new ContractDocumentDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final ContractDAO contractDAO = new ContractDAO();
    private final UserDAO userDAO = new UserDAO();
    private final RoleDAO roleDAO = new RoleDAO();
    private final MailService mailService = new MailService();
    private final ContractDocumentStorage documentStorage = new ContractDocumentStorage();
    private final SecureRandom random = new SecureRandom();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User currentUser = currentUser(request);
        boolean adminScope = isRole(currentUser, "ADMIN")
                && hasPermission(request, "APPROVE_EMPLOYEE_ACCOUNT_REQUEST");
        boolean requestScope = (isRole(currentUser, "HR_STAFF") || isRole(currentUser, "HR_MANAGER"))
                && hasPermission(request, "REQUEST_EMPLOYEE_ACCOUNT");

        if (!adminScope && !requestScope) {
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
            request.setAttribute("departments", requestDepartments(currentUser));
            request.setAttribute("requestRoles", allowedRequestRoles(currentUser));
            request.setAttribute("genders", User.Gender.values());
            request.setAttribute("contractTypes", Contract.ContractType.values());
            request.setAttribute("adminScope", adminScope);
            request.setAttribute("canRequestAccount", requestScope);
            request.setAttribute("hrStaffScope", isRole(currentUser, "HR_STAFF") && requestScope);
            request.setAttribute("hrManagerRequestScope", isRole(currentUser, "HR_MANAGER") && requestScope);
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
        String action = queryParam(request, "action");
        if (action == null || action.isBlank()) {
            action = request.getParameter("action");
        }
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
        } catch (IllegalStateException e) {
            flashError(request, "Contract document must be 10 MB or smaller.");
            response.sendRedirect(request.getContextPath() + "/employee-account-requests");
        }
    }

    private void handleCreateRequest(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        User currentUser = currentUser(request);
        if (!(isRole(currentUser, "HR_STAFF") || isRole(currentUser, "HR_MANAGER"))
                || !hasPermission(request, "REQUEST_EMPLOYEE_ACCOUNT")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        EmployeeAccountRequest accountRequest = readRequestForm(request);
        accountRequest.setRequestedBy(currentUser.getUserId());

        String error = validateRequestForm(accountRequest, currentUser);
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

        ContractDocument document;
        try {
            document = readUploadedRequestDocument(request, currentUser.getUserId());
        } catch (IOException | ServletException ex) {
            flashError(request, ex.getMessage());
            response.sendRedirect(request.getContextPath() + "/employee-account-requests");
            return;
        }
        applyDocumentMetadata(accountRequest, document);

        int requestId = requestDAO.insert(accountRequest);
        String contractCode = generatedContractCode(requestId);
        if (!requestDAO.updateContractCode(requestId, contractCode)) {
            throw new SQLException("Could not assign contract code.");
        }
        flash(request, "Contract and account request submitted to Admin.");
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

        Role requestedRole = resolveRequestedRole(accountRequest);
        if (requestedRole == null || !requestedRole.isActive()) {
            flashError(request, "The requested role is not configured or inactive.");
            response.sendRedirect(request.getContextPath() + "/employee-account-requests");
            return;
        }

        String roleError = validateRequestedRoleAndDepartment(accountRequest, requestedRole, null);
        if (roleError != null) {
            flashError(request, roleError);
            response.sendRedirect(request.getContextPath() + "/employee-account-requests");
            return;
        }

        if (isBlank(accountRequest.getContractCode()) || accountRequest.getContractType() == null
                || accountRequest.getContractStartDate() == null
                || accountRequest.getBasicSalary() == null
                || accountRequest.getStandardWorkingDays() == null) {
            flashError(request, "This request is missing contract information. Please reject it and ask HR to resubmit.");
            response.sendRedirect(request.getContextPath() + "/employee-account-requests");
            return;
        }
        if (contractExists(accountRequest.getContractCode())) {
            flashError(request, "Contract code '" + accountRequest.getContractCode() + "' already exists.");
            response.sendRedirect(request.getContextPath() + "/employee-account-requests");
            return;
        }
        if (!mailService.isConfigured()) {
            flashError(request, "Email is not configured. Please set MAIL_USERNAME and MAIL_PASSWORD "
                    + "or add mail.properties before approving account requests.");
            response.sendRedirect(request.getContextPath() + "/employee-account-requests");
            return;
        }

        String username = uniqueUsername(accountRequest.getEmail());
        String temporaryPassword = temporaryPassword(10);

        CreatedOnboarding created = createAccountEmployeeAndContract(
                accountRequest, requestedRole, username, temporaryPassword, admin.getUserId());

        String loginLink = buildLoginLink(request);
        try {
            mailService.sendAccountCreatedEmail(accountRequest.getEmail(), accountRequest.getFullName(),
                    username, temporaryPassword, loginLink);
            flash(request, "Account and contract created; notification email sent to "
                    + accountRequest.getEmail() + ".");
        } catch (MessagingException | UnsupportedEncodingException ex) {
            flashError(request, "Account and contract created, but email could not be sent: "
                    + ex.getMessage() + " (user #" + created.userId + ").");
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

        Integer requestedRoleId = parseIntOrNull(request.getParameter("requestedRoleId"));
        if (requestedRoleId != null) r.setRequestedRoleId(requestedRoleId);

        String hireDate = trim(request.getParameter("hireDate"));
        try {
            r.setHireDate(hireDate.isEmpty() ? LocalDate.now() : LocalDate.parse(hireDate));
        } catch (DateTimeParseException ex) {
            r.setHireDate(null);
        }

        String type = trim(request.getParameter("contractType"));
        if (!type.isEmpty()) {
            try { r.setContractType(Contract.ContractType.valueOf(type)); }
            catch (IllegalArgumentException ignored) {}
        }
        String contractStart = trim(request.getParameter("contractStartDate"));
        try {
            r.setContractStartDate(contractStart.isEmpty() ? r.getHireDate() : LocalDate.parse(contractStart));
        } catch (DateTimeParseException ex) {
            r.setContractStartDate(null);
        }
        String contractEnd = trim(request.getParameter("contractEndDate"));
        if (!contractEnd.isEmpty()) {
            try { r.setContractEndDate(LocalDate.parse(contractEnd)); }
            catch (DateTimeParseException ignored) {}
        }
        r.setBasicSalary(parsePositiveDecimal(request.getParameter("basicSalary")));
        BigDecimal workingDays = parsePositiveDecimal(request.getParameter("standardWorkingDays"));
        r.setStandardWorkingDays(workingDays == null ? new BigDecimal("26") : workingDays);
        r.setContractNote(limit(trim(request.getParameter("contractNote")), 255));
        return r;
    }

    private String validateRequestForm(EmployeeAccountRequest r, User requester) throws SQLException {
        if (r.getFullName().isEmpty() || r.getEmail().isEmpty() || isBlank(r.getPhone())
                || r.getGender() == null || r.getDateOfBirth() == null || isBlank(r.getAddress())
                || r.getDepartmentId() <= 0 || r.getHireDate() == null
                || r.getContractType() == null || r.getContractStartDate() == null
                || r.getBasicSalary() == null || r.getStandardWorkingDays() == null) {
            return "Please fill in all required fields.";
        }
        if (!r.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return "Please enter a valid email address.";
        }
        if (!r.getPhone().matches("^[0-9]{10,15}$")) {
            return "Phone number must contain digits only and be 10 to 15 characters long.";
        }
        if (r.getDateOfBirth().isAfter(LocalDate.now())) {
            return "Date of birth cannot be in the future.";
        }
        if (r.getDateOfBirth().isAfter(LocalDate.now().minusYears(18))) {
            return "Employee must be at least 18 years old.";
        }
        if (r.getContractStartDate().isBefore(r.getHireDate())) {
            return "Contract start date cannot be before hire date.";
        }
        if (r.getContractEndDate() != null
                && r.getContractEndDate().isBefore(r.getContractStartDate().plusMonths(1))) {
            return "Contract end date must be at least 1 month after contract start date.";
        }
        if (r.getBasicSalary().signum() < 0 || r.getStandardWorkingDays().signum() <= 0) {
            return "Salary must be non-negative and standard working days must be greater than 0.";
        }
        if (isRole(requester, "HR_MANAGER") && r.getRequestedRoleId() <= 0) {
            return "Please select a requested role.";
        }
        Role requestedRole = requestedRoleForRequester(r, requester);
        if (requestedRole == null || !requestedRole.isActive()) {
            return "The requested role is not configured or inactive.";
        }
        r.setRequestedRoleId(requestedRole.getRoleId());
        String roleError = validateRequestedRoleAndDepartment(r, requestedRole, requester);
        if (roleError != null) return roleError;
        Department department = departmentDAO.findById(r.getDepartmentId());
        r.setPositionTitle(generatedPositionTitle(requestedRole, department));
        return null;
    }

    private List<Department> requestDepartments(User requester) throws SQLException {
        return departmentDAO.findAllActive();
    }

    private List<Role> allowedRequestRoles(User requester) throws SQLException {
        List<Role> roles = new ArrayList<>();
        String[] roleNames = {"EMPLOYEE", "HR_STAFF", "MANAGER"};
        for (String roleName : roleNames) {
            Role role = roleDAO.findByName(roleName);
            if (role != null && role.isActive()) {
                roles.add(role);
            }
        }
        return roles;
    }

    private Role defaultRequestRole() throws SQLException {
        return roleDAO.findByName("EMPLOYEE");
    }

    private Role requestedRoleForRequester(EmployeeAccountRequest r, User requester) throws SQLException {
        if (isRole(requester, "HR_MANAGER")) {
            return r.getRequestedRoleId() > 0 ? roleDAO.findById(r.getRequestedRoleId()) : null;
        }
        return defaultRequestRole();
    }

    private String generatedPositionTitle(Role role, Department department) {
        String departmentName = department == null || isBlank(department.getDepartmentName())
                ? "Department"
                : department.getDepartmentName().trim();
        String roleName = role == null ? "" : role.getRoleName();
        if ("MANAGER".equalsIgnoreCase(roleName)) {
            return limit(departmentName + " Manager", 100);
        }
        if ("HR_STAFF".equalsIgnoreCase(roleName)) {
            return limit(departmentName + " Staff", 100);
        }
        return limit(departmentName + " Employee", 100);
    }

    private Role resolveRequestedRole(EmployeeAccountRequest r) throws SQLException {
        if (r.getRequestedRoleId() > 0) {
            return roleDAO.findById(r.getRequestedRoleId());
        }
        return roleDAO.findByName("EMPLOYEE");
    }

    private String validateRequestedRoleAndDepartment(EmployeeAccountRequest r, Role role, User requester)
            throws SQLException {
        if (role == null || !role.isActive()) {
            return "Please select an active role.";
        }
        String roleName = role.getRoleName();
        if (!("EMPLOYEE".equalsIgnoreCase(roleName)
                || "MANAGER".equalsIgnoreCase(roleName)
                || "HR_STAFF".equalsIgnoreCase(roleName))) {
            return "This role is not supported by the contract onboarding flow.";
        }

        Department dept = departmentDAO.findById(r.getDepartmentId());
        if (dept == null || !dept.isActive()) {
            return "Please select an active department.";
        }
        String departmentCode = dept.getDepartmentCode();
        if ("HR_STAFF".equalsIgnoreCase(roleName)) {
            if (!"HR".equalsIgnoreCase(departmentCode)) {
                return "HR Staff accounts must belong to the Human Resources department.";
            }
            return dept.getManagerId() == null
                    ? "Human Resources department has no HR Manager assigned."
                    : null;
        }
        if ("ADMIN_DEPT".equalsIgnoreCase(departmentCode)
                || "HR".equalsIgnoreCase(departmentCode)
                || "IT".equalsIgnoreCase(departmentCode)) {
            return roleName + " accounts cannot be assigned to Administration, HR, or IT department.";
        }
        if ("EMPLOYEE".equalsIgnoreCase(roleName) && dept.getManagerId() == null) {
            return "Selected department has no manager assigned.";
        }
        return null;
    }

    private CreatedOnboarding createAccountEmployeeAndContract(EmployeeAccountRequest r, Role role,
                                                               String username, String temporaryPassword,
                                                               int adminUserId) throws SQLException {
        Connection conn = null;
        try {
            conn = DBContext.getConnection();
            conn.setAutoCommit(false);

            Department dept = departmentDAO.findById(r.getDepartmentId());
            Integer managerId = managerIdForNewUser(role, dept);
            int newUserId = insertUser(conn, r, role, username, temporaryPassword, managerId);
            int employeeId = insertEmployee(conn, r, newUserId, adminUserId);
            String employeeCode = generatedEmployeeCode(employeeId);
            updateEmployeeCode(conn, employeeId, employeeCode, adminUserId);
            int contractId = insertContract(conn, r, employeeId, adminUserId);
            attachRequestDocument(conn, r, contractId);
            markRequestCreated(conn, r.getRequestId(), adminUserId, newUserId, employeeId,
                    contractId, "Created account " + username + ", employee " + employeeCode
                            + ", and contract " + r.getContractCode());

            conn.commit();
            return new CreatedOnboarding(newUserId, employeeId, contractId);
        } catch (SQLException ex) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw ex;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                DBContext.closeConnection(conn);
            }
        }
    }

    private Integer managerIdForNewUser(Role role, Department dept) {
        String roleName = role.getRoleName();
        if ("EMPLOYEE".equalsIgnoreCase(roleName) || "HR_STAFF".equalsIgnoreCase(roleName)) {
            return dept == null ? null : dept.getManagerId();
        }
        return null;
    }

    private int insertUser(Connection conn, EmployeeAccountRequest r, Role role,
                           String username, String temporaryPassword, Integer managerId)
            throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, full_name, email, phone, "
                   + "gender, date_of_birth, address, role_id, manager_id, is_active) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, PasswordUtil.hash(temporaryPassword));
            ps.setString(3, r.getFullName());
            ps.setString(4, r.getEmail());
            ps.setString(5, r.getPhone());
            ps.setString(6, r.getGender() == null ? User.Gender.Other.name() : r.getGender().name());
            if (r.getDateOfBirth() != null) ps.setDate(7, Date.valueOf(r.getDateOfBirth()));
            else                            ps.setNull(7, Types.DATE);
            ps.setString(8, r.getAddress());
            ps.setInt(9, role.getRoleId());
            if (managerId != null) ps.setInt(10, managerId);
            else                   ps.setNull(10, Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Could not create user.");
    }

    private int insertEmployee(Connection conn, EmployeeAccountRequest r, int userId,
                               int adminUserId) throws SQLException {
        String sql = "INSERT INTO employees (user_id, employee_code, department_id, "
                   + "hire_date, employment_status, created_by, updated_by) "
                   + "VALUES (?, ?, ?, ?, 'Working', ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, temporaryEmployeeCode(r.getRequestId()));
            ps.setInt(3, r.getDepartmentId());
            ps.setDate(4, Date.valueOf(r.getHireDate()));
            ps.setInt(5, adminUserId);
            ps.setInt(6, adminUserId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Could not create employee profile.");
    }

    private void updateEmployeeCode(Connection conn, int employeeId, String employeeCode, int adminUserId)
            throws SQLException {
        String sql = "UPDATE employees SET employee_code=?, updated_by=?, updated_at=NOW() "
                   + "WHERE employee_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, employeeCode);
            ps.setInt(2, adminUserId);
            ps.setInt(3, employeeId);
            if (ps.executeUpdate() <= 0) {
                throw new SQLException("Could not update employee code.");
            }
        }
    }

    private int insertContract(Connection conn, EmployeeAccountRequest r, int employeeId,
                               int adminUserId) throws SQLException {
        String sql = "INSERT INTO contracts (employee_id, contract_code, contract_type, start_date, end_date, "
                   + "basic_salary, standard_working_days, salary_policy, fixed_allowance_amount, "
                   + "is_system_contract, status, note, created_by, updated_by) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, 'Attendance Based', 0.00, 0, 'Active', ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, employeeId);
            ps.setString(2, r.getContractCode());
            ps.setString(3, r.getContractType().getDbValue());
            ps.setDate(4, Date.valueOf(r.getContractStartDate()));
            if (r.getContractEndDate() != null) ps.setDate(5, Date.valueOf(r.getContractEndDate()));
            else                                ps.setNull(5, Types.DATE);
            ps.setBigDecimal(6, r.getBasicSalary());
            ps.setBigDecimal(7, r.getStandardWorkingDays());
            ps.setString(8, isBlank(r.getContractNote()) ? null : r.getContractNote());
            ps.setInt(9, adminUserId);
            ps.setInt(10, adminUserId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Could not create contract.");
    }

    private void attachRequestDocument(Connection conn, EmployeeAccountRequest r, int contractId)
            throws SQLException {
        if (isBlank(r.getContractDocumentPath())) {
            throw new SQLException("Contract document is missing.");
        }
        ContractDocument document = new ContractDocument();
        document.setContractId(contractId);
        document.setOriginalFileName(r.getContractDocumentOriginalName());
        document.setStoredFileName(r.getContractDocumentStoredName());
        document.setRelativePath(r.getContractDocumentPath());
        document.setMimeType(r.getContractDocumentMimeType());
        document.setFileSize(r.getContractDocumentSize() == null ? 0L : r.getContractDocumentSize());
        document.setUploadedBy(r.getRequestedBy());
        documentDAO.replaceForContract(conn, document);
    }

    private void markRequestCreated(Connection conn, int requestId, int reviewedBy,
                                    int createdUserId, int createdEmployeeId,
                                    int createdContractId, String note) throws SQLException {
        String sql = "UPDATE employee_account_requests "
                   + "SET status='Created', reviewed_by=?, reviewed_at=NOW(), "
                   + "created_user_id=?, created_employee_id=?, created_contract_id=?, "
                   + "admin_note=?, updated_at=NOW() "
                   + "WHERE request_id=? AND status='Pending'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reviewedBy);
            ps.setInt(2, createdUserId);
            ps.setInt(3, createdEmployeeId);
            ps.setInt(4, createdContractId);
            ps.setString(5, note);
            ps.setInt(6, requestId);
            if (ps.executeUpdate() <= 0) {
                throw new SQLException("This account request is no longer pending.");
            }
        }
    }

    private boolean contractExists(String contractCode) throws SQLException {
        return !isBlank(contractCode) && contractDAO.existsByCode(contractCode);
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

    private String temporaryEmployeeCode(int requestId) {
        return String.format("REQ-%05d", requestId);
    }

    private String generatedEmployeeCode(int employeeId) {
        return String.format("MP%05d", employeeId);
    }

    private String generatedContractCode(int requestId) throws SQLException {
        if (requestId <= 0) {
            throw new SQLException("Could not create account request.");
        }
        String base = String.format("HD%05d", requestId);
        String candidate = base;
        int suffix = 1;
        while (contractDAO.existsByCode(candidate) || requestDAO.hasPendingByContractCode(candidate)) {
            candidate = String.format("%s_%02d", base, suffix++);
        }
        return candidate;
    }

    private ContractDocument readUploadedRequestDocument(HttpServletRequest request, int uploadedBy)
            throws IOException, ServletException {
        Part part;
        try {
            part = request.getPart("contractDocument");
        } catch (IllegalStateException ex) {
            throw new IOException("Contract document must be 10 MB or smaller.");
        }
        return documentStorage.save(getServletContext(), part, "account-requests", uploadedBy);
    }

    private void applyDocumentMetadata(EmployeeAccountRequest request, ContractDocument document) {
        request.setContractDocumentOriginalName(document.getOriginalFileName());
        request.setContractDocumentStoredName(document.getStoredFileName());
        request.setContractDocumentPath(document.getRelativePath());
        request.setContractDocumentMimeType(document.getMimeType());
        request.setContractDocumentSize(document.getFileSize());
    }

    private BigDecimal parsePositiveDecimal(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            BigDecimal n = new BigDecimal(value.trim().replace(",", ""));
            return n.signum() < 0 ? null : n;
        } catch (NumberFormatException ex) {
            return null;
        }
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

    private String queryParam(HttpServletRequest request, String name) {
        String query = request.getQueryString();
        if (query == null || query.isBlank()) return null;
        for (String part : query.split("&")) {
            int eq = part.indexOf('=');
            String key = eq >= 0 ? part.substring(0, eq) : part;
            if (name.equals(key)) {
                return eq >= 0 ? part.substring(eq + 1) : "";
            }
        }
        return null;
    }

    private String limit(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static final class CreatedOnboarding {
        final int userId;
        final int employeeId;
        final int contractId;

        CreatedOnboarding(int userId, int employeeId, int contractId) {
            this.userId = userId;
            this.employeeId = employeeId;
            this.contractId = contractId;
        }
    }
}
