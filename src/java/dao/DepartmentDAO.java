package dao;

import config.DBContext;
import model.Department;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DepartmentDAO {

    public Integer findIdByCode(String code) throws SQLException {
        Department d = findByCode(code);
        return d == null ? null : d.getDepartmentId();
    }

    public Department findByCode(String code) throws SQLException {
        String sql = "SELECT department_id, department_code, department_name, manager_id "
                   + "FROM departments WHERE department_code = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, code);
            rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    public Department findById(int departmentId) throws SQLException {
        String sql = "SELECT department_id, department_code, department_name, manager_id "
                   + "FROM departments WHERE department_id = ?";
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

    public List<Department> findEmployeeAssignable() throws SQLException {
        String sql = "SELECT department_id, department_code, department_name, manager_id "
                   + "FROM departments "
                   + "WHERE is_active = 1 AND department_code NOT IN ('ADMIN_DEPT', 'HR') "
                   + "ORDER BY department_name";
        return findList(sql);
    }

    public List<Department> findAllActive() throws SQLException {
        String sql = "SELECT department_id, department_code, department_name, manager_id "
                   + "FROM departments WHERE is_active = 1 ORDER BY department_name";
        return findList(sql);
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
        int managerId = rs.getInt("manager_id");
        if (!rs.wasNull()) d.setManagerId(managerId);
        return d;
    }

    private void close(Connection conn, PreparedStatement ps, ResultSet rs) {
        if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
        if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
        DBContext.closeConnection(conn);
    }
}
