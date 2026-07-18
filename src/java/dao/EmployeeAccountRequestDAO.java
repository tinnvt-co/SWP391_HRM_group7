package dao;

import config.DBContext;
import model.EmployeeAccountRequest;
import model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeAccountRequestDAO {

    private static final String BASE_SELECT =
            "SELECT ear.*, d.department_name, "
          + "       req.full_name AS requested_by_name, "
          + "       rev.full_name AS reviewed_by_name, "
          + "       cu.username AS created_username "
          + "FROM employee_account_requests ear "
          + "JOIN departments d ON ear.department_id = d.department_id "
          + "JOIN users req ON ear.requested_by = req.user_id "
          + "LEFT JOIN users rev ON ear.reviewed_by = rev.user_id "
          + "LEFT JOIN users cu ON ear.created_user_id = cu.user_id ";

    public int insert(EmployeeAccountRequest r) throws SQLException {
        String sql = "INSERT INTO employee_account_requests "
                   + "(full_name, email, phone, gender, date_of_birth, address, department_id, "
                   + " hire_date, employee_code, requested_by) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, r.getFullName());
            ps.setString(2, r.getEmail());
            ps.setString(3, r.getPhone());
            ps.setString(4, r.getGender() == null ? User.Gender.Other.name() : r.getGender().name());
            if (r.getDateOfBirth() != null) ps.setDate(5, Date.valueOf(r.getDateOfBirth()));
            else                            ps.setNull(5, Types.DATE);
            ps.setString(6, r.getAddress());
            ps.setInt(7, r.getDepartmentId());
            ps.setDate(8, Date.valueOf(r.getHireDate()));
            ps.setString(9, r.getEmployeeCode());
            ps.setInt(10, r.getRequestedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } finally {
            close(conn, ps, null);
        }
        return -1;
    }

    public int countForUser(boolean adminScope, int requesterUserId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM employee_account_requests";
        if (!adminScope) sql += " WHERE requested_by=?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            if (!adminScope) ps.setInt(1, requesterUserId);
            rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } finally {
            close(conn, ps, rs);
        }
    }

    public List<EmployeeAccountRequest> findPageForUser(boolean adminScope, int requesterUserId,
                                                        int offset, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        if (!adminScope) sql.append("WHERE ear.requested_by=? ");
        sql.append("ORDER BY CASE ear.status WHEN 'Pending' THEN 0 WHEN 'Rejected' THEN 1 ELSE 2 END, ")
           .append("ear.created_at DESC LIMIT ? OFFSET ?");

        List<EmployeeAccountRequest> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            if (!adminScope) ps.setInt(idx++, requesterUserId);
            ps.setInt(idx++, limit);
            ps.setInt(idx, offset);
            rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public EmployeeAccountRequest findById(int requestId) throws SQLException {
        String sql = BASE_SELECT + "WHERE ear.request_id=?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, requestId);
            rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        } finally {
            close(conn, ps, rs);
        }
    }

    public boolean hasPendingByEmail(String email) throws SQLException {
        String sql = "SELECT 1 FROM employee_account_requests WHERE email=? AND status='Pending'";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            rs = ps.executeQuery();
            return rs.next();
        } finally {
            close(conn, ps, rs);
        }
    }

    public boolean markCreated(int requestId, int reviewedBy, int createdUserId,
                               Integer createdEmployeeId, String note) throws SQLException {
        String sql = "UPDATE employee_account_requests "
                   + "SET status='Created', reviewed_by=?, reviewed_at=NOW(), "
                   + "created_user_id=?, created_employee_id=?, admin_note=?, updated_at=NOW() "
                   + "WHERE request_id=? AND status='Pending'";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, reviewedBy);
            ps.setInt(2, createdUserId);
            if (createdEmployeeId != null) ps.setInt(3, createdEmployeeId);
            else                           ps.setNull(3, Types.INTEGER);
            ps.setString(4, note);
            ps.setInt(5, requestId);
            return ps.executeUpdate() > 0;
        } finally {
            close(conn, ps, null);
        }
    }

    public boolean markRejected(int requestId, int reviewedBy, String note) throws SQLException {
        String sql = "UPDATE employee_account_requests "
                   + "SET status='Rejected', reviewed_by=?, reviewed_at=NOW(), "
                   + "admin_note=?, updated_at=NOW() "
                   + "WHERE request_id=? AND status='Pending'";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, reviewedBy);
            ps.setString(2, note);
            ps.setInt(3, requestId);
            return ps.executeUpdate() > 0;
        } finally {
            close(conn, ps, null);
        }
    }

    private EmployeeAccountRequest mapRow(ResultSet rs) throws SQLException {
        EmployeeAccountRequest r = new EmployeeAccountRequest();
        r.setRequestId(rs.getInt("request_id"));
        r.setFullName(rs.getString("full_name"));
        r.setEmail(rs.getString("email"));
        r.setPhone(rs.getString("phone"));
        String gender = rs.getString("gender");
        if (gender != null) {
            try { r.setGender(User.Gender.valueOf(gender)); } catch (IllegalArgumentException ignored) {}
        }
        Date dob = rs.getDate("date_of_birth");
        if (dob != null) r.setDateOfBirth(dob.toLocalDate());
        r.setAddress(rs.getString("address"));
        r.setDepartmentId(rs.getInt("department_id"));
        Date hire = rs.getDate("hire_date");
        if (hire != null) r.setHireDate(hire.toLocalDate());
        r.setEmployeeCode(rs.getString("employee_code"));
        r.setStatus(EmployeeAccountRequest.Status.fromDb(rs.getString("status")));
        r.setRequestedBy(rs.getInt("requested_by"));
        int reviewedBy = rs.getInt("reviewed_by");
        if (!rs.wasNull()) r.setReviewedBy(reviewedBy);
        Timestamp reviewedAt = rs.getTimestamp("reviewed_at");
        if (reviewedAt != null) r.setReviewedAt(reviewedAt.toLocalDateTime());
        int createdUserId = rs.getInt("created_user_id");
        if (!rs.wasNull()) r.setCreatedUserId(createdUserId);
        int createdEmployeeId = rs.getInt("created_employee_id");
        if (!rs.wasNull()) r.setCreatedEmployeeId(createdEmployeeId);
        r.setAdminNote(rs.getString("admin_note"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) r.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) r.setUpdatedAt(updatedAt.toLocalDateTime());
        r.setDepartmentName(rs.getString("department_name"));
        r.setRequestedByName(rs.getString("requested_by_name"));
        r.setReviewedByName(rs.getString("reviewed_by_name"));
        r.setCreatedUsername(rs.getString("created_username"));
        return r;
    }

    private void close(Connection conn, PreparedStatement ps, ResultSet rs) {
        if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
        if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
        DBContext.closeConnection(conn);
    }
}
