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
          + "       cu.username AS created_username, "
          + "       rr.role_name AS requested_role_name, "
          + "       ws.schedule_name AS work_schedule_name "
          + "FROM employee_account_requests ear "
          + "JOIN departments d ON ear.department_id = d.department_id "
          + "JOIN users req ON ear.requested_by = req.user_id "
          + "LEFT JOIN users rev ON ear.reviewed_by = rev.user_id "
          + "LEFT JOIN users cu ON ear.created_user_id = cu.user_id "
          + "LEFT JOIN roles rr ON ear.requested_role_id = rr.role_id "
          + "LEFT JOIN work_schedules ws ON ear.work_schedule_id = ws.work_schedule_id ";

    public int insert(EmployeeAccountRequest r) throws SQLException {
        String sql = "INSERT INTO employee_account_requests "
                   + "(full_name, email, phone, gender, date_of_birth, address, department_id, "
                   + " requested_role_id, position_title, hire_date, employee_code, "
                   + " contract_code, contract_type, contract_start_date, contract_end_date, "
                   + " basic_salary, work_schedule_id, contract_note, "
                   + " contract_document_original_name, contract_document_stored_name, "
                   + " contract_document_path, contract_document_mime_type, contract_document_size, "
                   + " requested_by) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
            if (r.getRequestedRoleId() > 0) ps.setInt(8, r.getRequestedRoleId());
            else                            ps.setNull(8, Types.INTEGER);
            ps.setString(9, r.getPositionTitle());
            ps.setDate(10, Date.valueOf(r.getHireDate()));
            ps.setString(11, r.getEmployeeCode());
            ps.setString(12, r.getContractCode());
            ps.setString(13, r.getContractType() == null ? null : r.getContractType().getDbValue());
            if (r.getContractStartDate() != null) ps.setDate(14, Date.valueOf(r.getContractStartDate()));
            else                                  ps.setNull(14, Types.DATE);
            if (r.getContractEndDate() != null) ps.setDate(15, Date.valueOf(r.getContractEndDate()));
            else                                ps.setNull(15, Types.DATE);
            ps.setBigDecimal(16, r.getBasicSalary());
            ps.setInt(17, r.getWorkScheduleId());
            ps.setString(18, r.getContractNote());
            ps.setString(19, r.getContractDocumentOriginalName());
            ps.setString(20, r.getContractDocumentStoredName());
            ps.setString(21, r.getContractDocumentPath());
            ps.setString(22, r.getContractDocumentMimeType());
            if (r.getContractDocumentSize() != null) ps.setLong(23, r.getContractDocumentSize());
            else                                     ps.setNull(23, Types.BIGINT);
            ps.setInt(24, r.getRequestedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } finally {
            close(conn, ps, null);
        }
        return -1;
    }

    public boolean updateContractCode(int requestId, String contractCode) throws SQLException {
        String sql = "UPDATE employee_account_requests "
                   + "SET contract_code=?, updated_at=NOW() "
                   + "WHERE request_id=? AND status='Pending'";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, contractCode);
            ps.setInt(2, requestId);
            return ps.executeUpdate() > 0;
        } finally {
            close(conn, ps, null);
        }
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

    public boolean hasPendingByContractCode(String contractCode) throws SQLException {
        String sql = "SELECT 1 FROM employee_account_requests WHERE contract_code=? AND status='Pending'";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, contractCode);
            rs = ps.executeQuery();
            return rs.next();
        } finally {
            close(conn, ps, rs);
        }
    }

    public boolean markCreated(int requestId, int reviewedBy, int createdUserId,
                               Integer createdEmployeeId, Integer createdContractId,
                               String note) throws SQLException {
        String sql = "UPDATE employee_account_requests "
                   + "SET status='Created', reviewed_by=?, reviewed_at=NOW(), "
                   + "created_user_id=?, created_employee_id=?, created_contract_id=?, "
                   + "admin_note=?, updated_at=NOW() "
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
            if (createdContractId != null) ps.setInt(4, createdContractId);
            else                           ps.setNull(4, Types.INTEGER);
            ps.setString(5, note);
            ps.setInt(6, requestId);
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
        int requestedRoleId = getIntOrNull(rs, "requested_role_id");
        if (requestedRoleId > 0) r.setRequestedRoleId(requestedRoleId);
        r.setPositionTitle(getStringOrNull(rs, "position_title"));
        Date hire = rs.getDate("hire_date");
        if (hire != null) r.setHireDate(hire.toLocalDate());
        r.setEmployeeCode(rs.getString("employee_code"));
        r.setContractCode(getStringOrNull(rs, "contract_code"));
        r.setContractType(model.Contract.ContractType.fromDb(getStringOrNull(rs, "contract_type")));
        Date contractStart = getDateOrNull(rs, "contract_start_date");
        if (contractStart != null) r.setContractStartDate(contractStart.toLocalDate());
        Date contractEnd = getDateOrNull(rs, "contract_end_date");
        if (contractEnd != null) r.setContractEndDate(contractEnd.toLocalDate());
        r.setBasicSalary(getBigDecimalOrNull(rs, "basic_salary"));
        r.setStandardWorkingDays(getBigDecimalOrNull(rs, "standard_working_days"));
        r.setWorkScheduleId(getIntOrNull(rs, "work_schedule_id"));
        r.setWorkScheduleName(getStringOrNull(rs, "work_schedule_name"));
        r.setContractNote(getStringOrNull(rs, "contract_note"));
        r.setContractDocumentOriginalName(getStringOrNull(rs, "contract_document_original_name"));
        r.setContractDocumentStoredName(getStringOrNull(rs, "contract_document_stored_name"));
        r.setContractDocumentPath(getStringOrNull(rs, "contract_document_path"));
        r.setContractDocumentMimeType(getStringOrNull(rs, "contract_document_mime_type"));
        long docSize = getLongOrNull(rs, "contract_document_size");
        if (docSize >= 0) r.setContractDocumentSize(docSize);
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
        int createdContractId = getIntOrNull(rs, "created_contract_id");
        if (createdContractId > 0) r.setCreatedContractId(createdContractId);
        r.setAdminNote(rs.getString("admin_note"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) r.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) r.setUpdatedAt(updatedAt.toLocalDateTime());
        r.setDepartmentName(rs.getString("department_name"));
        r.setRequestedRoleName(getStringOrNull(rs, "requested_role_name"));
        r.setRequestedByName(rs.getString("requested_by_name"));
        r.setReviewedByName(rs.getString("reviewed_by_name"));
        r.setCreatedUsername(rs.getString("created_username"));
        return r;
    }

    private int getIntOrNull(ResultSet rs, String column) throws SQLException {
        try {
            int v = rs.getInt(column);
            return rs.wasNull() ? 0 : v;
        } catch (SQLException ignored) {
            return 0;
        }
    }

    private String getStringOrNull(ResultSet rs, String column) throws SQLException {
        try {
            return rs.getString(column);
        } catch (SQLException ignored) {
            return null;
        }
    }

    private Date getDateOrNull(ResultSet rs, String column) throws SQLException {
        try {
            return rs.getDate(column);
        } catch (SQLException ignored) {
            return null;
        }
    }

    private java.math.BigDecimal getBigDecimalOrNull(ResultSet rs, String column) throws SQLException {
        try {
            return rs.getBigDecimal(column);
        } catch (SQLException ignored) {
            return null;
        }
    }

    private long getLongOrNull(ResultSet rs, String column) throws SQLException {
        try {
            long v = rs.getLong(column);
            return rs.wasNull() ? -1L : v;
        } catch (SQLException ignored) {
            return -1L;
        }
    }

    private void close(Connection conn, PreparedStatement ps, ResultSet rs) {
        if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
        if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
        DBContext.closeConnection(conn);
    }
}
