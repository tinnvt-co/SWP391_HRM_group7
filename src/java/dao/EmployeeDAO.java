package dao;

import config.DBContext;
import model.Employee;
import model.Employee.EmploymentStatus;

import java.sql.*;

public class EmployeeDAO {

    public Employee findByUserId(int userId) throws SQLException {
        String sql = "SELECT e.*, u.full_name, d.department_name, p.position_name "
                   + "FROM employees e "
                   + "JOIN users u       ON e.user_id       = u.user_id "
                   + "JOIN departments d ON e.department_id = d.department_id "
                   + "JOIN positions p   ON e.position_id   = p.position_id "
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

    private Employee mapRow(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.setEmployeeId(rs.getInt("employee_id"));
        e.setUserId(rs.getInt("user_id"));
        e.setEmployeeCode(rs.getString("employee_code"));
        e.setDepartmentId(rs.getInt("department_id"));
        e.setPositionId(rs.getInt("position_id"));
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
        e.setPositionName(rs.getString("position_name"));
        return e;
    }

    private void close(Connection conn, PreparedStatement ps, ResultSet rs) {
        if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
        if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
        DBContext.closeConnection(conn);
    }
}
