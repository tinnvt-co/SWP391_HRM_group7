package controller;

import dao.AttendanceRecordDAO;
import dao.EmployeeDAO;
import model.AttendanceRecord;
import model.AttendanceRecord.AttendanceStatus;
import model.AttendanceRecord.VerificationStatus;
import model.Employee;
import model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@WebServlet(name = "AttendanceServlet", urlPatterns = {"/attendance"})
public class AttendanceServlet extends HttpServlet {

    private final AttendanceRecordDAO attendanceDAO = new AttendanceRecordDAO();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        if (action == null) action = "list";

        try {
            switch (action) {
                case "add" -> {
                    if (!hasPermission(request, "VERIFY_STAFF_ATTENDANCE")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleAddForm(request, response);
                }
                case "edit" -> {
                    if (!hasPermission(request, "VERIFY_STAFF_ATTENDANCE")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleEditForm(request, response);
                }
                default -> {
                    if (!hasPermission(request, "VIEW_ATTENDANCE")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleList(request, response);
                }
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
                case "add" -> {
                    if (!hasPermission(request, "VERIFY_STAFF_ATTENDANCE")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleAdd(request, response);
                }
                case "edit" -> {
                    if (!hasPermission(request, "VERIFY_STAFF_ATTENDANCE")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleEdit(request, response);
                }
                case "delete" -> {
                    if (!hasPermission(request, "VERIFY_STAFF_ATTENDANCE")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleDelete(request, response);
                }
                case "verify" -> {
                    if (!hasPermission(request, "VERIFY_STAFF_ATTENDANCE")) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    handleVerify(request, response);
                }
                default -> response.sendRedirect(request.getContextPath() + "/attendance");
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void handleList(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        User currentUser = getCurrentUser(request);
        String roleName  = currentUser.getRole() != null ? currentUser.getRole().getRoleName() : "";

        boolean managerScope = "MANAGER".equalsIgnoreCase(roleName);
        boolean orgWide      = "ADMIN".equalsIgnoreCase(roleName)
                            || "HR_STAFF".equalsIgnoreCase(roleName)
                            || "HR_MANAGER".equalsIgnoreCase(roleName);

        Integer employeeIdFilter = parseIntOrNull(request.getParameter("employeeId"));
        LocalDate fromDate = parseDateOrNull(request.getParameter("fromDate"));
        LocalDate toDate   = parseDateOrNull(request.getParameter("toDate"));

        // Attendance never exists in the future: clamp filter dates to today.
        LocalDate today = LocalDate.now();
        if (fromDate != null && fromDate.isAfter(today)) fromDate = today;
        if (toDate != null && toDate.isAfter(today)) toDate = today;

        List<Employee> scopeEmployees;
        List<AttendanceRecord> records;

        if (managerScope) {
            scopeEmployees = employeeDAO.findByManagerUserId(currentUser.getUserId());
            records = attendanceDAO.findByManagerScope(currentUser.getUserId(), employeeIdFilter, fromDate, toDate);
        } else if (orgWide) {
            scopeEmployees = employeeDAO.findAllActive();
            records = attendanceDAO.findAll(employeeIdFilter, fromDate, toDate);
        } else {
            Employee me = employeeDAO.findByUserId(currentUser.getUserId());
            scopeEmployees = java.util.Collections.emptyList();
            records = (me != null)
                    ? attendanceDAO.findByEmployeeId(me.getEmployeeId(), fromDate, toDate)
                    : java.util.Collections.emptyList();
        }

        request.setAttribute("records", records);
        request.setAttribute("scopeEmployees", scopeEmployees);
        request.setAttribute("managerScope", managerScope);
        request.setAttribute("orgWide", orgWide);
        request.setAttribute("employeeIdFilter", employeeIdFilter);
        request.setAttribute("fromDate", fromDate);
        request.setAttribute("toDate", toDate);
        request.getRequestDispatcher("/views/attendance/attendance-list.jsp")
               .forward(request, response);
    }

    private void handleAddForm(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        User currentUser = getCurrentUser(request);
        List<Employee> employees = employeeDAO.findByManagerUserId(currentUser.getUserId());

        request.setAttribute("employees", employees);
        request.setAttribute("statuses", AttendanceStatus.values());
        request.getRequestDispatcher("/views/attendance/add-attendance.jsp")
               .forward(request, response);
    }

    private void handleAdd(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        User currentUser = getCurrentUser(request);
        List<Employee> employees = employeeDAO.findByManagerUserId(currentUser.getUserId());

        String employeeIdStr = request.getParameter("employeeId");
        String workDateStr   = trim(request.getParameter("workDate"));
        String statusStr     = request.getParameter("attendanceStatus");
        String checkInStr    = trim(request.getParameter("checkInTime"));
        String checkOutStr   = trim(request.getParameter("checkOutTime"));
        String overtimeStr   = trim(request.getParameter("overtimeHours"));
        String note          = trim(request.getParameter("note"));

        if (employeeIdStr == null || employeeIdStr.isBlank()
                || workDateStr.isEmpty() || statusStr == null || statusStr.isBlank()) {
            forwardAddForm(request, response, employees,
                    "Please fill in all required fields (employee, date, status).");
            return;
        }

        int employeeId;
        try { employeeId = Integer.parseInt(employeeIdStr); }
        catch (NumberFormatException ex) {
            forwardAddForm(request, response, employees, "Invalid employee.");
            return;
        }

        boolean ownsEmployee = employees.stream().anyMatch(e -> e.getEmployeeId() == employeeId);
        if (!ownsEmployee) {
            forwardAddForm(request, response, employees,
                    "You can only create attendance for your own subordinates.");
            return;
        }

        LocalDate workDate;
        try { workDate = LocalDate.parse(workDateStr); }
        catch (DateTimeParseException ex) {
            forwardAddForm(request, response, employees, "Invalid work date.");
            return;
        }

        if (workDate.isAfter(LocalDate.now())) {
            forwardAddForm(request, response, employees, "Work date cannot be in the future.");
            return;
        }

        AttendanceStatus status;
        try { status = AttendanceStatus.valueOf(statusStr); }
        catch (IllegalArgumentException ex) {
            forwardAddForm(request, response, employees, "Invalid attendance status.");
            return;
        }

        LocalTime checkIn  = parseTimeOrNull(checkInStr);
        LocalTime checkOut = parseTimeOrNull(checkOutStr);

        boolean requiresTime =
                status == AttendanceStatus.Present || status == AttendanceStatus.Late;
        if (requiresTime && (checkIn == null || checkOut == null)) {
            forwardAddForm(request, response, employees,
                    "Check-in and check-out time are required when status is Present or Late.");
            return;
        }
        if (checkIn != null && checkOut != null && checkOut.isBefore(checkIn)) {
            forwardAddForm(request, response, employees,
                    "Check-out time must be after check-in time.");
            return;
        }

        BigDecimal overtimeHours = BigDecimal.ZERO;
        if (!overtimeStr.isEmpty()) {
            try {
                overtimeHours = new BigDecimal(overtimeStr);
                if (overtimeHours.signum() < 0) {
                    forwardAddForm(request, response, employees, "Overtime hours cannot be negative.");
                    return;
                }
            } catch (NumberFormatException ex) {
                forwardAddForm(request, response, employees, "Invalid overtime hours.");
                return;
            }
        }

        if (note.length() > 255) {
            forwardAddForm(request, response, employees, "Note must be 255 characters or fewer.");
            return;
        }

        if (attendanceDAO.existsByEmployeeAndDate(employeeId, workDate)) {
            forwardAddForm(request, response, employees,
                    "An attendance record for this employee on this date already exists.");
            return;
        }

        BigDecimal workingHours = BigDecimal.ZERO;
        if (checkIn != null && checkOut != null) {
            long minutes = Duration.between(checkIn, checkOut).toMinutes();
            workingHours = BigDecimal.valueOf(minutes)
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        }

        AttendanceRecord r = new AttendanceRecord();
        r.setEmployeeId(employeeId);
        r.setWorkDate(workDate);
        r.setCheckInTime(checkIn);
        r.setCheckOutTime(checkOut);
        r.setWorkingHours(workingHours);
        r.setOvertimeHours(overtimeHours);
        r.setAttendanceStatus(status);
        r.setVerificationStatus(VerificationStatus.Pending);
        r.setNote(note);

        attendanceDAO.insert(r);
        response.sendRedirect(request.getContextPath() + "/attendance?created=success");
    }

    private void forwardAddForm(HttpServletRequest request, HttpServletResponse response,
                                List<Employee> employees, String error)
            throws ServletException, IOException {
        request.setAttribute("error", error);
        request.setAttribute("employees", employees);
        request.setAttribute("statuses", AttendanceStatus.values());
        request.getRequestDispatcher("/views/attendance/add-attendance.jsp")
               .forward(request, response);
    }

    private void handleEditForm(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        AttendanceRecord existing = loadOwnedRecordOrError(request, response);
        if (existing == null) return;

        if (existing.getVerificationStatus() == VerificationStatus.Verified) {
            response.sendRedirect(request.getContextPath()
                    + "/attendance?error=already-verified");
            return;
        }

        request.setAttribute("record", existing);
        request.setAttribute("statuses", AttendanceStatus.values());
        request.getRequestDispatcher("/views/attendance/edit-attendance.jsp")
               .forward(request, response);
    }

    private void handleEdit(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        AttendanceRecord existing = loadOwnedRecordOrError(request, response);
        if (existing == null) return;

        if (existing.getVerificationStatus() == VerificationStatus.Verified) {
            response.sendRedirect(request.getContextPath()
                    + "/attendance?error=already-verified");
            return;
        }

        String workDateStr  = trim(request.getParameter("workDate"));
        String statusStr    = request.getParameter("attendanceStatus");
        String checkInStr   = trim(request.getParameter("checkInTime"));
        String checkOutStr  = trim(request.getParameter("checkOutTime"));
        String overtimeStr  = trim(request.getParameter("overtimeHours"));
        String note         = trim(request.getParameter("note"));

        if (workDateStr.isEmpty() || statusStr == null || statusStr.isBlank()) {
            forwardEditForm(request, response, existing,
                    "Please fill in all required fields (date, status).");
            return;
        }

        LocalDate workDate;
        try { workDate = LocalDate.parse(workDateStr); }
        catch (DateTimeParseException ex) {
            forwardEditForm(request, response, existing, "Invalid work date.");
            return;
        }

        if (workDate.isAfter(LocalDate.now())) {
            forwardEditForm(request, response, existing, "Work date cannot be in the future.");
            return;
        }

        AttendanceStatus status;
        try { status = AttendanceStatus.valueOf(statusStr); }
        catch (IllegalArgumentException ex) {
            forwardEditForm(request, response, existing, "Invalid attendance status.");
            return;
        }

        LocalTime checkIn  = parseTimeOrNull(checkInStr);
        LocalTime checkOut = parseTimeOrNull(checkOutStr);

        boolean requiresTime =
                status == AttendanceStatus.Present || status == AttendanceStatus.Late;
        if (requiresTime && (checkIn == null || checkOut == null)) {
            forwardEditForm(request, response, existing,
                    "Check-in and check-out time are required when status is Present or Late.");
            return;
        }
        if (checkIn != null && checkOut != null && checkOut.isBefore(checkIn)) {
            forwardEditForm(request, response, existing,
                    "Check-out time must be after check-in time.");
            return;
        }

        BigDecimal overtimeHours = BigDecimal.ZERO;
        if (!overtimeStr.isEmpty()) {
            try {
                overtimeHours = new BigDecimal(overtimeStr);
                if (overtimeHours.signum() < 0) {
                    forwardEditForm(request, response, existing,
                            "Overtime hours cannot be negative.");
                    return;
                }
            } catch (NumberFormatException ex) {
                forwardEditForm(request, response, existing, "Invalid overtime hours.");
                return;
            }
        }

        if (note.length() > 255) {
            forwardEditForm(request, response, existing,
                    "Note must be 255 characters or fewer.");
            return;
        }

        if (!workDate.isEqual(existing.getWorkDate())
                && attendanceDAO.existsByEmployeeAndDate(existing.getEmployeeId(), workDate)) {
            forwardEditForm(request, response, existing,
                    "Another attendance record for this employee on this date already exists.");
            return;
        }

        BigDecimal workingHours = BigDecimal.ZERO;
        if (checkIn != null && checkOut != null) {
            long minutes = Duration.between(checkIn, checkOut).toMinutes();
            workingHours = BigDecimal.valueOf(minutes)
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        }

        existing.setWorkDate(workDate);
        existing.setCheckInTime(checkIn);
        existing.setCheckOutTime(checkOut);
        existing.setWorkingHours(workingHours);
        existing.setOvertimeHours(overtimeHours);
        existing.setAttendanceStatus(status);
        existing.setNote(note);

        attendanceDAO.update(existing);
        response.sendRedirect(request.getContextPath() + "/attendance?updated=success");
    }

    private void forwardEditForm(HttpServletRequest request, HttpServletResponse response,
                                 AttendanceRecord record, String error)
            throws ServletException, IOException {
        request.setAttribute("error", error);
        request.setAttribute("record", record);
        request.setAttribute("statuses", AttendanceStatus.values());
        request.getRequestDispatcher("/views/attendance/edit-attendance.jsp")
               .forward(request, response);
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {

        AttendanceRecord existing = loadOwnedRecordOrError(request, response);
        if (existing == null) return;

        if (existing.getVerificationStatus() == VerificationStatus.Verified) {
            response.sendRedirect(request.getContextPath()
                    + "/attendance?error=already-verified");
            return;
        }

        attendanceDAO.deleteById(existing.getAttendanceId());
        response.sendRedirect(request.getContextPath() + "/attendance?deleted=success");
    }

    private void handleVerify(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {

        AttendanceRecord existing = loadOwnedRecordOrError(request, response);
        if (existing == null) return;

        if (existing.getVerificationStatus() == VerificationStatus.Verified) {
            response.sendRedirect(request.getContextPath()
                    + "/attendance?error=already-verified");
            return;
        }

        User currentUser = getCurrentUser(request);
        attendanceDAO.verify(existing.getAttendanceId(), currentUser.getUserId());
        response.sendRedirect(request.getContextPath() + "/attendance?verified=success");
    }

    private AttendanceRecord loadOwnedRecordOrError(HttpServletRequest request,
                                                    HttpServletResponse response)
            throws SQLException, IOException {

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            response.sendRedirect(request.getContextPath() + "/attendance");
            return null;
        }

        int attendanceId;
        try { attendanceId = Integer.parseInt(idParam); }
        catch (NumberFormatException ex) {
            response.sendRedirect(request.getContextPath() + "/attendance");
            return null;
        }

        AttendanceRecord existing = attendanceDAO.findById(attendanceId);
        if (existing == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        User currentUser = getCurrentUser(request);
        boolean ownsThis = existing.getEmployeeManagerUserId() != null
                && existing.getEmployeeManagerUserId() == currentUser.getUserId();

        if (!ownsThis) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        return existing;
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

    private Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s); } catch (NumberFormatException ex) { return null; }
    }

    private LocalDate parseDateOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s); } catch (DateTimeParseException ex) { return null; }
    }

    private LocalTime parseTimeOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            if (s.length() == 5) return LocalTime.parse(s);
            return LocalTime.parse(s);
        } catch (DateTimeParseException ex) { return null; }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
