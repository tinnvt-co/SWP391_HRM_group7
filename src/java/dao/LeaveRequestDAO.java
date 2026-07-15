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

    public LeaveRequest findById(int leaveRequestId) throws SQLException {
        String sql = "SELECT lr.*, "
                   + "       u.full_name AS emp_full_name, u.email AS emp_email, "
                   + "       u.phone AS emp_phone, u.user_id AS emp_user_id, "
                   + "       u.manager_id AS emp_manager_user_id, "
                   + "       e.employee_code, "
                   + "       d.department_name, "
                   + "       au.full_name AS approver_full_name "
                   + "FROM leave_requests lr "
                   + "JOIN employees e   ON lr.employee_id   = e.employee_id "
                   + "JOIN users u       ON e.user_id        = u.user_id "
                   + "JOIN departments d ON e.department_id  = d.department_id "
                   + "LEFT JOIN users au ON lr.approved_by   = au.user_id "
                   + "WHERE lr.leave_request_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, leaveRequestId);
            rs = ps.executeQuery();
            if (rs.next()) return mapDetailRow(rs);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    private LeaveRequest mapDetailRow(ResultSet rs) throws SQLException {
        LeaveRequest lr = mapRow(rs);
        try { lr.setEmployeeEmail(rs.getString("emp_email")); } catch (SQLException ignored) {}
        try { lr.setEmployeePhone(rs.getString("emp_phone")); } catch (SQLException ignored) {}
        try { lr.setEmployeeDepartment(rs.getString("department_name")); } catch (SQLException ignored) {}
        try {
            int userId = rs.getInt("emp_user_id");
            if (!rs.wasNull()) lr.setEmployeeUserId(userId);
        } catch (SQLException ignored) {}
        try {
            int mgrId = rs.getInt("emp_manager_user_id");
            if (!rs.wasNull()) lr.setEmployeeManagerUserId(mgrId);
        } catch (SQLException ignored) {}
        try { lr.setApproverFullName(rs.getString("approver_full_name")); } catch (SQLException ignored) {}
        return lr;
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

    public List<LeaveRequest> findByManagerUserId(int managerUserId, Status statusFilter) throws SQLException {
        StringBuilder sql = new StringBuilder()
            .append("SELECT lr.*, u.full_name AS emp_full_name, e.employee_code ")
            .append("FROM leave_requests lr ")
            .append("JOIN employees e ON lr.employee_id = e.employee_id ")
            .append("JOIN users u     ON e.user_id      = u.user_id ")
            .append("WHERE u.manager_id = ? ");
        if (statusFilter != null) sql.append("AND lr.status = ? ");
        sql.append("ORDER BY ")
           .append("CASE lr.status WHEN 'Pending' THEN 0 WHEN 'Approved' THEN 1 ")
           .append("WHEN 'Rejected' THEN 2 ELSE 3 END, lr.created_at DESC");

        List<LeaveRequest> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            ps.setInt(1, managerUserId);
            if (statusFilter != null) ps.setString(2, statusFilter.name());
            rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public List<LeaveRequest> findAll(Status statusFilter) throws SQLException {
        StringBuilder sql = new StringBuilder()
            .append("SELECT lr.*, u.full_name AS emp_full_name, e.employee_code ")
            .append("FROM leave_requests lr ")
            .append("JOIN employees e ON lr.employee_id = e.employee_id ")
            .append("JOIN users u     ON e.user_id      = u.user_id ");
        if (statusFilter != null) sql.append("WHERE lr.status = ? ");
        sql.append("ORDER BY ")
           .append("CASE lr.status WHEN 'Pending' THEN 0 WHEN 'Approved' THEN 1 ")
           .append("WHEN 'Rejected' THEN 2 ELSE 3 END, lr.created_at DESC");

        List<LeaveRequest> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            if (statusFilter != null) ps.setString(1, statusFilter.name());
            rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public List<LeaveRequest> findApprovedOverlappingForAttendance(LocalDate fromDate,
                                                                   LocalDate toDate,
                                                                   Integer departmentId,
                                                                   Integer employeeId)
            throws SQLException {
        StringBuilder sql = new StringBuilder()
            .append("SELECT lr.*, ")
            .append("       u.full_name AS emp_full_name, u.email AS emp_email, ")
            .append("       u.phone AS emp_phone, u.user_id AS emp_user_id, ")
            .append("       u.manager_id AS emp_manager_user_id, ")
            .append("       e.employee_code, ")
            .append("       d.department_name, ")
            .append("       au.full_name AS approver_full_name ")
            .append("FROM leave_requests lr ")
            .append("JOIN employees e   ON lr.employee_id  = e.employee_id ")
            .append("JOIN users u       ON e.user_id       = u.user_id ")
            .append("JOIN departments d ON e.department_id = d.department_id ")
            .append("LEFT JOIN users au ON lr.approved_by  = au.user_id ")
            .append("WHERE lr.status = 'Approved' ")
            .append("  AND lr.start_date <= ? ")
            .append("  AND lr.end_date >= ? ")
            .append("  AND d.department_code NOT IN ('ADMIN_DEPT', 'HR', 'IT') ");
        List<Object> params = new ArrayList<>();
        params.add(Date.valueOf(toDate));
        params.add(Date.valueOf(fromDate));
        if (departmentId != null) {
            sql.append("AND e.department_id = ? ");
            params.add(departmentId);
        }
        if (employeeId != null) {
            sql.append("AND lr.employee_id = ? ");
            params.add(employeeId);
        }
        sql.append("ORDER BY d.department_name, u.full_name, lr.start_date, lr.end_date");

        List<LeaveRequest> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            rs = ps.executeQuery();
            while (rs.next()) list.add(mapDetailRow(rs));
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public boolean approve(int leaveRequestId, int approverUserId, String managerNote) throws SQLException {
        String sql = "UPDATE leave_requests SET status='Approved', approved_by=?, "
                   + "approved_at=NOW(), manager_note=?, updated_at=NOW() "
                   + "WHERE leave_request_id=? AND status='Pending'";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, approverUserId);
            ps.setString(2, managerNote);
            ps.setInt(3, leaveRequestId);
            return ps.executeUpdate() > 0;
        } finally {
            close(conn, ps, null);
        }
    }

    public boolean reject(int leaveRequestId, int approverUserId, String managerNote) throws SQLException {
        String sql = "UPDATE leave_requests SET status='Rejected', approved_by=?, "
                   + "approved_at=NOW(), manager_note=?, updated_at=NOW() "
                   + "WHERE leave_request_id=? AND status='Pending'";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, approverUserId);
            ps.setString(2, managerNote);
            ps.setInt(3, leaveRequestId);
            return ps.executeUpdate() > 0;
        } finally {
            close(conn, ps, null);
        }
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
