package dao;

import config.DBContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PermissionDAO {

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
}
