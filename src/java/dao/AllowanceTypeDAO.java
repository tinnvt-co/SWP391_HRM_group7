package dao;

import config.DBContext;
import model.AllowanceType;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AllowanceTypeDAO {

    private static final BigDecimal DEFAULT_HR_STAFF_RESPONSIBILITY =
            new BigDecimal("2000000");
    private static final BigDecimal DEFAULT_MANAGER_RESPONSIBILITY =
            new BigDecimal("5000000");

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

    public List<AllowanceType> findActive() throws SQLException {
        String sql = "SELECT * FROM allowance_types "
                   + "WHERE is_active = 1 "
                   + "ORDER BY allowance_name ASC";
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

    public List<AllowanceType> findActiveForRole(String roleName) throws SQLException {
        ensureResponsibilityAllowances(null);
        String roleAllowanceCode = responsibilityCodeForRole(roleName);
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM allowance_types "
              + "WHERE is_active = 1 "
              + "AND (allowance_code NOT IN (?, ?) ");
        if (roleAllowanceCode != null) {
            sql.append("OR allowance_code = ? ");
        }
        sql.append(") ORDER BY allowance_name ASC");

        List<AllowanceType> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            int idx = bindResponsibilityExclusions(ps, 1);
            if (roleAllowanceCode != null) {
                ps.setString(idx, roleAllowanceCode);
            }
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

    public BigDecimal sumCommonActiveAllowances() throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) "
                   + "FROM allowance_types "
                   + "WHERE is_active = 1 "
                   + "AND allowance_code NOT IN (?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            bindResponsibilityExclusions(ps, 1);
            rs = ps.executeQuery();
            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        } finally {
            close(conn, ps, rs);
        }
    }

    public BigDecimal sumPayableAllowancesForRole(String roleName) throws SQLException {
        ensureResponsibilityAllowances(null);
        String roleAllowanceCode = responsibilityCodeForRole(roleName);
        StringBuilder sql = new StringBuilder(
                "SELECT COALESCE(SUM(amount), 0) "
              + "FROM allowance_types "
              + "WHERE is_active = 1 "
              + "AND (allowance_code NOT IN (?, ?) ");
        if (roleAllowanceCode != null) {
            sql.append("OR allowance_code = ? ");
        }
        sql.append(")");

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            int idx = bindResponsibilityExclusions(ps, 1);
            if (roleAllowanceCode != null) {
                ps.setString(idx, roleAllowanceCode);
            }
            rs = ps.executeQuery();
            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        } finally {
            close(conn, ps, rs);
        }
    }

    public void ensureResponsibilityAllowances(Integer actorUserId) throws SQLException {
        insertDefaultIfMissing(
                AllowanceType.RESPONSIBILITY_HR_STAFF_CODE,
                "Responsibility allowance - HR Staff",
                DEFAULT_HR_STAFF_RESPONSIBILITY,
                "Supports HR Staff in carrying out their assigned responsibilities.",
                actorUserId);
        insertDefaultIfMissing(
                AllowanceType.RESPONSIBILITY_MANAGER_CODE,
                "Responsibility allowance - Manager",
                DEFAULT_MANAGER_RESPONSIBILITY,
                "Supports managers in carrying out their management responsibilities.",
                actorUserId);
    }

    private void insertDefaultIfMissing(String code, String name, BigDecimal amount,
                                        String description, Integer actorUserId)
            throws SQLException {
        if (existsByCode(code, null)) return;

        AllowanceType allowance = new AllowanceType();
        Integer seedActorUserId = resolveSeedActorUserId(actorUserId);
        allowance.setAllowanceCode(code);
        allowance.setAllowanceName(name);
        allowance.setAmount(amount);
        allowance.setDescription(description);
        allowance.setActive(true);
        allowance.setCreatedBy(seedActorUserId);
        allowance.setUpdatedBy(seedActorUserId);
        insert(allowance);
    }

    private Integer resolveSeedActorUserId(Integer actorUserId) throws SQLException {
        if (actorUserId != null) return actorUserId;

        String sql = "SELECT u.user_id "
                   + "FROM users u "
                   + "JOIN roles r ON u.role_id = r.role_id "
                   + "WHERE u.is_active = 1 "
                   + "ORDER BY CASE "
                   + "  WHEN r.role_name = 'HR_MANAGER' THEN 1 "
                   + "  WHEN r.role_name = 'ADMIN' THEN 2 "
                   + "  ELSE 3 "
                   + "END, u.user_id "
                   + "LIMIT 1";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            return rs.next() ? rs.getInt("user_id") : null;
        } finally {
            close(conn, ps, rs);
        }
    }

    private int bindResponsibilityExclusions(PreparedStatement ps, int startIndex)
            throws SQLException {
        ps.setString(startIndex++, AllowanceType.RESPONSIBILITY_HR_STAFF_CODE);
        ps.setString(startIndex++, AllowanceType.RESPONSIBILITY_MANAGER_CODE);
        return startIndex;
    }

    private String responsibilityCodeForRole(String roleName) {
        if ("HR_STAFF".equalsIgnoreCase(nullToEmpty(roleName))) {
            return AllowanceType.RESPONSIBILITY_HR_STAFF_CODE;
        }
        if ("MANAGER".equalsIgnoreCase(nullToEmpty(roleName))
                || "HR_MANAGER".equalsIgnoreCase(nullToEmpty(roleName))) {
            return AllowanceType.RESPONSIBILITY_MANAGER_CODE;
        }
        return null;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
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
