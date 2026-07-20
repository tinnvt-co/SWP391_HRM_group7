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
                   + " basic_salary, actual_working_days, work_salary, total_allowance, "
                   + " attendance_bonus_amount, kpi_bonus, "
                   + " normal_overtime_hours, weekend_overtime_hours, holiday_overtime_hours, "
                   + " normal_overtime_salary, weekend_overtime_salary, holiday_overtime_salary, "
                   + " overtime_salary, gross_salary, insurance_base, social_insurance, "
                   + " health_insurance, unemployment_insurance, personal_income_tax, "
                   + " advance_payment, late_penalty_amount, total_deduction, net_salary, maternity_leave_days, "
                   + " social_insurance_benefit, status, note) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Draft', ?)";
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
            ps.setBigDecimal(7, nz(p.getWorkSalary()));
            ps.setBigDecimal(8, nz(p.getTotalAllowance()));
            ps.setBigDecimal(9, nz(p.getAttendanceBonusAmount()));
            ps.setBigDecimal(10, nz(p.getKpiBonus()));
            ps.setBigDecimal(11, nz(p.getNormalOvertimeHours()));
            ps.setBigDecimal(12, nz(p.getWeekendOvertimeHours()));
            ps.setBigDecimal(13, nz(p.getHolidayOvertimeHours()));
            ps.setBigDecimal(14, nz(p.getNormalOvertimeSalary()));
            ps.setBigDecimal(15, nz(p.getWeekendOvertimeSalary()));
            ps.setBigDecimal(16, nz(p.getHolidayOvertimeSalary()));
            ps.setBigDecimal(17, nz(p.getOvertimeSalary()));
            ps.setBigDecimal(18, nz(p.getGrossSalary()));
            ps.setBigDecimal(19, nz(p.getInsuranceBase()));
            ps.setBigDecimal(20, nz(p.getSocialInsurance()));
            ps.setBigDecimal(21, nz(p.getHealthInsurance()));
            ps.setBigDecimal(22, nz(p.getUnemploymentInsurance()));
            ps.setBigDecimal(23, nz(p.getPersonalIncomeTax()));
            ps.setBigDecimal(24, nz(p.getAdvancePayment()));
            ps.setBigDecimal(25, nz(p.getLatePenaltyAmount()));
            ps.setBigDecimal(26, nz(p.getTotalDeduction()));
            ps.setBigDecimal(27, nz(p.getNetSalary()));
            ps.setBigDecimal(28, nz(p.getMaternityLeaveDays()));
            ps.setBigDecimal(29, nz(p.getSocialInsuranceBenefit()));
            ps.setString(30, p.getNote());
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

    public int countByMonth(int year, int month) throws SQLException {
        String sql = "SELECT COUNT(*) "
                   + "FROM payrolls p "
                   + "JOIN payroll_periods pp ON p.payroll_period_id = pp.payroll_period_id "
                   + "WHERE pp.payroll_year=? AND pp.payroll_month=?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, year);
            ps.setInt(2, month);
            rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } finally {
            close(conn, ps, rs);
        }
    }

    public boolean existsByPeriodAndEmployee(int periodId, int employeeId) throws SQLException {
        String sql = "SELECT 1 FROM payrolls WHERE payroll_period_id=? AND employee_id=?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, periodId);
            ps.setInt(2, employeeId);
            rs = ps.executeQuery();
            return rs.next();
        } finally {
            close(conn, ps, rs);
        }
    }

    public int countByMonthAndStatuses(int year, int month, Status... statuses) throws SQLException {
        if (statuses == null || statuses.length == 0) return 0;
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) ")
                .append("FROM payrolls p ")
                .append("JOIN payroll_periods pp ON p.payroll_period_id = pp.payroll_period_id ")
                .append("JOIN attendance_reports ar ON p.attendance_report_id = ar.attendance_report_id ")
                .append("WHERE pp.payroll_year=? AND pp.payroll_month=? AND pp.status IN (");
        appendPlaceholders(sql, statuses.length);
        sql.append(") ").append(managerConfirmedReportPredicate("ar"));

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            ps.setInt(idx++, year);
            ps.setInt(idx++, month);
            setStatusParams(ps, idx, statuses);
            rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } finally {
            close(conn, ps, rs);
        }
    }

    public int countByDepartmentMonth(int departmentId, int year, int month) throws SQLException {
        String sql = "SELECT COUNT(*) "
                   + "FROM payrolls p "
                   + "JOIN payroll_periods pp ON p.payroll_period_id = pp.payroll_period_id "
                   + "WHERE pp.department_id=? AND pp.payroll_year=? AND pp.payroll_month=?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, departmentId);
            ps.setInt(2, year);
            ps.setInt(3, month);
            rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } finally {
            close(conn, ps, rs);
        }
    }

    public BigDecimal sumNetSalaryByDepartmentMonth(int departmentId, int year, int month)
            throws SQLException {
        String sql = "SELECT COALESCE(SUM(p.net_salary), 0) "
                   + "FROM payrolls p "
                   + "JOIN payroll_periods pp ON p.payroll_period_id = pp.payroll_period_id "
                   + "WHERE pp.department_id = ? "
                   + "AND pp.payroll_year = ? "
                   + "AND pp.payroll_month = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, departmentId);
            ps.setInt(2, year);
            ps.setInt(3, month);
            rs = ps.executeQuery();
            return rs.next() ? nz(rs.getBigDecimal(1)) : BigDecimal.ZERO;
        } finally {
            close(conn, ps, rs);
        }
    }

    public BigDecimal sumNetSalaryByDepartmentYear(int departmentId, int year)
            throws SQLException {
        String sql = "SELECT COALESCE(SUM(p.net_salary), 0) "
                   + "FROM payrolls p "
                   + "JOIN payroll_periods pp ON p.payroll_period_id = pp.payroll_period_id "
                   + "WHERE pp.department_id = ? "
                   + "AND pp.payroll_year = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, departmentId);
            ps.setInt(2, year);
            rs = ps.executeQuery();
            return rs.next() ? nz(rs.getBigDecimal(1)) : BigDecimal.ZERO;
        } finally {
            close(conn, ps, rs);
        }
    }

    public BigDecimal sumNetSalaryByMonth(int year, int month) throws SQLException {
        String sql = "SELECT COALESCE(SUM(p.net_salary), 0) "
                   + "FROM payrolls p "
                   + "JOIN payroll_periods pp ON p.payroll_period_id = pp.payroll_period_id "
                   + "WHERE pp.payroll_year = ? "
                   + "AND pp.payroll_month = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, year);
            ps.setInt(2, month);
            rs = ps.executeQuery();
            return rs.next() ? nz(rs.getBigDecimal(1)) : BigDecimal.ZERO;
        } finally {
            close(conn, ps, rs);
        }
    }

    public BigDecimal sumNetSalaryByMonthAndStatuses(int year, int month, Status... statuses)
            throws SQLException {
        if (statuses == null || statuses.length == 0) return BigDecimal.ZERO;
        StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(p.net_salary), 0) ")
                .append("FROM payrolls p ")
                .append("JOIN payroll_periods pp ON p.payroll_period_id = pp.payroll_period_id ")
                .append("JOIN attendance_reports ar ON p.attendance_report_id = ar.attendance_report_id ")
                .append("WHERE pp.payroll_year = ? ")
                .append("AND pp.payroll_month = ? ")
                .append("AND pp.status IN (");
        appendPlaceholders(sql, statuses.length);
        sql.append(") ").append(managerConfirmedReportPredicate("ar"));

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            ps.setInt(idx++, year);
            ps.setInt(idx++, month);
            setStatusParams(ps, idx, statuses);
            rs = ps.executeQuery();
            return rs.next() ? nz(rs.getBigDecimal(1)) : BigDecimal.ZERO;
        } finally {
            close(conn, ps, rs);
        }
    }

    public BigDecimal sumNetSalaryByYear(int year) throws SQLException {
        String sql = "SELECT COALESCE(SUM(p.net_salary), 0) "
                   + "FROM payrolls p "
                   + "JOIN payroll_periods pp ON p.payroll_period_id = pp.payroll_period_id "
                   + "WHERE pp.payroll_year = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, year);
            rs = ps.executeQuery();
            return rs.next() ? nz(rs.getBigDecimal(1)) : BigDecimal.ZERO;
        } finally {
            close(conn, ps, rs);
        }
    }

    public BigDecimal sumNetSalaryByYearAndStatuses(int year, Status... statuses)
            throws SQLException {
        if (statuses == null || statuses.length == 0) return BigDecimal.ZERO;
        StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(p.net_salary), 0) ")
                .append("FROM payrolls p ")
                .append("JOIN payroll_periods pp ON p.payroll_period_id = pp.payroll_period_id ")
                .append("JOIN attendance_reports ar ON p.attendance_report_id = ar.attendance_report_id ")
                .append("WHERE pp.payroll_year = ? ")
                .append("AND pp.status IN (");
        appendPlaceholders(sql, statuses.length);
        sql.append(") ").append(managerConfirmedReportPredicate("ar"));

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            ps.setInt(idx++, year);
            setStatusParams(ps, idx, statuses);
            rs = ps.executeQuery();
            return rs.next() ? nz(rs.getBigDecimal(1)) : BigDecimal.ZERO;
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

    public List<Payroll> findByMonthPage(int year, int month, int offset, int limit) throws SQLException {
        String sql = baseSelect()
                   + "WHERE pp.payroll_year=? AND pp.payroll_month=? "
                   + "ORDER BY d.department_name, eu.full_name "
                   + "LIMIT ? OFFSET ?";
        List<Payroll> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, year);
            ps.setInt(2, month);
            ps.setInt(3, limit);
            ps.setInt(4, offset);
            rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public List<Payroll> findByMonthPageAndStatuses(int year, int month, int offset, int limit,
                                                    Status... statuses) throws SQLException {
        List<Payroll> list = new ArrayList<>();
        if (statuses == null || statuses.length == 0) return list;

        StringBuilder sql = new StringBuilder(baseSelect())
                .append("JOIN attendance_reports ar ON p.attendance_report_id = ar.attendance_report_id ")
                .append("WHERE pp.payroll_year=? AND pp.payroll_month=? AND pp.status IN (");
        appendPlaceholders(sql, statuses.length);
        sql.append(") ")
           .append(managerConfirmedReportPredicate("ar"))
           .append("ORDER BY d.department_name, eu.full_name LIMIT ? OFFSET ?");

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            ps.setInt(idx++, year);
            ps.setInt(idx++, month);
            idx = setStatusParams(ps, idx, statuses);
            ps.setInt(idx++, limit);
            ps.setInt(idx, offset);
            rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public List<Payroll> findByDepartmentMonthPage(int departmentId, int year, int month,
                                                   int offset, int limit) throws SQLException {
        String sql = baseSelect()
                   + "WHERE pp.department_id=? AND pp.payroll_year=? AND pp.payroll_month=? "
                   + "ORDER BY eu.full_name "
                   + "LIMIT ? OFFSET ?";
        List<Payroll> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, departmentId);
            ps.setInt(2, year);
            ps.setInt(3, month);
            ps.setInt(4, limit);
            ps.setInt(5, offset);
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

    /** Released payslip for one employee in a given month (employee self-view). */
    public Payroll findPaidByEmployeeAndMonth(int employeeId, int year, int month) throws SQLException {
        String sql = baseSelect()
                   + "JOIN attendance_reports ar ON p.attendance_report_id = ar.attendance_report_id "
                   + "WHERE p.employee_id=? AND pp.payroll_year=? AND pp.payroll_month=? "
                   + "  AND p.status = 'Paid' AND pp.status = 'Paid' "
                   + managerConfirmedReportPredicate("ar");
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

    public boolean updateStatusById(int payrollId, Status newStatus) throws SQLException {
        String sql = "UPDATE payrolls SET status=?, updated_at=NOW() WHERE payroll_id=?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, newStatus.getDbValue());
            ps.setInt(2, payrollId);
            return ps.executeUpdate() > 0;
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
        p.setWorkSalary(rs.getBigDecimal("work_salary"));
        p.setTotalAllowance(rs.getBigDecimal("total_allowance"));
        p.setAttendanceBonusAmount(rs.getBigDecimal("attendance_bonus_amount"));
        p.setKpiBonus(rs.getBigDecimal("kpi_bonus"));
        p.setNormalOvertimeHours(rs.getBigDecimal("normal_overtime_hours"));
        p.setWeekendOvertimeHours(rs.getBigDecimal("weekend_overtime_hours"));
        p.setHolidayOvertimeHours(rs.getBigDecimal("holiday_overtime_hours"));
        p.setNormalOvertimeSalary(rs.getBigDecimal("normal_overtime_salary"));
        p.setWeekendOvertimeSalary(rs.getBigDecimal("weekend_overtime_salary"));
        p.setHolidayOvertimeSalary(rs.getBigDecimal("holiday_overtime_salary"));
        p.setOvertimeSalary(rs.getBigDecimal("overtime_salary"));
        p.setGrossSalary(rs.getBigDecimal("gross_salary"));
        p.setInsuranceBase(rs.getBigDecimal("insurance_base"));
        p.setSocialInsurance(rs.getBigDecimal("social_insurance"));
        p.setHealthInsurance(rs.getBigDecimal("health_insurance"));
        p.setUnemploymentInsurance(rs.getBigDecimal("unemployment_insurance"));
        p.setPersonalIncomeTax(rs.getBigDecimal("personal_income_tax"));
        p.setAdvancePayment(rs.getBigDecimal("advance_payment"));
        p.setLatePenaltyAmount(rs.getBigDecimal("late_penalty_amount"));
        p.setTotalDeduction(rs.getBigDecimal("total_deduction"));
        p.setNetSalary(rs.getBigDecimal("net_salary"));
        p.setMaternityLeaveDays(rs.getBigDecimal("maternity_leave_days"));
        p.setSocialInsuranceBenefit(rs.getBigDecimal("social_insurance_benefit"));
        p.setStatus(Status.fromDb(rs.getString("status")));
        p.setNote(rs.getString("note"));
        Timestamp c = rs.getTimestamp("created_at"); if (c != null) p.setCreatedAt(c.toLocalDateTime());
        Timestamp u = rs.getTimestamp("updated_at"); if (u != null) p.setUpdatedAt(u.toLocalDateTime());
        p.setEmployeeFullName(rs.getString("emp_full_name"));
        p.setEmployeeCode(rs.getString("employee_code"));
        p.setDepartmentName(rs.getString("department_name"));
        p.setOvertimeHours(nz(p.getNormalOvertimeHours())
                .add(nz(p.getWeekendOvertimeHours()))
                .add(nz(p.getHolidayOvertimeHours())));
        return p;
    }

    private static BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    private void appendPlaceholders(StringBuilder sql, int count) {
        for (int i = 0; i < count; i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }
    }

    private int setStatusParams(PreparedStatement ps, int start, Status... statuses) throws SQLException {
        int idx = start;
        for (Status status : statuses) {
            ps.setString(idx++, status.getDbValue());
        }
        return idx;
    }

    private String managerConfirmedReportPredicate(String reportAlias) {
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

    private void close(Connection c, PreparedStatement ps, ResultSet rs) {
        try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
        try { if (ps != null) ps.close(); } catch (SQLException ignored) {}
        try { if (c  != null) c.close();  } catch (SQLException ignored) {}
    }
}
