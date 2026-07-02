package dao;

import config.DBContext;
import model.Department;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DepartmentDAO {

    private static final String BASE_SELECT =
            "SELECT d.department_id, d.department_code, d.department_name, d.description, "
          + "       d.manager_id, d.is_active, d.created_at, d.updated_at, "
          + "       u.full_name AS manager_name, "
          + "       (SELECT COUNT(*) FROM employees e WHERE e.department_id = d.department_id) AS employee_count "
          + "FROM departments d "
          + "LEFT JOIN users u ON d.manager_id = u.user_id ";

    public Integer findIdByCode(String code) throws SQLException {
        Department d = findByCode(code);
        return d == null ? null : d.getDepartmentId();
    }

    public Department findByCode(String code) throws SQLException {
        String sql = BASE_SELECT + "WHERE d.department_code = ?";
        return querySingle(sql, code);
    }

    public Department findById(int departmentId) throws SQLException {
        String sql = BASE_SELECT + "WHERE d.department_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, departmentId);
            rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    public List<Department> findAll() throws SQLException {
        return findList(BASE_SELECT + "ORDER BY d.department_id");
    }

    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM departments";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }

    public int countActiveByManagerId(int managerUserId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM departments WHERE manager_id = ? AND is_active = 1";
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

    public List<Department> findEmployeeAssignable() throws SQLException {
        return findList(BASE_SELECT
                + "WHERE d.is_active = 1 AND d.department_code NOT IN ('ADMIN_DEPT', 'HR') "
                + "ORDER BY d.department_name");
    }

    public List<Department> findAttendanceDepartments() throws SQLException {
        return findList(BASE_SELECT
                + "WHERE d.is_active = 1 "
                + "AND d.department_code NOT IN ('ADMIN_DEPT', 'HR', 'IT') "
                + "ORDER BY d.department_name");
    }

    public List<Department> findAllActive() throws SQLException {
        return findList(BASE_SELECT + "WHERE d.is_active = 1 ORDER BY d.department_name");
    }

    public boolean existsByCode(String code) throws SQLException {
        String sql = "SELECT 1 FROM departments WHERE department_code = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, code);
            rs = ps.executeQuery();
            return rs.next();
        } finally {
            close(conn, ps, rs);
        }
    }

    public int insert(Department d) throws SQLException {
        String sql = "INSERT INTO departments (department_code, department_name, description, "
                   + "manager_id, is_active) VALUES (?, ?, ?, ?, 1)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, d.getDepartmentCode());
            ps.setString(2, d.getDepartmentName());
            ps.setString(3, d.getDescription());
            if (d.getManagerId() != null) ps.setInt(4, d.getManagerId());
            else                          ps.setNull(4, Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } finally {
            close(conn, ps, null);
        }
        return -1;
    }

    public boolean update(Department d) throws SQLException {
        String sql = "UPDATE departments SET department_name=?, description=?, manager_id=?, "
                   + "updated_at=NOW() WHERE department_id=?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, d.getDepartmentName());
            ps.setString(2, d.getDescription());
            if (d.getManagerId() != null) ps.setInt(3, d.getManagerId());
            else                          ps.setNull(3, Types.INTEGER);
            ps.setInt(4, d.getDepartmentId());
            return ps.executeUpdate() > 0;
        } finally {
            close(conn, ps, null);
        }
    }

    public boolean setActiveStatus(int departmentId, boolean isActive) throws SQLException {
        String sql = "UPDATE departments SET is_active=?, updated_at=NOW() WHERE department_id=?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setBoolean(1, isActive);
            ps.setInt(2, departmentId);
            return ps.executeUpdate() > 0;
        } finally {
            close(conn, ps, null);
        }
    }

    private Department querySingle(String sql, String param) throws SQLException {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, param);
            rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    private List<Department> findList(String sql) throws SQLException {
        List<Department> list = new ArrayList<>();
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

    private Department mapRow(ResultSet rs) throws SQLException {
        Department d = new Department();
        d.setDepartmentId(rs.getInt("department_id"));
        d.setDepartmentCode(rs.getString("department_code"));
        d.setDepartmentName(rs.getString("department_name"));
        d.setDescription(rs.getString("description"));
        int managerId = rs.getInt("manager_id");
        if (!rs.wasNull()) d.setManagerId(managerId);
        d.setActive(rs.getBoolean("is_active"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) d.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) d.setUpdatedAt(updated.toLocalDateTime());
        try { d.setManagerName(rs.getString("manager_name")); } catch (SQLException ignored) {}
        try { d.setEmployeeCount(rs.getInt("employee_count")); } catch (SQLException ignored) {}
        return d;
    }

    private void close(Connection conn, PreparedStatement ps, ResultSet rs) {
        if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
        if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
        DBContext.closeConnection(conn);
    }
}
