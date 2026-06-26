package dao;

import config.DBContext;
import model.PayrollPeriod;
import model.PayrollPeriod.Status;

import java.sql.*;

public class PayrollPeriodDAO {

    /** Create a Draft period for (month, year). Returns new id, or -1 if it
     *  already exists (unique key month+year). */
    public int createDraft(PayrollPeriod p) throws SQLException {
        String sql = "INSERT INTO payroll_periods "
                   + "(period_name, payroll_month, payroll_year, status, created_by) "
                   + "VALUES (?, ?, ?, 'Draft', ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, p.getPeriodName());
            ps.setInt(2, p.getPayrollMonth());
            ps.setInt(3, p.getPayrollYear());
            if (p.getCreatedBy() != null) ps.setInt(4, p.getCreatedBy());
            else                          ps.setNull(4, Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } catch (SQLIntegrityConstraintViolationException dup) {
            return -1; // period already exists for this month/year
        } finally {
            close(conn, ps, null);
        }
        return -1;
    }

    public PayrollPeriod findByMonth(int year, int month) throws SQLException {
        return findOne("WHERE pp.payroll_year=? AND pp.payroll_month=?", year, month);
    }

    public PayrollPeriod findById(int id) throws SQLException {
        return findOne("WHERE pp.payroll_period_id=?", id);
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
             + "  (SELECT COUNT(*) FROM payrolls p WHERE p.payroll_period_id = pp.payroll_period_id) AS payroll_count "
             + "FROM payroll_periods pp "
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

    private PayrollPeriod mapRow(ResultSet rs) throws SQLException {
        PayrollPeriod p = new PayrollPeriod();
        p.setPayrollPeriodId(rs.getInt("payroll_period_id"));
        p.setPeriodName(rs.getString("period_name"));
        p.setPayrollMonth(rs.getInt("payroll_month"));
        p.setPayrollYear(rs.getInt("payroll_year"));
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
        p.setPayrollCount(rs.getInt("payroll_count"));
        return p;
    }

    private void close(Connection c, PreparedStatement ps, ResultSet rs) {
        try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
        try { if (ps != null) ps.close(); } catch (SQLException ignored) {}
        try { if (c  != null) c.close();  } catch (SQLException ignored) {}
    }
}
