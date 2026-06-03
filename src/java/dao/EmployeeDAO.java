package dao;

import config.DBContext;
import model.Employee;
import model.Employee.EmploymentStatus;

import java.sql.*;
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

    private Employee mapRow(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.setEmployeeId(rs.getInt("employee_id"));
        e.setUserId(rs.getInt("user_id"));
        e.setEmployeeCode(rs.getString("employee_code"));
        e.setDepartmentId(rs.getInt("department_id"));
        Date hire = rs.getDate("hire_date");
        if (hire != null) e.setHireDate(hire.toLocalDate());
        String status = rs.getString("employment_status");
        if (status != null) {
            try { e.setEmploymentStatus(EmploymentStatus.valueOf(status)); }
            catch (IllegalArgumentException ignored) {}
        }
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
