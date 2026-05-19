package dao;

import config.DBContext;
import model.Role;
import model.User;
import model.User.Gender;

import java.sql.*;

public class UserDAO {

    public User findByUsernameAndPassword(String username, String password) throws SQLException {
        String sql = "SELECT u.*, r.role_name FROM users u "
                   + "JOIN roles r ON u.role_id = r.role_id "
                   + "WHERE u.username = ? AND u.password_hash = ? AND u.is_active = 1";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, password);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            DBContext.closeConnection(conn);
        }
        return null;
    }

    public boolean updateLastLogin(int userId) throws SQLException {
        String sql = "UPDATE users SET last_login = NOW() WHERE user_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } finally {
            if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
            DBContext.closeConnection(conn);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        String g = rs.getString("gender");
        if (g != null) u.setGender(Gender.valueOf(g));
        Date dob = rs.getDate("date_of_birth");
        if (dob != null) u.setDateOfBirth(dob.toLocalDate());
        u.setAddress(rs.getString("address"));
        u.setRoleId(rs.getInt("role_id"));
        u.setActive(rs.getBoolean("is_active"));
        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null) u.setLastLogin(lastLogin.toLocalDateTime());
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) u.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) u.setUpdatedAt(updated.toLocalDateTime());

        Role role = new Role();
        role.setRoleId(rs.getInt("role_id"));
        role.setRoleName(rs.getString("role_name"));
        u.setRole(role);

        return u;
    }
}
