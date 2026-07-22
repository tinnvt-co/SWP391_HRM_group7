package dao;

import config.DBContext;
import model.Employee;
import model.Employee.EmploymentStatus;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {

    public Employee findByUserId(int userId) throws SQLException {
        String sql = "SELECT e.*, u.full_name, d.department_name "
                   + "FROM employees e "
                   + "JOIN users u       ON e.user_id       = u.user_id "
                   + "JOIN departments d ON e.department_id = d.department_id "
                   + "WHERE e.user_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    public int countByRoleName(String roleName) throws SQLException {
        String sql = "SELECT COUNT(*) "
                   + "FROM employees e "
                   + "JOIN users u  ON e.user_id = u.user_id "
                   + "JOIN roles ro ON u.role_id = ro.role_id "
                   + "WHERE ro.role_name = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, roleName);
            rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }

    public int countEmployeesInManagedDepartments(int managerUserId) throws SQLException {
        String sql = "SELECT COUNT(*) "
                   + "FROM employees e "
                   + "JOIN users u       ON e.user_id       = u.user_id "
                   + "JOIN roles ro      ON u.role_id       = ro.role_id "
                   + "JOIN departments d ON e.department_id = d.department_id "
                   + "WHERE d.manager_id = ? "
                   + "AND u.is_active = 1 "
                   + "AND ro.role_name = 'EMPLOYEE'";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, managerUserId);
            rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }

    public Employee findById(int employeeId) throws SQLException {
        String sql = "SELECT e.*, u.full_name, d.department_name "
                   + "FROM employees e "
                   + "JOIN users u       ON e.user_id       = u.user_id "
                   + "JOIN departments d ON e.department_id = d.department_id "
                   + "WHERE e.employee_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, employeeId);
            rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    public String findRoleNameByEmployeeId(int employeeId) throws SQLException {
        String sql = "SELECT ro.role_name "
                   + "FROM employees e "
                   + "JOIN users u  ON e.user_id = u.user_id "
                   + "JOIN roles ro ON u.role_id = ro.role_id "
                   + "WHERE e.employee_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, employeeId);
            rs = ps.executeQuery();
            return rs.next() ? rs.getString("role_name") : null;
        } finally {
            close(conn, ps, rs);
        }
    }

    public List<Employee> findByManagerUserId(int managerUserId) throws SQLException {
        String sql = "SELECT e.*, u.full_name, d.department_name "
                   + "FROM employees e "
                   + "JOIN users u       ON e.user_id       = u.user_id "
                   + "JOIN departments d ON e.department_id = d.department_id "
                   + "WHERE u.manager_id = ? AND u.is_active = 1 "
                   + "ORDER BY u.full_name";
        List<Employee> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, managerUserId);
            rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public void upsertBasicProfile(Employee employee, int actorUserId) throws SQLException {
        Employee existing = findByUserId(employee.getUserId());
        if (existing == null) {
            insertBasicProfile(employee, actorUserId);
        } else {
            employee.setEmployeeId(existing.getEmployeeId());
            updateBasicProfile(employee, actorUserId);
        }
    }

    private void insertBasicProfile(Employee e, int actorUserId) throws SQLException {
        String sql = "INSERT INTO employees (user_id, employee_code, department_id, "
                   + "hire_date, employment_status, created_by, updated_by) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, e.getUserId());
            ps.setString(2, e.getEmployeeCode());
            ps.setInt(3, e.getDepartmentId());
            ps.setDate(4, Date.valueOf(e.getHireDate()));
            ps.setString(5, e.getEmploymentStatus() == null ? EmploymentStatus.Working.name() : e.getEmploymentStatus().name());
            ps.setInt(6, actorUserId);
            ps.setInt(7, actorUserId);
            ps.executeUpdate();
        } finally {
            close(conn, ps, null);
        }
    }

    private void updateBasicProfile(Employee e, int actorUserId) throws SQLException {
        String sql = "UPDATE employees SET employee_code=?, department_id=?, "
                   + "hire_date=?, employment_status=?, updated_by=?, updated_at=NOW() "
                   + "WHERE user_id=?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, e.getEmployeeCode());
            ps.setInt(2, e.getDepartmentId());
            ps.setDate(3, Date.valueOf(e.getHireDate()));
            ps.setString(4, e.getEmploymentStatus() == null ? EmploymentStatus.Working.name() : e.getEmploymentStatus().name());
            ps.setInt(5, actorUserId);
            ps.setInt(6, e.getUserId());
            ps.executeUpdate();
        } finally {
            close(conn, ps, null);
        }
    }

    public boolean updateBankInfo(int userId, String bankName, String bankAccountNumber,
                                  String bankBranch, int actorUserId) throws SQLException {
        String sql = "UPDATE employees SET bank_name=?, bank_account_number=?, bank_branch=?, "
                   + "updated_by=?, updated_at=NOW() WHERE user_id=?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, bankName);
            ps.setString(2, bankAccountNumber);
            ps.setString(3, bankBranch);
            ps.setInt(4, actorUserId);
            ps.setInt(5, userId);
            return ps.executeUpdate() > 0;
        } finally {
            close(conn, ps, null);
        }
    }

    public boolean existsByEmployeeCode(String employeeCode) throws SQLException {
        String sql = "SELECT 1 FROM employees WHERE employee_code = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, employeeCode);
            rs = ps.executeQuery();
            return rs.next();
        } finally {
            close(conn, ps, rs);
        }
    }

    public List<Employee> findByRoleName(String roleName) throws SQLException {
        String sql = "SELECT e.*, u.full_name, u.username, u.email, u.phone, u.is_active, "
                   + "       d.department_name "
                   + "FROM employees e "
                   + "JOIN users u       ON e.user_id       = u.user_id "
                   + "JOIN roles ro      ON u.role_id       = ro.role_id "
                   + "JOIN departments d ON e.department_id = d.department_id "
                   + "WHERE ro.role_name = ? "
                   + "ORDER BY u.full_name";
        List<Employee> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, roleName);
            rs = ps.executeQuery();
            while (rs.next()) {
                Employee e = mapRow(rs);
                e.setUsername(rs.getString("username"));
                e.setEmail(rs.getString("email"));
                e.setPhone(rs.getString("phone"));
                list.add(e);
            }
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public int countByRoleName(String roleName, String keyword) throws SQLException {
        String sql = "SELECT COUNT(*) "
                   + "FROM employees e "
                   + "JOIN users u       ON e.user_id       = u.user_id "
                   + "JOIN roles ro      ON u.role_id       = ro.role_id "
                   + "JOIN departments d ON e.department_id = d.department_id "
                   + "WHERE ro.role_name = ? ";
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (hasKeyword) {
            sql += "AND (LOWER(u.full_name) LIKE ? "
                + "OR LOWER(e.employee_code) LIKE ? "
                + "OR LOWER(u.email) LIKE ? "
                + "OR LOWER(d.department_name) LIKE ?) ";
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, roleName);
            if (hasKeyword) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                ps.setString(2, like);
                ps.setString(3, like);
                ps.setString(4, like);
                ps.setString(5, like);
            }
            rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } finally {
            close(conn, ps, rs);
        }
    }

    public List<Employee> findByRoleNamePage(String roleName, String keyword,
                                             int offset, int limit) throws SQLException {
        String sql = "SELECT e.*, u.full_name, u.username, u.email, u.phone, u.is_active, "
                   + "       d.department_name "
                   + "FROM employees e "
                   + "JOIN users u       ON e.user_id       = u.user_id "
                   + "JOIN roles ro      ON u.role_id       = ro.role_id "
                   + "JOIN departments d ON e.department_id = d.department_id "
                   + "WHERE ro.role_name = ? ";
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (hasKeyword) {
            sql += "AND (LOWER(u.full_name) LIKE ? "
                + "OR LOWER(e.employee_code) LIKE ? "
                + "OR LOWER(u.email) LIKE ? "
                + "OR LOWER(d.department_name) LIKE ?) ";
        }
        sql += "ORDER BY u.full_name LIMIT ? OFFSET ?";

        List<Employee> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            int idx = 1;
            ps.setString(idx++, roleName);
            if (hasKeyword) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                ps.setString(idx++, like);
                ps.setString(idx++, like);
                ps.setString(idx++, like);
                ps.setString(idx++, like);
            }
            ps.setInt(idx++, limit);
            ps.setInt(idx, offset);
            rs = ps.executeQuery();
            while (rs.next()) {
                Employee e = mapRow(rs);
                e.setUsername(rs.getString("username"));
                e.setEmail(rs.getString("email"));
                e.setPhone(rs.getString("phone"));
                list.add(e);
            }
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public Employee findDetailById(int employeeId) throws SQLException {
        String sql = "SELECT e.*, u.full_name, u.username, u.email, u.phone, u.gender, "
                   + "       u.date_of_birth, u.address, d.department_name, ro.role_name "
                   + "FROM employees e "
                   + "JOIN users u       ON e.user_id       = u.user_id "
                   + "JOIN roles ro      ON u.role_id       = ro.role_id "
                   + "JOIN departments d ON e.department_id = d.department_id "
                   + "WHERE e.employee_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, employeeId);
            rs = ps.executeQuery();
            if (rs.next()) {
                Employee e = mapRow(rs);
                e.setUsername(rs.getString("username"));
                e.setEmail(rs.getString("email"));
                e.setPhone(rs.getString("phone"));
                e.setGender(rs.getString("gender"));
                Date dob = rs.getDate("date_of_birth");
                if (dob != null) e.setDateOfBirth(dob.toLocalDate());
                e.setAddress(rs.getString("address"));
                return e;
            }
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    public boolean updateEmploymentStatus(int employeeId, EmploymentStatus status, int actorUserId)
            throws SQLException {
        String sql = "UPDATE employees SET employment_status=?, updated_by=?, updated_at=NOW() "
                   + "WHERE employee_id=?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, status.name());
            ps.setInt(2, actorUserId);
            ps.setInt(3, employeeId);
            return ps.executeUpdate() > 0;
        } finally {
            close(conn, ps, null);
        }
    }

    public List<Employee> findAllActive() throws SQLException {
        String sql = "SELECT e.*, u.full_name, d.department_name "
                   + "FROM employees e "
                   + "JOIN users u       ON e.user_id       = u.user_id "
                   + "JOIN departments d ON e.department_id = d.department_id "
                   + "WHERE u.is_active = 1 "
                   + "ORDER BY u.full_name";
        List<Employee> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public List<Employee> findAttendanceActive() throws SQLException {
        String sql = "SELECT e.*, u.full_name, d.department_name "
                   + "FROM employees e "
                   + "JOIN users u       ON e.user_id       = u.user_id "
                   + "JOIN roles ro      ON u.role_id       = ro.role_id "
                   + "JOIN departments d ON e.department_id = d.department_id "
                   + "WHERE u.is_active = 1 "
                   + "AND ro.role_name IN ('EMPLOYEE', 'MANAGER', 'HR_STAFF', 'HR_MANAGER') "
                   + "AND d.department_code <> 'IT' "
                   + "ORDER BY u.full_name";
        List<Employee> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public List<Employee> findAttendanceEligible(LocalDate periodStart,
                                                  LocalDate periodEnd) throws SQLException {
        String sql = "SELECT e.*, u.full_name, d.department_name, "
                   + "       GREATEST(e.hire_date, c.start_date, ?) AS attendance_start_date, "
                   + "       LEAST(CASE WHEN c.status = 'Active' THEN ? "
                   + "                  ELSE COALESCE(c.end_date, ?) END, ?) AS attendance_end_date "
                   + "FROM employees e "
                   + "JOIN users u       ON e.user_id       = u.user_id "
                   + "JOIN roles ro      ON u.role_id       = ro.role_id "
                   + "JOIN departments d ON e.department_id = d.department_id "
                   + "JOIN contracts c ON c.contract_id = ("
                   + "    SELECT c2.contract_id FROM contracts c2 "
                   + "    WHERE c2.employee_id = e.employee_id "
                   + "      AND ((c2.status = 'Active' AND c2.start_date <= ?) "
                   + "        OR (c2.status = 'Expired' AND c2.start_date <= ? "
                   + "            AND (c2.end_date IS NULL OR c2.end_date >= ?))) "
                   + "    ORDER BY (c2.status = 'Active') DESC, c2.start_date DESC, "
                   + "             c2.contract_id DESC LIMIT 1"
                   + ") "
                   + "WHERE u.is_active = 1 "
                   + "  AND e.employment_status IN ('Working', 'Probation') "
                   + "  AND e.hire_date <= ? "
                   + "  AND ro.role_name IN ('EMPLOYEE', 'MANAGER', 'HR_STAFF', 'HR_MANAGER') "
                   + "  AND d.department_code <> 'IT' "
                   + "ORDER BY u.full_name";

        List<Employee> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setDate(1, Date.valueOf(periodStart));
            ps.setDate(2, Date.valueOf(periodEnd));
            ps.setDate(3, Date.valueOf(periodEnd));
            ps.setDate(4, Date.valueOf(periodEnd));
            ps.setDate(5, Date.valueOf(periodEnd));
            ps.setDate(6, Date.valueOf(periodEnd));
            ps.setDate(7, Date.valueOf(periodStart));
            ps.setDate(8, Date.valueOf(periodEnd));
            rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public List<Employee> findAttendanceActiveByDepartment(Integer departmentId) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT e.*, u.full_name, d.department_name "
              + "FROM employees e "
              + "JOIN users u       ON e.user_id       = u.user_id "
              + "JOIN roles ro      ON u.role_id       = ro.role_id "
              + "JOIN departments d ON e.department_id = d.department_id "
              + "WHERE u.is_active = 1 "
              + "AND ro.role_name IN ('EMPLOYEE', 'MANAGER', 'HR_STAFF', 'HR_MANAGER') "
              + "AND d.department_code <> 'IT' ");
        if (departmentId != null) sql.append("AND e.department_id = ? ");
        sql.append("ORDER BY u.full_name");

        List<Employee> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            if (departmentId != null) ps.setInt(1, departmentId);
            rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    private Employee mapRow(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.setEmployeeId(rs.getInt("employee_id"));
        e.setUserId(rs.getInt("user_id"));
        e.setEmployeeCode(rs.getString("employee_code"));
        e.setDepartmentId(rs.getInt("department_id"));
        Date hire = rs.getDate("hire_date");
        if (hire != null) e.setHireDate(hire.toLocalDate());
        try {
            Date attendanceStart = rs.getDate("attendance_start_date");
            if (attendanceStart != null) e.setAttendanceStartDate(attendanceStart.toLocalDate());
            Date attendanceEnd = rs.getDate("attendance_end_date");
            if (attendanceEnd != null) e.setAttendanceEndDate(attendanceEnd.toLocalDate());
        } catch (SQLException ignored) {
            // Attendance boundaries are present only in period-scoped roster queries.
        }
        String status = rs.getString("employment_status");
        if (status != null) {
            try { e.setEmploymentStatus(EmploymentStatus.valueOf(status)); }
            catch (IllegalArgumentException ignored) {}
        }
        e.setDependentCount(rs.getInt("dependent_count"));
        e.setBankName(rs.getString("bank_name"));
        e.setBankAccountNumber(rs.getString("bank_account_number"));
        e.setBankBranch(rs.getString("bank_branch"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) e.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) e.setUpdatedAt(updated.toLocalDateTime());
        e.setFullName(rs.getString("full_name"));
        e.setDepartmentName(rs.getString("department_name"));
        return e;
    }

    private void close(Connection conn, PreparedStatement ps, ResultSet rs) {
        if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
        if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
        DBContext.closeConnection(conn);
    }
}
