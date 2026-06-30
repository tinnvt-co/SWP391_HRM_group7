package dao;

import config.DBContext;
import model.Payroll;
import model.Payroll.Status;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PayrollDAO {

    /** Insert one payroll line. Returns generated id. */
    public int insert(Payroll p) throws SQLException {
        String sql = "INSERT INTO payrolls "
                   + "(payroll_period_id, employee_id, contract_id, attendance_report_id, "
                   + " basic_salary, actual_working_days, total_allowance, kpi_bonus, overtime_salary, "
                   + " gross_salary, total_deduction, net_salary, status, note) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Draft', ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, p.getPayrollPeriodId());
            ps.setInt(2, p.getEmployeeId());
            if (p.getContractId() != null) ps.setInt(3, p.getContractId());
            else                           ps.setNull(3, Types.INTEGER);
            if (p.getAttendanceReportId() != null) ps.setInt(4, p.getAttendanceReportId());
            else                                   ps.setNull(4, Types.INTEGER);
            ps.setBigDecimal(5, nz(p.getBasicSalary()));
            ps.setBigDecimal(6, nz(p.getActualWorkingDays()));
            ps.setBigDecimal(7, nz(p.getTotalAllowance()));
            ps.setBigDecimal(8, nz(p.getKpiBonus()));
            ps.setBigDecimal(9, nz(p.getOvertimeSalary()));
            ps.setBigDecimal(10, nz(p.getGrossSalary()));
            ps.setBigDecimal(11, nz(p.getTotalDeduction()));
            ps.setBigDecimal(12, nz(p.getNetSalary()));
            ps.setString(13, p.getNote());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } finally {
            close(conn, ps, null);
        }
        return -1;
    }

    public List<Payroll> findByPeriod(int periodId) throws SQLException {
        String sql = baseSelect() + "WHERE p.payroll_period_id=? ORDER BY d.department_name, eu.full_name";
        List<Payroll> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, periodId);
            rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public int countByPeriod(int periodId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM payrolls WHERE payroll_period_id=?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, periodId);
            rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } finally {
            close(conn, ps, rs);
        }
    }

    public List<Payroll> findByPeriodPage(int periodId, int offset, int limit) throws SQLException {
        String sql = baseSelect()
                   + "WHERE p.payroll_period_id=? "
                   + "ORDER BY d.department_name, eu.full_name "
                   + "LIMIT ? OFFSET ?";
        List<Payroll> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, periodId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public Payroll findById(int payrollId) throws SQLException {
        String sql = baseSelect() + "WHERE p.payroll_id=?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, payrollId);
            rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        } finally {
            close(conn, ps, rs);
        }
    }

    /** Paid payslip for one employee in a given month (employee self-view). */
    public Payroll findPaidByEmployeeAndMonth(int employeeId, int year, int month) throws SQLException {
        String sql = baseSelect()
                   + "WHERE p.employee_id=? AND pp.payroll_year=? AND pp.payroll_month=? "
                   + "  AND p.status='Paid'";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, employeeId);
            ps.setInt(2, year);
            ps.setInt(3, month);
            rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        } finally {
            close(conn, ps, rs);
        }
    }

    private String baseSelect() {
        return "SELECT p.*, eu.full_name AS emp_full_name, e.employee_code, d.department_name, "
             + "  pp.payroll_month, pp.payroll_year "
             + "FROM payrolls p "
             + "JOIN employees e   ON p.employee_id   = e.employee_id "
             + "JOIN users eu      ON e.user_id        = eu.user_id "
             + "JOIN departments d ON e.department_id   = d.department_id "
             + "JOIN payroll_periods pp ON p.payroll_period_id = pp.payroll_period_id ";
    }

    /** Move every line of a period to a new status (follows the parent period). */
    public int updateStatusByPeriod(int periodId, Status newStatus) throws SQLException {
        String sql = "UPDATE payrolls SET status=?, updated_at=NOW() WHERE payroll_period_id=?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, newStatus.getDbValue());
            ps.setInt(2, periodId);
            return ps.executeUpdate();
        } finally {
            close(conn, ps, null);
        }
    }

    /** Remove all lines of a period (used when regenerating a Draft). */
    public int deleteByPeriod(int periodId) throws SQLException {
        String sql = "DELETE FROM payrolls WHERE payroll_period_id=?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, periodId);
            return ps.executeUpdate();
        } finally {
            close(conn, ps, null);
        }
    }

    private Payroll mapRow(ResultSet rs) throws SQLException {
        Payroll p = new Payroll();
        p.setPayrollId(rs.getInt("payroll_id"));
        p.setPayrollPeriodId(rs.getInt("payroll_period_id"));
        p.setEmployeeId(rs.getInt("employee_id"));
        int cid = rs.getInt("contract_id"); if (!rs.wasNull()) p.setContractId(cid);
        int aid = rs.getInt("attendance_report_id"); if (!rs.wasNull()) p.setAttendanceReportId(aid);
        p.setBasicSalary(rs.getBigDecimal("basic_salary"));
        p.setActualWorkingDays(rs.getBigDecimal("actual_working_days"));
        p.setTotalAllowance(rs.getBigDecimal("total_allowance"));
        p.setKpiBonus(rs.getBigDecimal("kpi_bonus"));
        p.setOvertimeSalary(rs.getBigDecimal("overtime_salary"));
        p.setGrossSalary(rs.getBigDecimal("gross_salary"));
        p.setTotalDeduction(rs.getBigDecimal("total_deduction"));
        p.setNetSalary(rs.getBigDecimal("net_salary"));
        p.setStatus(Status.fromDb(rs.getString("status")));
        p.setNote(rs.getString("note"));
        Timestamp c = rs.getTimestamp("created_at"); if (c != null) p.setCreatedAt(c.toLocalDateTime());
        Timestamp u = rs.getTimestamp("updated_at"); if (u != null) p.setUpdatedAt(u.toLocalDateTime());
        p.setEmployeeFullName(rs.getString("emp_full_name"));
        p.setEmployeeCode(rs.getString("employee_code"));
        p.setDepartmentName(rs.getString("department_name"));
        return p;
    }

    private static BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    private void close(Connection c, PreparedStatement ps, ResultSet rs) {
        try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
        try { if (ps != null) ps.close(); } catch (SQLException ignored) {}
        try { if (c  != null) c.close();  } catch (SQLException ignored) {}
    }
}
