package dao;

import config.DBContext;
import model.AttendanceReport;
import model.AttendanceReport.Status;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttendanceReportDAO {

    /**
     * Insert a report, or update the summary if one already exists for the same
     * (employee, month, year). Sets status to "Submitted To HR Staff" and stamps
     * submitted_at. Returns true on success.
     */
    public boolean upsertSubmitted(AttendanceReport r) throws SQLException {
        String sql =
            "INSERT INTO attendance_reports "
          + "(employee_id, manager_id, department_id, report_month, report_year, "
          + " standard_working_days, actual_working_days, paid_leave_days, "
          + " unpaid_leave_days, overtime_hours, status, submitted_at) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Submitted To HR Staff', NOW()) "
          + "ON DUPLICATE KEY UPDATE "
          + " manager_id=VALUES(manager_id), department_id=VALUES(department_id), "
          + " standard_working_days=VALUES(standard_working_days), "
          + " actual_working_days=VALUES(actual_working_days), "
          + " paid_leave_days=VALUES(paid_leave_days), "
          + " unpaid_leave_days=VALUES(unpaid_leave_days), "
          + " overtime_hours=VALUES(overtime_hours), "
          + " status='Submitted To HR Staff', submitted_at=NOW(), updated_at=NOW()";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, r.getEmployeeId());
            ps.setInt(2, r.getManagerId());
            ps.setInt(3, r.getDepartmentId());
            ps.setInt(4, r.getReportMonth());
            ps.setInt(5, r.getReportYear());
            ps.setBigDecimal(6, nz(r.getStandardWorkingDays(), new BigDecimal("26")));
            ps.setBigDecimal(7, nz(r.getActualWorkingDays(), BigDecimal.ZERO));
            ps.setBigDecimal(8, nz(r.getPaidLeaveDays(), BigDecimal.ZERO));
            ps.setBigDecimal(9, nz(r.getUnpaidLeaveDays(), BigDecimal.ZERO));
            ps.setBigDecimal(10, nz(r.getOvertimeHours(), BigDecimal.ZERO));
            return ps.executeUpdate() > 0;
        } finally {
            close(conn, ps, null);
        }
    }

    /**
     * Reports submitted to HR for a given month, across all departments.
     * Only rows already sent to HR (not Draft) are returned.
     */
    public List<AttendanceReport> findSubmittedByMonth(int year, int month,
                                                       Integer departmentId) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT ar.*, "
          + "  eu.full_name AS emp_full_name, e.employee_code, "
          + "  d.department_name, mu.full_name AS manager_full_name "
          + "FROM attendance_reports ar "
          + "JOIN employees e   ON ar.employee_id  = e.employee_id "
          + "JOIN users eu      ON e.user_id       = eu.user_id "
          + "JOIN departments d ON ar.department_id = d.department_id "
          + "JOIN users mu      ON ar.manager_id    = mu.user_id "
          + "WHERE ar.report_year=? AND ar.report_month=? "
          + "  AND ar.status <> 'Draft' ");
        if (departmentId != null) sql.append("AND ar.department_id=? ");
        sql.append("ORDER BY d.department_name, eu.full_name");

        List<AttendanceReport> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            ps.setInt(1, year);
            ps.setInt(2, month);
            if (departmentId != null) ps.setInt(3, departmentId);
            rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    private AttendanceReport mapRow(ResultSet rs) throws SQLException {
        AttendanceReport r = new AttendanceReport();
        r.setAttendanceReportId(rs.getInt("attendance_report_id"));
        r.setEmployeeId(rs.getInt("employee_id"));
        r.setManagerId(rs.getInt("manager_id"));
        r.setDepartmentId(rs.getInt("department_id"));
        r.setReportMonth(rs.getInt("report_month"));
        r.setReportYear(rs.getInt("report_year"));
        r.setStandardWorkingDays(rs.getBigDecimal("standard_working_days"));
        r.setActualWorkingDays(rs.getBigDecimal("actual_working_days"));
        r.setPaidLeaveDays(rs.getBigDecimal("paid_leave_days"));
        r.setUnpaidLeaveDays(rs.getBigDecimal("unpaid_leave_days"));
        r.setOvertimeHours(rs.getBigDecimal("overtime_hours"));
        r.setKpiBonus(rs.getBigDecimal("kpi_bonus"));
        r.setAdvancePayment(rs.getBigDecimal("advance_payment"));
        r.setStatus(Status.fromDb(rs.getString("status")));
        Timestamp sub = rs.getTimestamp("submitted_at");
        if (sub != null) r.setSubmittedAt(sub.toLocalDateTime());
        int reviewedBy = rs.getInt("reviewed_by");
        if (!rs.wasNull()) r.setReviewedBy(reviewedBy);
        Timestamp rev = rs.getTimestamp("reviewed_at");
        if (rev != null) r.setReviewedAt(rev.toLocalDateTime());
        r.setHrNote(rs.getString("hr_note"));
        r.setEmployeeFullName(rs.getString("emp_full_name"));
        r.setEmployeeCode(rs.getString("employee_code"));
        r.setDepartmentName(rs.getString("department_name"));
        r.setManagerFullName(rs.getString("manager_full_name"));
        return r;
    }

    private static BigDecimal nz(BigDecimal v, BigDecimal dflt) {
        return v != null ? v : dflt;
    }

    private void close(Connection c, PreparedStatement ps, ResultSet rs) {
        try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
        try { if (ps != null) ps.close(); } catch (SQLException ignored) {}
        try { if (c  != null) c.close();  } catch (SQLException ignored) {}
    }
}
