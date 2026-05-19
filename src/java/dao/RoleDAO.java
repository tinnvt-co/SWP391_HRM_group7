package dao;

import config.DBContext;
import model.Role;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoleDAO {

    public List<Role> findAll() throws SQLException {
        String sql = "SELECT * FROM roles ORDER BY role_id";
        List<Role> list = new ArrayList<>();
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

    public List<Role> findAllActive() throws SQLException {
        String sql = "SELECT * FROM roles WHERE is_active = 1 ORDER BY role_id";
        List<Role> list = new ArrayList<>();
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

    public Role findById(int roleId) throws SQLException {
        String sql = "SELECT * FROM roles WHERE role_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, roleId);
            rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    public boolean update(Role role) throws SQLException {
        String sql = "UPDATE roles SET role_name = ?, description = ?, updated_at = NOW() WHERE role_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, role.getRoleName());
            ps.setString(2, role.getDescription());
            ps.setInt(3, role.getRoleId());
            return ps.executeUpdate() > 0;
        } finally {
            close(conn, ps, null);
        }
    }

    private Role mapRow(ResultSet rs) throws SQLException {
        Role r = new Role();
        r.setRoleId(rs.getInt("role_id"));
        r.setRoleName(rs.getString("role_name"));
        r.setDescription(rs.getString("description"));
        r.setActive(rs.getBoolean("is_active"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) r.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) r.setUpdatedAt(updated.toLocalDateTime());
        return r;
    }

    private void close(Connection conn, PreparedStatement ps, ResultSet rs) {
        if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
        if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
        DBContext.closeConnection(conn);
    }
}
