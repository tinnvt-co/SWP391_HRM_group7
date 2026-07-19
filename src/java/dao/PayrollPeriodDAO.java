package dao;

import config.DBContext;
import model.PayrollTaskSummary;
import model.PayrollPeriod;
import model.PayrollPeriod.Status;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PayrollPeriodDAO {

    /** Create a Draft period for (month, year, department). Returns new id, or -1 if it already exists. */
    public int createDraft(PayrollPeriod p) throws SQLException {
        String sql = "INSERT INTO payroll_periods "
                   + "(period_name, payroll_month, payroll_year, department_id, status, created_by) "
                   + "VALUES (?, ?, ?, ?, 'Draft', ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, p.getPeriodName());
            ps.setInt(2, p.getPayrollMonth());
            ps.setInt(3, p.getPayrollYear());
            ps.setInt(4, p.getDepartmentId());
            if (p.getCreatedBy() != null) ps.setInt(5, p.getCreatedBy());
            else                          ps.setNull(5, Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLIntegrityConstraintViolationException dup) {
            return -1; // period already exists for this month/year/department
        } finally {
            close(conn, ps, null);
        }
        return -1;
    }

    public PayrollPeriod findByMonth(int year, int month) throws SQLException {
        return findOne("WHERE pp.payroll_year=? AND pp.payroll_month=?", year, month);
    }

    public PayrollPeriod findByMonthAndDepartment(int year, int month, int departmentId) throws SQLException {
        return findOne("WHERE pp.payroll_year=? AND pp.payroll_month=? AND pp.department_id=?",
                year, month, departmentId);
    }

    public PayrollPeriod findById(int id) throws SQLException {
        return findOne("WHERE pp.payroll_period_id=?", id);
    }

    public List<PayrollPeriod> findByMonthAndStatuses(int year, int month,
                                                      Status... statuses) throws SQLException {
        List<PayrollPeriod> list = new ArrayList<>();
        if (statuses == null || statuses.length == 0) return list;

        StringBuilder sql = new StringBuilder(baseSelect())
                .append("WHERE pp.payroll_year=? AND pp.payroll_month=? AND pp.status IN (");
        for (int i = 0; i < statuses.length; i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }
        sql.append(") ORDER BY d.department_name");

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            ps.setInt(idx++, year);
            ps.setInt(idx++, month);
            for (Status status : statuses) {
                ps.setString(idx++, status.getDbValue());
            }
            rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    private PayrollPeriod findOne(String where, int... args) throws SQLException {
        String sql = baseSelect() + where;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            for (int i = 0; i < args.length; i++) ps.setInt(i + 1, args[i]);
            rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        } finally {
            close(conn, ps, rs);
        }
    }

    private String baseSelect() {
        return "SELECT pp.*, "
             + "  cu.full_name AS created_by_name, au.full_name AS approved_by_name, "
             + "  d.department_name, "
             + "  (SELECT COUNT(*) FROM payrolls p WHERE p.payroll_period_id = pp.payroll_period_id) AS payroll_count "
             + "FROM payroll_periods pp "
             + "JOIN departments d ON pp.department_id = d.department_id "
             + "LEFT JOIN users cu ON pp.created_by  = cu.user_id "
             + "LEFT JOIN users au ON pp.approved_by = au.user_id ";
    }

    /** Generic status transition. Sets approved_by/paid_by/reject_reason as needed. */
    public boolean updateStatus(int periodId, Status newStatus,
                                Integer actorUserId, String rejectReason) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "UPDATE payroll_periods SET status=?, updated_at=NOW()");
        if (newStatus == Status.Approved || newStatus == Status.Rejected) {
            sql.append(", approved_by=?");
        }
        if (newStatus == Status.Paid) {
            sql.append(", paid_by=?, payment_date=CURDATE()");
        }
        if (newStatus == Status.Rejected) {
            sql.append(", reject_reason=?");
        } else {
            sql.append(", reject_reason=NULL");
        }
        sql.append(" WHERE payroll_period_id=?");

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            int i = 1;
            ps.setString(i++, newStatus.getDbValue());
            if (newStatus == Status.Approved || newStatus == Status.Rejected) {
                if (actorUserId != null) ps.setInt(i++, actorUserId);
                else                     ps.setNull(i++, Types.INTEGER);
            }
            if (newStatus == Status.Paid) {
                if (actorUserId != null) ps.setInt(i++, actorUserId);
                else                     ps.setNull(i++, Types.INTEGER);
            }
            if (newStatus == Status.Rejected) {
                ps.setString(i++, rejectReason);
            }
            ps.setInt(i, periodId);
            return ps.executeUpdate() > 0;
        } finally {
            close(conn, ps, null);
        }
    }

    public PayrollTaskSummary findHrStaffTaskSummary() throws SQLException {
        Connection conn = null;
        try {
            conn = DBContext.getConnection();

            PayrollTaskSummary summary = new PayrollTaskSummary();
            summary.setCount(countHrStaffMissingPayrollTasks(conn));
            copyTask(summary, findLatestHrStaffMissingPayrollTask(conn));
            return summary;
        } finally {
            close(conn, null, null);
        }
    }

    public PayrollTaskSummary findHrManagerTaskSummary() throws SQLException {
        Connection conn = null;
        try {
            conn = DBContext.getConnection();

            PayrollTaskSummary summary = new PayrollTaskSummary();
            summary.setCount(countHrManagerApprovalTasks(conn));
            copyTask(summary, findLatestHrManagerApprovalTask(conn));
            return summary;
        } finally {
            close(conn, null, null);
        }
    }

    private int countHrStaffPeriodTasks(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM payroll_periods "
                   + "WHERE status IN ('Draft', 'Rejected', 'Approved')";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private int countHrStaffMissingPayrollTasks(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM ("
                   + "  SELECT pp.payroll_year AS task_year, pp.payroll_month AS task_month "
                   + "  FROM payroll_periods pp "
                   + "  WHERE pp.status IN ('Draft', 'Rejected', 'Approved') "
                   + "  GROUP BY pp.payroll_year, pp.payroll_month "
                   + "  UNION "
                   + "  SELECT ar.report_year AS task_year, ar.report_month AS task_month "
                   + "  FROM attendance_reports ar "
                   + "  LEFT JOIN payroll_periods pp "
                   + "    ON pp.department_id = ar.department_id "
                   + "   AND pp.payroll_year = ar.report_year "
                   + "   AND pp.payroll_month = ar.report_month "
                   + "  WHERE ar.status = 'Approved By HR Manager' "
                   + "    AND pp.payroll_period_id IS NULL "
                   + "  GROUP BY ar.report_year, ar.report_month"
                   + ") pending_periods";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private int countHrManagerApprovalTasks(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM ("
                   + "  SELECT payroll_year, payroll_month "
                   + "  FROM payroll_periods "
                   + "  WHERE status = 'Pending Approval' "
                   + "  GROUP BY payroll_year, payroll_month"
                   + ") pending_periods";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private PayrollTaskSummary findLatestHrStaffPeriodTask(Connection conn) throws SQLException {
        String sql = "SELECT pp.department_id, d.department_name, "
                   + "       pp.payroll_month AS task_month, pp.payroll_year AS task_year, "
                   + "       CASE pp.status "
                   + "         WHEN 'Draft' THEN 'Submit for Approval' "
                   + "         WHEN 'Rejected' THEN 'Re-submit for Approval' "
                   + "         WHEN 'Approved' THEN 'Confirm Payment' "
                   + "         ELSE pp.status "
                   + "       END AS task_label "
                   + "FROM payroll_periods pp "
                   + "JOIN departments d ON pp.department_id = d.department_id "
                   + "WHERE pp.status IN ('Draft', 'Rejected', 'Approved') "
                   + "ORDER BY pp.payroll_year DESC, pp.payroll_month DESC, "
                   + "  CASE pp.status WHEN 'Approved' THEN 0 WHEN 'Rejected' THEN 1 ELSE 2 END, "
                   + "  pp.updated_at DESC, pp.created_at DESC "
                   + "LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? mapTaskSummary(rs) : null;
        }
    }

    private PayrollTaskSummary findLatestHrStaffMissingPayrollTask(Connection conn) throws SQLException {
        String sql = "SELECT NULL AS department_id, 'All departments' AS department_name, "
                   + "       pending_periods.task_month, pending_periods.task_year, "
                   + "       'Process Payroll' AS task_label "
                   + "FROM ("
                   + "  SELECT pp.payroll_year AS task_year, pp.payroll_month AS task_month "
                   + "  FROM payroll_periods pp "
                   + "  WHERE pp.status IN ('Draft', 'Rejected', 'Approved') "
                   + "  GROUP BY pp.payroll_year, pp.payroll_month "
                   + "  UNION "
                   + "  SELECT ar.report_year AS task_year, ar.report_month AS task_month "
                   + "  FROM attendance_reports ar "
                   + "  LEFT JOIN payroll_periods pp "
                   + "    ON pp.department_id = ar.department_id "
                   + "   AND pp.payroll_year = ar.report_year "
                   + "   AND pp.payroll_month = ar.report_month "
                   + "  WHERE ar.status = 'Approved By HR Manager' "
                   + "    AND pp.payroll_period_id IS NULL "
                   + "  GROUP BY ar.report_year, ar.report_month"
                   + ") pending_periods "
                   + "ORDER BY pending_periods.task_year DESC, pending_periods.task_month DESC "
                   + "LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? mapTaskSummary(rs) : null;
        }
    }

    private PayrollTaskSummary findLatestHrManagerApprovalTask(Connection conn) throws SQLException {
        String sql = "SELECT NULL AS department_id, 'All departments' AS department_name, "
                   + "       pp.payroll_month AS task_month, pp.payroll_year AS task_year, "
                   + "       'Approve Payroll' AS task_label "
                   + "FROM payroll_periods pp "
                   + "WHERE pp.status = 'Pending Approval' "
                   + "GROUP BY pp.payroll_month, pp.payroll_year "
                   + "ORDER BY pp.payroll_year DESC, pp.payroll_month DESC, "
                   + "  MAX(pp.updated_at) DESC, MAX(pp.created_at) DESC "
                   + "LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? mapTaskSummary(rs) : null;
        }
    }

    private PayrollTaskSummary latestTask(PayrollTaskSummary first, PayrollTaskSummary second) {
        if (first == null) return second;
        if (second == null) return first;
        if (first.getYear() != second.getYear()) {
            return first.getYear() > second.getYear() ? first : second;
        }
        if (first.getMonth() != second.getMonth()) {
            return first.getMonth() > second.getMonth() ? first : second;
        }
        return first;
    }

    private void copyTask(PayrollTaskSummary target, PayrollTaskSummary source) {
        if (source == null) return;
        target.setDepartmentId(source.getDepartmentId());
        target.setDepartmentName(source.getDepartmentName());
        target.setMonth(source.getMonth());
        target.setYear(source.getYear());
        target.setTaskLabel(source.getTaskLabel());
    }

    private PayrollTaskSummary mapTaskSummary(ResultSet rs) throws SQLException {
        PayrollTaskSummary summary = new PayrollTaskSummary();
        summary.setDepartmentId(rs.getInt("department_id"));
        summary.setDepartmentName(rs.getString("department_name"));
        summary.setMonth(rs.getInt("task_month"));
        summary.setYear(rs.getInt("task_year"));
        summary.setTaskLabel(rs.getString("task_label"));
        return summary;
    }

    private PayrollPeriod mapRow(ResultSet rs) throws SQLException {
        PayrollPeriod p = new PayrollPeriod();
        p.setPayrollPeriodId(rs.getInt("payroll_period_id"));
        p.setPeriodName(rs.getString("period_name"));
        p.setPayrollMonth(rs.getInt("payroll_month"));
        p.setPayrollYear(rs.getInt("payroll_year"));
        p.setDepartmentId(rs.getInt("department_id"));
        Date pd = rs.getDate("payment_date");
        if (pd != null) p.setPaymentDate(pd.toLocalDate());
        p.setStatus(Status.fromDb(rs.getString("status")));
        int cb = rs.getInt("created_by");  if (!rs.wasNull()) p.setCreatedBy(cb);
        int ab = rs.getInt("approved_by"); if (!rs.wasNull()) p.setApprovedBy(ab);
        int pb = rs.getInt("paid_by");     if (!rs.wasNull()) p.setPaidBy(pb);
        p.setRejectReason(rs.getString("reject_reason"));
        Timestamp c = rs.getTimestamp("created_at"); if (c != null) p.setCreatedAt(c.toLocalDateTime());
        Timestamp u = rs.getTimestamp("updated_at"); if (u != null) p.setUpdatedAt(u.toLocalDateTime());
        p.setCreatedByName(rs.getString("created_by_name"));
        p.setApprovedByName(rs.getString("approved_by_name"));
        p.setDepartmentName(rs.getString("department_name"));
        p.setPayrollCount(rs.getInt("payroll_count"));
        return p;
    }

    private void close(Connection c, PreparedStatement ps, ResultSet rs) {
        try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
        try { if (ps != null) ps.close(); } catch (SQLException ignored) {}
        try { if (c  != null) c.close();  } catch (SQLException ignored) {}
    }
}
