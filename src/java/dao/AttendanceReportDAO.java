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
     * (employee, month, year). Manager confirmation sends reports back to HR
     * Staff first; HR Staff submits them to HR Manager in a separate step.
     */
    public boolean upsertSubmitted(AttendanceReport r) throws SQLException {
        String sql =
            "INSERT INTO attendance_reports "
          + "(employee_id, manager_id, department_id, report_month, report_year, "
          + " standard_working_days, actual_working_days, paid_leave_days, "
          + " unpaid_leave_days, maternity_leave_days, overtime_hours, late_penalty_amount, "
          + " status, submitted_at) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Submitted To HR Staff', NOW()) "
          + "ON DUPLICATE KEY UPDATE "
          + " manager_id=VALUES(manager_id), department_id=VALUES(department_id), "
          + " standard_working_days=VALUES(standard_working_days), "
          + " actual_working_days=VALUES(actual_working_days), "
          + " paid_leave_days=VALUES(paid_leave_days), "
          + " unpaid_leave_days=VALUES(unpaid_leave_days), "
          + " maternity_leave_days=VALUES(maternity_leave_days), "
          + " overtime_hours=VALUES(overtime_hours), "
          + " late_penalty_amount=VALUES(late_penalty_amount), "
          + " status='Submitted To HR Staff', submitted_at=NOW(), "
          + " reviewed_by=NULL, reviewed_at=NULL, hr_note=NULL, updated_at=NOW()";
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
            ps.setBigDecimal(10, nz(r.getMaternityLeaveDays(), BigDecimal.ZERO));
            ps.setBigDecimal(11, nz(r.getOvertimeHours(), BigDecimal.ZERO));
            ps.setBigDecimal(12, nz(r.getLatePenaltyAmount(), BigDecimal.ZERO));
            return ps.executeUpdate() > 0;
        } finally {
            close(conn, ps, null);
        }
    }

    public boolean upsertPendingHrManager(AttendanceReport r) throws SQLException {
        String sql =
            "INSERT INTO attendance_reports "
          + "(employee_id, manager_id, department_id, report_month, report_year, "
          + " standard_working_days, actual_working_days, paid_leave_days, "
          + " unpaid_leave_days, maternity_leave_days, overtime_hours, late_penalty_amount, "
          + " status, submitted_at) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Pending HR Manager Approval', NOW()) "
          + "ON DUPLICATE KEY UPDATE "
          + " manager_id=CASE WHEN attendance_reports.status='Approved By HR Manager' THEN manager_id ELSE VALUES(manager_id) END, "
          + " department_id=CASE WHEN attendance_reports.status='Approved By HR Manager' THEN department_id ELSE VALUES(department_id) END, "
          + " standard_working_days=CASE WHEN attendance_reports.status='Approved By HR Manager' THEN standard_working_days ELSE VALUES(standard_working_days) END, "
          + " actual_working_days=CASE WHEN attendance_reports.status='Approved By HR Manager' THEN actual_working_days ELSE VALUES(actual_working_days) END, "
          + " paid_leave_days=CASE WHEN attendance_reports.status='Approved By HR Manager' THEN paid_leave_days ELSE VALUES(paid_leave_days) END, "
          + " unpaid_leave_days=CASE WHEN attendance_reports.status='Approved By HR Manager' THEN unpaid_leave_days ELSE VALUES(unpaid_leave_days) END, "
          + " maternity_leave_days=CASE WHEN attendance_reports.status='Approved By HR Manager' THEN maternity_leave_days ELSE VALUES(maternity_leave_days) END, "
          + " overtime_hours=CASE WHEN attendance_reports.status='Approved By HR Manager' THEN overtime_hours ELSE VALUES(overtime_hours) END, "
          + " late_penalty_amount=CASE WHEN attendance_reports.status='Approved By HR Manager' THEN late_penalty_amount ELSE VALUES(late_penalty_amount) END, "
          + " status=CASE WHEN attendance_reports.status='Approved By HR Manager' THEN status ELSE 'Pending HR Manager Approval' END, "
          + " submitted_at=CASE WHEN attendance_reports.status='Approved By HR Manager' THEN submitted_at ELSE NOW() END, "
          + " reviewed_by=CASE WHEN attendance_reports.status='Approved By HR Manager' THEN reviewed_by ELSE NULL END, "
          + " reviewed_at=CASE WHEN attendance_reports.status='Approved By HR Manager' THEN reviewed_at ELSE NULL END, "
          + " hr_note=CASE WHEN attendance_reports.status='Approved By HR Manager' THEN hr_note ELSE NULL END, "
          + " updated_at=NOW()";
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
            ps.setBigDecimal(10, nz(r.getMaternityLeaveDays(), BigDecimal.ZERO));
            ps.setBigDecimal(11, nz(r.getOvertimeHours(), BigDecimal.ZERO));
            ps.setBigDecimal(12, nz(r.getLatePenaltyAmount(), BigDecimal.ZERO));
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
        return findSubmittedByMonth(year, month, departmentId, null);
    }

    public int countSubmittedByMonth(int year, int month,
                                     Integer departmentId, Integer managerUserId) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) "
          + "FROM attendance_reports ar "
          + "WHERE ar.report_year=? AND ar.report_month=? "
          + "  AND ar.status <> 'Draft' ");
        sql.append(managerConfirmedPredicate("ar"));
        if (departmentId != null) sql.append("AND ar.department_id=? ");
        if (managerUserId != null) sql.append("AND ar.manager_id=? ");

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            ps.setInt(idx++, year);
            ps.setInt(idx++, month);
            if (departmentId != null) ps.setInt(idx++, departmentId);
            if (managerUserId != null) ps.setInt(idx, managerUserId);
            rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } finally {
            close(conn, ps, rs);
        }
    }

    public List<AttendanceReport> findSubmittedByMonth(int year, int month,
                                                       Integer departmentId, Integer managerUserId)
            throws SQLException {
        return findSubmittedByMonthPage(year, month, departmentId, managerUserId, 0, Integer.MAX_VALUE);
    }

    public int countForHrManagerByMonth(int year, int month,
                                        Integer departmentId) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) "
          + "FROM attendance_reports ar "
          + "WHERE ar.report_year=? AND ar.report_month=? "
          + "  AND ar.status IN ("
          + "      'Pending HR Manager Approval', "
          + "      'Approved By HR Manager', "
          + "      'Rejected By HR Manager'"
          + "  ) ");
        sql.append(managerConfirmedPredicate("ar"));
        if (departmentId != null) sql.append("AND ar.department_id=? ");

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            ps.setInt(idx++, year);
            ps.setInt(idx++, month);
            if (departmentId != null) ps.setInt(idx, departmentId);
            rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } finally {
            close(conn, ps, rs);
        }
    }

    public List<AttendanceReport> findForHrManagerByMonthPage(int year, int month,
                                                              Integer departmentId,
                                                              int offset, int limit)
            throws SQLException {
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
          + "  AND ar.status IN ("
          + "      'Pending HR Manager Approval', "
          + "      'Approved By HR Manager', "
          + "      'Rejected By HR Manager'"
          + "  ) ");
        sql.append(managerConfirmedPredicate("ar"));
        if (departmentId != null) sql.append("AND ar.department_id=? ");
        sql.append("ORDER BY d.department_name, eu.full_name LIMIT ? OFFSET ?");

        List<AttendanceReport> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            ps.setInt(idx++, year);
            ps.setInt(idx++, month);
            if (departmentId != null) ps.setInt(idx++, departmentId);
            ps.setInt(idx++, limit);
            ps.setInt(idx, offset);
            rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public int countPendingHrManagerByMonth(int year, int month,
                                            Integer departmentId) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) "
          + "FROM attendance_reports ar "
          + "WHERE report_year=? AND report_month=? "
          + "  AND status='Pending HR Manager Approval' ");
        sql.append(managerConfirmedPredicate("ar"));
        if (departmentId != null) sql.append("AND ar.department_id=? ");

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            ps.setInt(idx++, year);
            ps.setInt(idx++, month);
            if (departmentId != null) ps.setInt(idx, departmentId);
            rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } finally {
            close(conn, ps, rs);
        }
    }

    public int approvePendingHrManagerByMonth(int year, int month,
                                              Integer departmentId,
                                              int reviewedBy) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "UPDATE attendance_reports "
          + "SET status='Approved By HR Manager', reviewed_by=?, reviewed_at=NOW(), "
          + "    hr_note=NULL, updated_at=NOW() "
          + "WHERE report_year=? AND report_month=? "
          + "  AND status='Pending HR Manager Approval' ");
        sql.append(managerConfirmedPredicate("attendance_reports"));
        if (departmentId != null) sql.append("AND department_id=? ");

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            ps.setInt(idx++, reviewedBy);
            ps.setInt(idx++, year);
            ps.setInt(idx++, month);
            if (departmentId != null) ps.setInt(idx, departmentId);
            return ps.executeUpdate();
        } finally {
            close(conn, ps, null);
        }
    }

    public List<AttendanceReport> findApprovedForPayrollByMonth(int year, int month,
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
          + "  AND ar.status = 'Approved By HR Manager' ");
        sql.append(managerConfirmedPredicate("ar"));
        if (departmentId != null) sql.append("AND ar.department_id=? ");
        sql.append("ORDER BY d.department_name, eu.full_name");

        List<AttendanceReport> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            ps.setInt(idx++, year);
            ps.setInt(idx++, month);
            if (departmentId != null) ps.setInt(idx, departmentId);
            rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public int countReadyForHrManagerSubmission(int year, int month,
                                                Integer departmentId) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) "
          + "FROM attendance_reports ar "
          + "WHERE report_year=? AND report_month=? "
          + "  AND status IN ('Submitted To HR Staff', 'Rejected By HR Manager') ");
        sql.append(managerConfirmedPredicate("ar"));
        if (departmentId != null) sql.append("AND ar.department_id=? ");

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            ps.setInt(idx++, year);
            ps.setInt(idx++, month);
            if (departmentId != null) ps.setInt(idx, departmentId);
            rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } finally {
            close(conn, ps, rs);
        }
    }

    public int submitReadyToHrManager(int year, int month,
                                      Integer departmentId) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "UPDATE attendance_reports "
          + "SET status='Pending HR Manager Approval', submitted_at=NOW(), "
          + "    reviewed_by=NULL, reviewed_at=NULL, hr_note=NULL, updated_at=NOW() "
          + "WHERE report_year=? AND report_month=? "
          + "  AND status IN ('Submitted To HR Staff', 'Rejected By HR Manager') ");
        sql.append(managerConfirmedPredicate("attendance_reports"));
        if (departmentId != null) sql.append("AND department_id=? ");

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            ps.setInt(idx++, year);
            ps.setInt(idx++, month);
            if (departmentId != null) ps.setInt(idx, departmentId);
            return ps.executeUpdate();
        } finally {
            close(conn, ps, null);
        }
    }

    public boolean isMonthLockedForImport(int year, int month) throws SQLException {
        String sql =
            "SELECT 1 "
          + "FROM attendance_reports "
          + "WHERE report_year=? AND report_month=? "
          + "  AND status IN ("
          + "      'Submitted To HR Staff', "
          + "      'Pending HR Manager Approval', "
          + "      'Approved By HR Manager', "
          + "      'Rejected By HR Manager'"
          + "  ) "
          + "LIMIT 1";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, year);
            ps.setInt(2, month);
            rs = ps.executeQuery();
            return rs.next();
        } finally {
            close(conn, ps, rs);
        }
    }

    public boolean hasHrManagerApprovedMonth(int year, int month) throws SQLException {
        return hasHrManagerApprovedMonth(year, month, null);
    }

    public boolean hasHrManagerApprovedMonth(int year, int month,
                                             Integer departmentId) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT 1 "
          + "FROM attendance_reports "
          + "WHERE report_year=? AND report_month=? "
          + "  AND status='Approved By HR Manager' ");
        if (departmentId != null) sql.append("AND department_id=? ");
        sql.append("LIMIT 1");

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
            return rs.next();
        } finally {
            close(conn, ps, rs);
        }
    }

    public boolean hasHrManagerApprovedMonthForManager(int year, int month,
                                                        int managerUserId) throws SQLException {
        String sql = "SELECT 1 FROM attendance_reports "
                   + "WHERE report_year=? AND report_month=? "
                   + "AND manager_id=? AND status='Approved By HR Manager' LIMIT 1";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, year);
            ps.setInt(2, month);
            ps.setInt(3, managerUserId);
            rs = ps.executeQuery();
            return rs.next();
        } finally {
            close(conn, ps, rs);
        }
    }

    public AttendanceReport findById(int attendanceReportId) throws SQLException {
        String sql =
            "SELECT ar.*, "
          + "  eu.full_name AS emp_full_name, e.employee_code, "
          + "  d.department_name, mu.full_name AS manager_full_name "
          + "FROM attendance_reports ar "
          + "JOIN employees e   ON ar.employee_id  = e.employee_id "
          + "JOIN users eu      ON e.user_id       = eu.user_id "
          + "JOIN departments d ON ar.department_id = d.department_id "
          + "JOIN users mu      ON ar.manager_id    = mu.user_id "
          + "WHERE ar.attendance_report_id=?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, attendanceReportId);
            rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        } finally {
            close(conn, ps, rs);
        }
    }

    public boolean updateHrManagerDecision(int attendanceReportId, Status status,
                                           int reviewedBy, String note) throws SQLException {
        if (status != Status.ApprovedByHrManager && status != Status.RejectedByHrManager) {
            throw new IllegalArgumentException("Invalid HR Manager decision status.");
        }
        String sql = "UPDATE attendance_reports "
                   + "SET status=?, reviewed_by=?, reviewed_at=NOW(), hr_note=?, updated_at=NOW() "
                   + "WHERE attendance_report_id=? AND status='Pending HR Manager Approval'"
                   + managerConfirmedPredicate("attendance_reports");
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, status.getDbValue());
            ps.setInt(2, reviewedBy);
            ps.setString(3, note);
            ps.setInt(4, attendanceReportId);
            return ps.executeUpdate() > 0;
        } finally {
            close(conn, ps, null);
        }
    }

    public List<AttendanceReport> findSubmittedByMonthPage(int year, int month,
                                                           Integer departmentId, Integer managerUserId,
                                                           int offset, int limit) throws SQLException {
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
        sql.append(managerConfirmedPredicate("ar"));
        if (departmentId != null) sql.append("AND ar.department_id=? ");
        if (managerUserId != null) sql.append("AND ar.manager_id=? ");
        sql.append("ORDER BY d.department_name, eu.full_name");
        sql.append(" LIMIT ? OFFSET ?");

        List<AttendanceReport> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            ps.setInt(idx++, year);
            ps.setInt(idx++, month);
            if (departmentId != null) ps.setInt(idx++, departmentId);
            if (managerUserId != null) ps.setInt(idx++, managerUserId);
            ps.setInt(idx++, limit);
            ps.setInt(idx, offset);
            rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    private String managerConfirmedPredicate(String reportAlias) {
        return " AND ("
             + roleExemptFromManagerConfirmation(reportAlias)
             + " OR ("
             + "  EXISTS ("
             + "    SELECT 1 "
             + "    FROM users mgr "
             + "    JOIN roles mgr_role ON mgr.role_id = mgr_role.role_id "
             + "    WHERE mgr.user_id = " + reportAlias + ".manager_id "
             + "      AND mgr_role.role_name = 'MANAGER'"
             + "  ) "
             + "  AND EXISTS ("
             + "    SELECT 1 "
             + "    FROM attendance_records verified_ar "
             + "    WHERE verified_ar.employee_id = " + reportAlias + ".employee_id "
             + "      AND verified_ar.verification_status = 'Verified' "
             + "      AND verified_ar.verified_by = " + reportAlias + ".manager_id "
             + "      AND YEAR(verified_ar.work_date) = " + reportAlias + ".report_year "
             + "      AND MONTH(verified_ar.work_date) = " + reportAlias + ".report_month"
             + "  )"
             + " )"
             + ") ";
    }

    private String roleExemptFromManagerConfirmation(String reportAlias) {
        return "EXISTS ("
             + "  SELECT 1 "
             + "  FROM employees exempt_emp "
             + "  JOIN users exempt_user ON exempt_emp.user_id = exempt_user.user_id "
             + "  JOIN roles exempt_role ON exempt_user.role_id = exempt_role.role_id "
             + "  WHERE exempt_emp.employee_id = " + reportAlias + ".employee_id "
             + "    AND exempt_role.role_name IN ('HR_STAFF', 'MANAGER', 'HR_MANAGER')"
             + ")";
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
        r.setMaternityLeaveDays(rs.getBigDecimal("maternity_leave_days"));
        r.setOvertimeHours(rs.getBigDecimal("overtime_hours"));
        r.setLatePenaltyAmount(rs.getBigDecimal("late_penalty_amount"));
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
