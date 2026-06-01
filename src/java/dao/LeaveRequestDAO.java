package dao;

import config.DBContext;
import model.LeaveRequest;
import model.LeaveRequest.LeaveType;
import model.LeaveRequest.Status;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LeaveRequestDAO {

    public int insert(LeaveRequest lr) throws SQLException {
        String sql = "INSERT INTO leave_requests (employee_id, leave_type, start_date, end_date, "
                   + "total_days, reason, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, lr.getEmployeeId());
            ps.setString(2, lr.getLeaveType().getDbValue());
            ps.setDate(3, Date.valueOf(lr.getStartDate()));
            ps.setDate(4, Date.valueOf(lr.getEndDate()));
            ps.setBigDecimal(5, lr.getTotalDays());
            ps.setString(6, lr.getReason());
            ps.setString(7, lr.getStatus() == null ? Status.Pending.name() : lr.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } finally {
            close(conn, ps, null);
        }
        return -1;
    }

    public List<LeaveRequest> findByEmployeeId(int employeeId) throws SQLException {
        String sql = "SELECT lr.*, u.full_name AS emp_full_name, e.employee_code "
                   + "FROM leave_requests lr "
                   + "JOIN employees e ON lr.employee_id = e.employee_id "
                   + "JOIN users u     ON e.user_id      = u.user_id "
                   + "WHERE lr.employee_id = ? "
                   + "ORDER BY lr.created_at DESC";
        List<LeaveRequest> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, employeeId);
            rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public boolean hasOverlapping(int employeeId, LocalDate start, LocalDate end) throws SQLException {
        String sql = "SELECT COUNT(*) FROM leave_requests "
                   + "WHERE employee_id = ? "
                   + "AND status IN ('Pending', 'Approved') "
                   + "AND NOT (end_date < ? OR start_date > ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, employeeId);
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(end));
            rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } finally {
            close(conn, ps, rs);
        }
        return false;
    }

    private LeaveRequest mapRow(ResultSet rs) throws SQLException {
        LeaveRequest lr = new LeaveRequest();
        lr.setLeaveRequestId(rs.getInt("leave_request_id"));
        lr.setEmployeeId(rs.getInt("employee_id"));
        lr.setLeaveType(LeaveType.fromDb(rs.getString("leave_type")));
        Date start = rs.getDate("start_date");
        if (start != null) lr.setStartDate(start.toLocalDate());
        Date end = rs.getDate("end_date");
        if (end != null) lr.setEndDate(end.toLocalDate());
        lr.setTotalDays(rs.getBigDecimal("total_days"));
        lr.setReason(rs.getString("reason"));
        String status = rs.getString("status");
        if (status != null) {
            try { lr.setStatus(Status.valueOf(status)); } catch (IllegalArgumentException ignored) {}
        }
        int approvedBy = rs.getInt("approved_by");
        if (!rs.wasNull()) lr.setApprovedBy(approvedBy);
        Timestamp approvedAt = rs.getTimestamp("approved_at");
        if (approvedAt != null) lr.setApprovedAt(approvedAt.toLocalDateTime());
        lr.setManagerNote(rs.getString("manager_note"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) lr.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) lr.setUpdatedAt(updated.toLocalDateTime());
        try { lr.setEmployeeFullName(rs.getString("emp_full_name")); } catch (SQLException ignored) {}
        try { lr.setEmployeeCode(rs.getString("employee_code")); } catch (SQLException ignored) {}
        return lr;
    }

    private void close(Connection conn, PreparedStatement ps, ResultSet rs) {
        if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
        if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
        DBContext.closeConnection(conn);
    }
}
