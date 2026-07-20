package controller;

import dao.AllowanceTypeDAO;
import dao.ContractDocumentDAO;
import dao.ContractDAO;
import dao.EmployeeDAO;
import model.Contract;
import model.ContractDocument;
import model.Contract.ContractType;
import model.Contract.Status;
import model.Employee;
import model.User;
import service.ContractDocumentStorage;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@WebServlet(name = "ContractServlet", urlPatterns = {"/contracts"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 10L * 1024 * 1024,
        maxRequestSize = 12L * 1024 * 1024
)
public class ContractServlet extends HttpServlet {

    private final ContractDAO contractDAO = new ContractDAO();
    private final ContractDocumentDAO documentDAO = new ContractDocumentDAO();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final AllowanceTypeDAO allowanceDAO = new AllowanceTypeDAO();
    private final ContractDocumentStorage documentStorage = new ContractDocumentStorage();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!hasPermission(request, "VIEW_CONTRACT_LIST")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String action = request.getParameter("action");
        if (action == null) action = "list";

        try {
            switch (action) {
                case "add"  -> handleAddForm(request, response);
                case "edit" -> handleEditForm(request, response);
                case "view" -> handleView(request, response);
                default     -> handleList(request, response);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = actionFromQuery(request);
        if (action == null || action.isBlank()) {
            action = request.getParameter("action");
        }
        if (action == null) action = "";

        try {
            switch (action) {
                case "add"       -> handleAdd(request, response);
                case "edit"      -> handleEdit(request, response);
                case "terminate" -> handleTerminate(request, response);
                default          -> response.sendRedirect(request.getContextPath() + "/contracts");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        } catch (IllegalStateException e) {
            handleUploadTooLarge(request, response, action);
        }
    }

    private static final int PAGE_SIZE = 10;

    private void handleList(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        String statusParam = request.getParameter("status");
        Status statusFilter = null;
        if (statusParam != null && !statusParam.isBlank() && !"all".equalsIgnoreCase(statusParam)) {
            try { statusFilter = Status.valueOf(statusParam); }
            catch (IllegalArgumentException ignored) { statusParam = "all"; }
        }

        int totalContracts = contractDAO.countAll(statusFilter);
        int totalPages = Math.max(1, (int) Math.ceil(totalContracts / (double) PAGE_SIZE));

        int page = 1;
        String pageParam = request.getParameter("page");
        if (pageParam != null && !pageParam.isBlank()) {
            try { page = Integer.parseInt(pageParam); } catch (NumberFormatException ignored) {}
        }
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        int offset = (page - 1) * PAGE_SIZE;

        request.setAttribute("contracts", contractDAO.findPage(statusFilter, offset, PAGE_SIZE));
        request.setAttribute("statusFilter", statusParam == null ? "all" : statusParam);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalContracts", totalContracts);
        request.setAttribute("canCreateContract", canCreateContract(request));
        request.setAttribute("canEditSystemContracts", canEditSystemContracts(request));
        request.getRequestDispatcher("/views/contract/contract-list.jsp").forward(request, response);
    }

    private void handleView(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/contracts");
            return;
        }
        Contract c = contractDAO.findById(Integer.parseInt(idParam));
        if (c == null) {
            response.sendRedirect(request.getContextPath() + "/contracts");
            return;
        }
        request.setAttribute("contract", c);
        request.setAttribute("readonly", true);
        request.setAttribute("systemContract", c.isSystemContract());
        request.setAttribute("contractTypes", ContractType.values());
        loadAllowanceAttributes(request);
        request.getRequestDispatcher("/views/contract/edit-contract.jsp").forward(request, response);
    }

    private void handleAddForm(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        if (!canCreateContract(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        request.setAttribute("employees", employeeDAO.findAllActive());
        request.setAttribute("contractTypes", ContractType.values());
        loadAllowanceAttributes(request);
        request.getRequestDispatcher("/views/contract/add-contract.jsp").forward(request, response);
    }

    private void handleAdd(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        if (!canCreateContract(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String empIdStr   = request.getParameter("employeeId");
        String code       = trim(request.getParameter("contractCode"));
        String typeStr    = request.getParameter("contractType");
        String startStr   = trim(request.getParameter("startDate"));
        String endStr     = trim(request.getParameter("endDate"));
        String note       = trim(request.getParameter("note"));

        if (empIdStr == null || empIdStr.isBlank() || code.isEmpty()
                || typeStr == null || typeStr.isBlank() || startStr.isEmpty()) {
            forwardAddForm(request, response, "Please fill in all required fields.");
            return;
        }

        int employeeId;
        try { employeeId = Integer.parseInt(empIdStr); }
        catch (NumberFormatException ex) {
            forwardAddForm(request, response, "Invalid employee.");
            return;
        }

        if (employeeDAO.findById(employeeId) == null) {
            forwardAddForm(request, response, "Selected employee does not exist.");
            return;
        }

        ContractType type;
        try { type = ContractType.valueOf(typeStr); }
        catch (IllegalArgumentException ex) {
            forwardAddForm(request, response, "Invalid contract type.");
            return;
        }

        if (contractDAO.existsByCode(code)) {
            forwardAddForm(request, response, "Contract code '" + code + "' already exists.");
            return;
        }

        if (contractDAO.hasActiveContract(employeeId)) {
            forwardAddForm(request, response,
                    "This employee already has an active contract. Terminate it before creating a new one.");
            return;
        }

        LocalDate startDate;
        try { startDate = LocalDate.parse(startStr); }
        catch (DateTimeParseException ex) {
            forwardAddForm(request, response, "Invalid start date.");
            return;
        }

        LocalDate endDate = null;
        if (!endStr.isEmpty()) {
            try { endDate = LocalDate.parse(endStr); }
            catch (DateTimeParseException ex) {
                forwardAddForm(request, response, "Invalid end date.");
                return;
            }
            if (endDate.isBefore(startDate)) {
                forwardAddForm(request, response, "End date must be on or after start date.");
                return;
            }
        }

        String salaryError = null;
        BigDecimal basicSalary  = parsePositiveMoney(request.getParameter("basicSalary"));
        BigDecimal workingDays  = parsePositiveMoney(request.getParameter("standardWorkingDays"));
        if (basicSalary == null || workingDays == null) {
            salaryError = "Salary and working days must be valid non-negative numbers.";
        } else if (workingDays.signum() <= 0) {
            salaryError = "Standard working days must be greater than 0.";
        }
        if (salaryError != null) {
            forwardAddForm(request, response, salaryError);
            return;
        }

        if (note.length() > 255) {
            forwardAddForm(request, response, "Note must be 255 characters or fewer.");
            return;
        }

        User currentUser = getCurrentUser(request);
        ContractDocument document;
        try {
            document = readUploadedDocument(request, true, "manual-contracts", currentUser.getUserId());
        } catch (IOException ex) {
            forwardAddForm(request, response, ex.getMessage());
            return;
        }

        Contract c = new Contract();
        c.setEmployeeId(employeeId);
        c.setContractCode(code);
        c.setContractType(type);
        c.setStartDate(startDate);
        c.setEndDate(endDate);
        c.setBasicSalary(basicSalary);
        c.setStandardWorkingDays(workingDays);
        c.setStatus(Status.Active);
        c.setNote(note.isEmpty() ? null : note);
        c.setCreatedBy(currentUser.getUserId());
        c.setUpdatedBy(currentUser.getUserId());

        int contractId = contractDAO.insert(c);
        document.setContractId(contractId);
        documentDAO.replaceForContract(document);
        response.sendRedirect(request.getContextPath() + "/contracts?added=success");
    }

    private void handleEditForm(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        if (!hasPermission(request, "UPDATE_CONTRACT")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/contracts");
            return;
        }

        Contract c = contractDAO.findById(Integer.parseInt(idParam));
        if (c == null) {
            response.sendRedirect(request.getContextPath() + "/contracts");
            return;
        }

        boolean readonly = c.getStatus() != Status.Active
                || (c.isSystemContract() && !canEditSystemContracts(request));
        request.setAttribute("contract", c);
        request.setAttribute("contractTypes", ContractType.values());
        request.setAttribute("readonly", readonly);
        request.setAttribute("systemContract", c.isSystemContract());
        request.setAttribute("canEditSystemContract", c.isSystemContract() && canEditSystemContracts(request));
        loadAllowanceAttributes(request);
        request.getRequestDispatcher("/views/contract/edit-contract.jsp").forward(request, response);
    }

    private void handleEdit(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        if (!hasPermission(request, "UPDATE_CONTRACT")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String idParam  = request.getParameter("contractId");
        String typeStr  = request.getParameter("contractType");
        String startStr = trim(request.getParameter("startDate"));
        String endStr   = trim(request.getParameter("endDate"));
        String note     = trim(request.getParameter("note"));

        if (idParam == null || idParam.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/contracts");
            return;
        }

        int contractId = Integer.parseInt(idParam);
        Contract existing = contractDAO.findById(contractId);
        if (existing == null) {
            response.sendRedirect(request.getContextPath() + "/contracts");
            return;
        }

        if (existing.getStatus() != Status.Active) {
            response.sendRedirect(request.getContextPath()
                    + "/contracts?error=not-active");
            return;
        }
        if (existing.isSystemContract() && !canEditSystemContracts(request)) {
            forwardEditForm(request, response, contractId,
                    "System contracts are seeded from the database and cannot be edited.");
            return;
        }

        ContractType type;
        try { type = ContractType.valueOf(typeStr); }
        catch (IllegalArgumentException ex) {
            forwardEditForm(request, response, contractId, "Invalid contract type.");
            return;
        }

        if (startStr.isEmpty()) {
            forwardEditForm(request, response, contractId, "Start date is required.");
            return;
        }

        LocalDate startDate;
        try { startDate = LocalDate.parse(startStr); }
        catch (DateTimeParseException ex) {
            forwardEditForm(request, response, contractId, "Invalid start date.");
            return;
        }

        LocalDate endDate = null;
        if (!endStr.isEmpty()) {
            try { endDate = LocalDate.parse(endStr); }
            catch (DateTimeParseException ex) {
                forwardEditForm(request, response, contractId, "Invalid end date.");
                return;
            }
            if (endDate.isBefore(startDate)) {
                forwardEditForm(request, response, contractId, "End date must be on or after start date.");
                return;
            }
        }

        BigDecimal basicSalary    = parsePositiveMoney(request.getParameter("basicSalary"));
        BigDecimal workingDays    = parsePositiveMoney(request.getParameter("standardWorkingDays"));
        if (basicSalary == null || workingDays == null) {
            forwardEditForm(request, response, contractId,
                    "Salary and working days must be valid non-negative numbers.");
            return;
        }
        if (workingDays.signum() <= 0) {
            forwardEditForm(request, response, contractId, "Standard working days must be greater than 0.");
            return;
        }
        if (note.length() > 255) {
            forwardEditForm(request, response, contractId, "Note must be 255 characters or fewer.");
            return;
        }

        User currentUser = getCurrentUser(request);
        ContractDocument document;
        try {
            document = readUploadedDocument(request, false, "manual-contracts", currentUser.getUserId());
        } catch (IOException ex) {
            forwardEditForm(request, response, contractId, ex.getMessage());
            return;
        }

        Contract c = new Contract();
        c.setContractId(contractId);
        c.setContractType(type);
        c.setStartDate(startDate);
        c.setEndDate(endDate);
        c.setBasicSalary(basicSalary);
        c.setStandardWorkingDays(workingDays);
        c.setNote(note.isEmpty() ? null : note);
        c.setUpdatedBy(currentUser.getUserId());

        if (!contractDAO.update(c, canEditSystemContracts(request))) {
            forwardEditForm(request, response, contractId,
                    "System contracts are seeded from the database and cannot be edited.");
            return;
        }
        if (document != null) {
            document.setContractId(contractId);
            documentDAO.replaceForContract(document);
        }
        response.sendRedirect(request.getContextPath() + "/contracts?updated=success");
    }

    private void handleTerminate(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {

        if (!hasPermission(request, "TERMINATE_CONTRACT")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String idParam = request.getParameter("contractId");
        if (idParam == null || idParam.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/contracts");
            return;
        }

        User currentUser = getCurrentUser(request);
        boolean terminated = contractDAO.terminate(Integer.parseInt(idParam), currentUser.getUserId());
        response.sendRedirect(request.getContextPath() + "/contracts?"
                + (terminated ? "terminated=success" : "error=system-contract"));
    }

    private void forwardAddForm(HttpServletRequest request, HttpServletResponse response, String error)
            throws SQLException, ServletException, IOException {
        request.setAttribute("error", error);
        request.setAttribute("employees", employeeDAO.findAllActive());
        request.setAttribute("contractTypes", ContractType.values());
        loadAllowanceAttributes(request);
        request.getRequestDispatcher("/views/contract/add-contract.jsp").forward(request, response);
    }

    private void forwardEditForm(HttpServletRequest request, HttpServletResponse response,
                                 int contractId, String error)
            throws SQLException, ServletException, IOException {
        request.setAttribute("error", error);
        Contract contract = contractDAO.findById(contractId);
        request.setAttribute("contract", contract);
        boolean readonly = contract == null
                || contract.getStatus() != Status.Active
                || (contract.isSystemContract() && !canEditSystemContracts(request));
        request.setAttribute("readonly", readonly);
        request.setAttribute("systemContract", contract != null && contract.isSystemContract());
        request.setAttribute("canEditSystemContract", contract != null
                && contract.isSystemContract()
                && canEditSystemContracts(request));
        request.setAttribute("contractTypes", ContractType.values());
        loadAllowanceAttributes(request);
        request.getRequestDispatcher("/views/contract/edit-contract.jsp").forward(request, response);
    }

    private ContractDocument readUploadedDocument(HttpServletRequest request, boolean required,
                                                  String namespace, int uploadedBy)
            throws IOException, ServletException {
        Part part;
        try {
            part = request.getPart("contractDocument");
        } catch (IllegalStateException ex) {
            throw new IOException("Contract document must be 10 MB or smaller.");
        }
        if (part == null || part.getSize() <= 0) {
            if (required) {
                throw new IOException("Please upload the signed contract document.");
            }
            return null;
        }
        return documentStorage.save(getServletContext(), part, namespace, uploadedBy);
    }

    private void loadAllowanceAttributes(HttpServletRequest request) throws SQLException {
        User currentUser = getCurrentUser(request);
        allowanceDAO.ensureResponsibilityAllowances(
                currentUser == null ? null : currentUser.getUserId());
        request.setAttribute("activeAllowanceTypes", allowanceDAO.findActive());
        request.setAttribute("totalActiveAllowance", allowanceDAO.sumActiveAllowances());
        request.setAttribute("commonActiveAllowance", allowanceDAO.sumCommonActiveAllowances());
    }

    private BigDecimal parsePositiveMoney(String value) {
        if (value == null || value.isBlank()) return BigDecimal.ZERO;
        try {
            BigDecimal d = new BigDecimal(value.trim());
            return d.signum() < 0 ? null : d;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("currentUser");
    }

    private boolean canCreateContract(HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        String roleName = currentUser != null && currentUser.getRole() != null
                ? currentUser.getRole().getRoleName()
                : "";
        return hasPermission(request, "CREATE_CONTRACT")
                && !"HR_STAFF".equalsIgnoreCase(roleName);
    }

    private boolean canEditSystemContracts(HttpServletRequest request) {
        User currentUser = getCurrentUser(request);
        return currentUser != null
                && currentUser.getRole() != null
                && "HR_MANAGER".equalsIgnoreCase(currentUser.getRole().getRoleName())
                && hasPermission(request, "UPDATE_CONTRACT");
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

    private String actionFromQuery(HttpServletRequest request) {
        return queryParam(request, "action");
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

    private void handleUploadTooLarge(HttpServletRequest request, HttpServletResponse response,
                                      String action)
            throws ServletException, IOException {
        String message = "Contract document must be 10 MB or smaller.";
        try {
            if ("edit".equals(action)) {
                String idParam = queryParam(request, "id");
                if (idParam == null || idParam.isBlank()) {
                    response.sendRedirect(request.getContextPath() + "/contracts?error=upload-too-large");
                    return;
                }
                forwardEditForm(request, response, Integer.parseInt(idParam), message);
                return;
            }
            forwardAddForm(request, response, message);
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }
    }
}
