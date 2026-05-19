package dao;

import config.DBContext;
import model.Permission;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PermissionDAO {

    public List<Permission> findAll() throws SQLException {
        String sql = "SELECT * FROM permissions ORDER BY permission_id";
        List<Permission> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            DBContext.closeConnection(conn);
        }
        return list;
    }

    public List<Permission> findByRoleId(int roleId) throws SQLException {
        String sql = "SELECT p.* FROM permissions p "
                   + "JOIN role_permissions rp ON p.permission_id = rp.permission_id "
                   + "WHERE rp.role_id = ? ORDER BY p.permission_id";
        List<Permission> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, roleId);
            rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            DBContext.closeConnection(conn);
        }
        return list;
    }

    public List<String> findCodesByUserId(int userId) throws SQLException {
        String sql = "SELECT p.permission_code FROM permissions p "
                   + "JOIN role_permissions rp ON p.permission_id = rp.permission_id "
                   + "JOIN users u ON rp.role_id = u.role_id "
                   + "WHERE u.user_id = ?";
        List<String> codes = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            while (rs.next()) {
                codes.add(rs.getString("permission_code"));
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            DBContext.closeConnection(conn);
        }
        return codes;
    }

    private Permission mapRow(ResultSet rs) throws SQLException {
        Permission p = new Permission();
        p.setPermissionId(rs.getInt("permission_id"));
        p.setPermissionCode(rs.getString("permission_code"));
        p.setPermissionName(rs.getString("permission_name"));
        p.setDescription(rs.getString("description"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) p.setCreatedAt(created.toLocalDateTime());
        return p;
    }
}
