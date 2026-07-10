package dao;

import config.DBContext;
import model.AllowanceType;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AllowanceTypeDAO {

    public List<AllowanceType> findAll() throws SQLException {
        String sql = "SELECT * FROM allowance_types "
                   + "ORDER BY is_active DESC, allowance_name ASC";
        List<AllowanceType> list = new ArrayList<>();
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

    public AllowanceType findById(int allowanceTypeId) throws SQLException {
        String sql = "SELECT * FROM allowance_types WHERE allowance_type_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, allowanceTypeId);
            rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        } finally {
            close(conn, ps, rs);
        }
    }

    public boolean existsByCode(String allowanceCode, Integer excludeId) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT 1 FROM allowance_types WHERE allowance_code = ?");
        if (excludeId != null) sql.append(" AND allowance_type_id <> ?");

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            ps.setString(1, allowanceCode);
            if (excludeId != null) ps.setInt(2, excludeId);
            rs = ps.executeQuery();
            return rs.next();
        } finally {
            close(conn, ps, rs);
        }
    }

    public int insert(AllowanceType a) throws SQLException {
        String sql = "INSERT INTO allowance_types "
                   + "(allowance_code, allowance_name, amount, description, "
                   + " is_active, created_by, updated_by) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, a.getAllowanceCode());
            ps.setString(2, a.getAllowanceName());
            ps.setBigDecimal(3, nz(a.getAmount()));
            ps.setString(4, a.getDescription());
            ps.setBoolean(5, a.isActive());
            setNullableInt(ps, 6, a.getCreatedBy());
            setNullableInt(ps, 7, a.getUpdatedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
            return -1;
        } finally {
            close(conn, ps, null);
        }
    }

    public boolean update(AllowanceType a) throws SQLException {
        String sql = "UPDATE allowance_types "
                   + "SET allowance_name=?, amount=?, description=?, updated_by=?, updated_at=NOW() "
                   + "WHERE allowance_type_id=?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, a.getAllowanceName());
            ps.setBigDecimal(2, nz(a.getAmount()));
            ps.setString(3, a.getDescription());
            setNullableInt(ps, 4, a.getUpdatedBy());
            ps.setInt(5, a.getAllowanceTypeId());
            return ps.executeUpdate() > 0;
        } finally {
            close(conn, ps, null);
        }
    }

    public boolean setActiveStatus(int allowanceTypeId, boolean active, int actorUserId)
            throws SQLException {
        String sql = "UPDATE allowance_types "
                   + "SET is_active=?, updated_by=?, updated_at=NOW() "
                   + "WHERE allowance_type_id=?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setBoolean(1, active);
            ps.setInt(2, actorUserId);
            ps.setInt(3, allowanceTypeId);
            return ps.executeUpdate() > 0;
        } finally {
            close(conn, ps, null);
        }
    }

    public BigDecimal sumActiveAllowances() throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM allowance_types WHERE is_active = 1";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        } finally {
            close(conn, ps, rs);
        }
    }

    private AllowanceType mapRow(ResultSet rs) throws SQLException {
        AllowanceType a = new AllowanceType();
        a.setAllowanceTypeId(rs.getInt("allowance_type_id"));
        a.setAllowanceCode(rs.getString("allowance_code"));
        a.setAllowanceName(rs.getString("allowance_name"));
        a.setAmount(rs.getBigDecimal("amount"));
        a.setDescription(rs.getString("description"));
        a.setActive(rs.getBoolean("is_active"));
        int createdBy = rs.getInt("created_by");
        if (!rs.wasNull()) a.setCreatedBy(createdBy);
        int updatedBy = rs.getInt("updated_by");
        if (!rs.wasNull()) a.setUpdatedBy(updatedBy);
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) a.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) a.setUpdatedAt(updated.toLocalDateTime());
        return a;
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private void close(Connection conn, PreparedStatement ps, ResultSet rs) {
        if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
        if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
        DBContext.closeConnection(conn);
    }
}
