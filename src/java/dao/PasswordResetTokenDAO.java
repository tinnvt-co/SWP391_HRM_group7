package dao;

import config.DBContext;
import model.PasswordResetToken;

import java.sql.*;

public class PasswordResetTokenDAO {

    public int insert(PasswordResetToken token) throws SQLException {
        String sql = "INSERT INTO password_reset_tokens (user_id, reset_token, expired_at, is_used) "
                   + "VALUES (?, ?, ?, 0)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, token.getUserId());
            ps.setString(2, token.getResetToken());
            ps.setTimestamp(3, Timestamp.valueOf(token.getExpiredAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } finally {
            close(conn, ps, null);
        }
        return -1;
    }

    public PasswordResetToken findByToken(String token) throws SQLException {
        String sql = "SELECT * FROM password_reset_tokens WHERE reset_token = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, token);
            rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    public boolean markAsUsed(int tokenId) throws SQLException {
        String sql = "UPDATE password_reset_tokens SET is_used = 1 WHERE token_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, tokenId);
            return ps.executeUpdate() > 0;
        } finally {
            close(conn, ps, null);
        }
    }

    public void invalidateAllForUser(int userId) throws SQLException {
        String sql = "UPDATE password_reset_tokens SET is_used = 1 WHERE user_id = ? AND is_used = 0";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.executeUpdate();
        } finally {
            close(conn, ps, null);
        }
    }

    private PasswordResetToken mapRow(ResultSet rs) throws SQLException {
        PasswordResetToken t = new PasswordResetToken();
        t.setTokenId(rs.getInt("token_id"));
        t.setUserId(rs.getInt("user_id"));
        t.setResetToken(rs.getString("reset_token"));
        Timestamp expired = rs.getTimestamp("expired_at");
        if (expired != null) t.setExpiredAt(expired.toLocalDateTime());
        t.setUsed(rs.getBoolean("is_used"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) t.setCreatedAt(created.toLocalDateTime());
        return t;
    }

    private void close(Connection conn, PreparedStatement ps, ResultSet rs) {
        if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
        if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
        DBContext.closeConnection(conn);
    }
}
