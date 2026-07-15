package dao;

import config.DBContext;
import model.AttendanceRecord;
import model.AttendanceRecord.AttendanceStatus;
import model.AttendanceRecord.VerificationStatus;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class AttendanceRecordDAO {

    public int insert(AttendanceRecord r) throws SQLException {
        String sql = "INSERT INTO attendance_records (employee_id, work_date, "
                   + "overtime_hours, attendance_status, verification_status, "
                   + "verified_by, verified_at, note) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, r.getEmployeeId());
            ps.setDate(2, Date.valueOf(r.getWorkDate()));
            ps.setBigDecimal(3, r.getOvertimeHours() != null ? r.getOvertimeHours() : java.math.BigDecimal.ZERO);
            ps.setString(4, r.getAttendanceStatus().getDbValue());
            ps.setString(5, r.getVerificationStatus() == null
                    ? VerificationStatus.Pending.name() : r.getVerificationStatus().name());
            if (r.getVerifiedBy() != null) ps.setInt(6, r.getVerifiedBy());
            else                           ps.setNull(6, Types.INTEGER);
            if (r.getVerifiedAt() != null) ps.setTimestamp(7, Timestamp.valueOf(r.getVerifiedAt()));
            else                           ps.setNull(7, Types.TIMESTAMP);
            ps.setString(8, r.getNote());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } finally {
            close(conn, ps, null);
        }
        return -1;
    }

    public boolean existsByEmployeeAndDate(int employeeId, LocalDate workDate) throws SQLException {
        String sql = "SELECT 1 FROM attendance_records WHERE employee_id = ? AND work_date = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, employeeId);
            ps.setDate(2, Date.valueOf(workDate));
            rs = ps.executeQuery();
            return rs.next();
        } finally {
            close(conn, ps, rs);
        }
    }

    public AttendanceRecord findById(int attendanceId) throws SQLException {
        String sql = "SELECT a.*, u.full_name AS emp_full_name, e.employee_code, "
                   + "       u.manager_id AS emp_manager_user_id, "
                   + "       vu.full_name AS verified_by_name "
                   + "FROM attendance_records a "
                   + "JOIN employees e   ON a.employee_id = e.employee_id "
                   + "JOIN users u       ON e.user_id     = u.user_id "
                   + "LEFT JOIN users vu ON a.verified_by = vu.user_id "
                   + "WHERE a.attendance_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, attendanceId);
            rs = ps.executeQuery();
            if (rs.next()) {
                AttendanceRecord r = mapRow(rs);
                int mgrId = rs.getInt("emp_manager_user_id");
                r.setEmployeeManagerUserId(rs.wasNull() ? null : mgrId);
                return r;
            }
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    public boolean update(AttendanceRecord r) throws SQLException {
        String sql = "UPDATE attendance_records SET work_date=?, "
                   + "overtime_hours=?, attendance_status=?, note=?, "
                   + "updated_at=NOW() "
                   + "WHERE attendance_id=?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setDate(1, Date.valueOf(r.getWorkDate()));
            ps.setBigDecimal(2, r.getOvertimeHours() != null ? r.getOvertimeHours() : java.math.BigDecimal.ZERO);
            ps.setString(3, r.getAttendanceStatus().getDbValue());
            ps.setString(4, r.getNote());
            ps.setInt(5, r.getAttendanceId());
            return ps.executeUpdate() > 0;
        } finally {
            close(conn, ps, null);
        }
    }

    public boolean deleteById(int attendanceId) throws SQLException {
        String sql = "DELETE FROM attendance_records WHERE attendance_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, attendanceId);
            return ps.executeUpdate() > 0;
        } finally {
            close(conn, ps, null);
        }
    }

    public boolean verify(int attendanceId, int verifierUserId) throws SQLException {
        String sql = "UPDATE attendance_records SET verification_status='Verified', "
                   + "verified_by=?, verified_at=NOW(), updated_at=NOW() "
                   + "WHERE attendance_id=?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, verifierUserId);
            ps.setInt(2, attendanceId);
            return ps.executeUpdate() > 0;
        } finally {
            close(conn, ps, null);
        }
    }

    /**
     * Bulk-verify every Pending record belonging to employees managed by the
     * given manager (optionally limited to a date range). Returns the number of
     * records updated. Used by "Send to HR Staff".
     */
    public int verifyAllPendingByManager(int managerUserId, int verifierUserId,
                                         LocalDate fromDate, LocalDate toDate) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "UPDATE attendance_records ar "
              + "JOIN employees e ON ar.employee_id = e.employee_id "
              + "JOIN users u     ON e.user_id      = u.user_id "
              + "SET ar.verification_status='Verified', ar.verified_by=?, "
              + "    ar.verified_at=NOW(), ar.updated_at=NOW() "
              + "WHERE u.manager_id=? AND ar.verification_status='Pending'");
        if (fromDate != null) sql.append(" AND ar.work_date >= ?");
        if (toDate   != null) sql.append(" AND ar.work_date <= ?");

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            int i = 1;
            ps.setInt(i++, verifierUserId);
            ps.setInt(i++, managerUserId);
            if (fromDate != null) ps.setDate(i++, Date.valueOf(fromDate));
            if (toDate   != null) ps.setDate(i++, Date.valueOf(toDate));
            return ps.executeUpdate();
        } finally {
            close(conn, ps, null);
        }
    }

    /** Count Pending records for employees managed by the given manager. */
    public int countPendingByManager(int managerUserId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM attendance_records ar "
                   + "JOIN employees e ON ar.employee_id = e.employee_id "
                   + "JOIN users u     ON e.user_id      = u.user_id "
                   + "WHERE u.manager_id=? AND ar.verification_status='Pending'";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, managerUserId);
            rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } finally {
            close(conn, ps, rs);
        }
    }

    public int countPendingByManager(int managerUserId, LocalDate fromDate, LocalDate toDate)
            throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM attendance_records ar "
              + "JOIN employees e ON ar.employee_id = e.employee_id "
              + "JOIN users u     ON e.user_id      = u.user_id "
              + "WHERE u.manager_id=? AND ar.verification_status='Pending'");
        if (fromDate != null) sql.append(" AND ar.work_date >= ?");
        if (toDate != null) sql.append(" AND ar.work_date <= ?");

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            int i = 1;
            ps.setInt(i++, managerUserId);
            if (fromDate != null) ps.setDate(i++, Date.valueOf(fromDate));
            if (toDate != null) ps.setDate(i++, Date.valueOf(toDate));
            rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } finally {
            close(conn, ps, rs);
        }
    }

    public YearMonth findLatestMonth(Integer managerUserId, Integer departmentId) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT MAX(a.work_date) AS latest_date "
              + "FROM attendance_records a "
              + "JOIN employees e ON a.employee_id = e.employee_id "
              + "JOIN users u ON e.user_id = u.user_id "
              + "WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (managerUserId != null) { sql.append("AND u.manager_id = ? "); params.add(managerUserId); }
        if (departmentId != null) { sql.append("AND e.department_id = ? "); params.add(departmentId); }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            rs = ps.executeQuery();
            if (rs.next()) {
                Date latest = rs.getDate("latest_date");
                if (latest != null) {
                    LocalDate d = latest.toLocalDate();
                    return YearMonth.of(d.getYear(), d.getMonthValue());
                }
            }
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    /** Monthly attendance aggregate for one employee (used to build a report). */
    public static final class MonthlySummary {
        public int employeeId;
        public int departmentId;
        public int actualWorkingDays;   // Present + Late + paid Leave
        public int paidLeaveDays;       // Leave
        public int unpaidLeaveDays;     // Unpaid Leave
        public int maternityLeaveDays;  // Maternity Leave
        public java.math.BigDecimal overtimeHours = java.math.BigDecimal.ZERO;
    }

    /** Pending attendance group that is old enough for automatic manager confirmation. */
    public static final class AutoConfirmBatch {
        public int managerUserId;
        public int year;
        public int month;
        public int pendingCount;
        public LocalDateTime latestCreatedAt;
    }

    public List<AutoConfirmBatch> findAutoConfirmBatches(LocalDateTime cutoff)
            throws SQLException {
        String sql =
            "SELECT u.manager_id AS manager_user_id, "
          + "       YEAR(ar.work_date) AS report_year, "
          + "       MONTH(ar.work_date) AS report_month, "
          + "       COUNT(*) AS pending_count, "
          + "       MAX(ar.created_at) AS latest_created_at "
          + "FROM attendance_records ar "
          + "JOIN employees e ON ar.employee_id = e.employee_id "
          + "JOIN users u ON e.user_id = u.user_id "
          + "JOIN roles ro ON u.role_id = ro.role_id "
          + "WHERE u.manager_id IS NOT NULL "
          + "  AND ro.role_name = 'EMPLOYEE' "
          + "  AND ar.verification_status = 'Pending' "
          + "GROUP BY u.manager_id, YEAR(ar.work_date), MONTH(ar.work_date) "
          + "HAVING MAX(ar.created_at) <= ? "
          + "ORDER BY report_year, report_month, manager_user_id";

        List<AutoConfirmBatch> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, Timestamp.valueOf(cutoff));
            rs = ps.executeQuery();
            while (rs.next()) {
                AutoConfirmBatch batch = new AutoConfirmBatch();
                batch.managerUserId = rs.getInt("manager_user_id");
                batch.year = rs.getInt("report_year");
                batch.month = rs.getInt("report_month");
                batch.pendingCount = rs.getInt("pending_count");
                Timestamp latest = rs.getTimestamp("latest_created_at");
                if (latest != null) batch.latestCreatedAt = latest.toLocalDateTime();
                list.add(batch);
            }
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public int autoVerifyPendingByManagerMonth(int managerUserId, int year, int month)
            throws SQLException {
        String sql =
            "UPDATE attendance_records ar "
          + "JOIN employees e ON ar.employee_id = e.employee_id "
          + "JOIN users u ON e.user_id = u.user_id "
          + "JOIN roles ro ON u.role_id = ro.role_id "
          + "SET ar.verification_status = 'Verified', "
          + "    ar.verified_by = NULL, "
          + "    ar.verified_at = NOW(), "
          + "    ar.note = LEFT(CONCAT("
          + "        CASE WHEN COALESCE(ar.note, '') = '' THEN '' ELSE CONCAT(ar.note, ' | ') END, "
          + "        'Auto-confirmed after 2 days'), 255), "
          + "    ar.updated_at = NOW() "
          + "WHERE u.manager_id = ? "
          + "  AND ro.role_name = 'EMPLOYEE' "
          + "  AND ar.verification_status = 'Pending' "
          + "  AND YEAR(ar.work_date) = ? "
          + "  AND MONTH(ar.work_date) = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, managerUserId);
            ps.setInt(2, year);
            ps.setInt(3, month);
            return ps.executeUpdate();
        } finally {
            close(conn, ps, null);
        }
    }

    /** Per-employee attendance summary for the card view. */
    public static final class EmployeeAttSummary {
        public int employeeId;
        public String employeeCode;
        public String fullName;
        public String departmentName;
        public int departmentId;
        public int totalRecords;
        public int pendingCount;
        public int verifiedCount;
        public int presentDays;
        public int absentDays;
        public int leaveDays;

        public int getEmployeeId() { return employeeId; }
        public String getEmployeeCode() { return employeeCode; }
        public String getFullName() { return fullName; }
        public String getDepartmentName() { return departmentName; }
        public int getDepartmentId() { return departmentId; }
        public int getTotalRecords() { return totalRecords; }
        public int getPendingCount() { return pendingCount; }
        public int getVerifiedCount() { return verifiedCount; }
        public int getPresentDays() { return presentDays; }
        public int getAbsentDays() { return absentDays; }
        public int getLeaveDays() { return leaveDays; }
    }

    /**
     * Employee attendance card data for a manager's team.
     * Returns one summary row per active employee, even when the date range has no records yet.
     */
    public List<EmployeeAttSummary> summaryByManager(int managerUserId,
                                                      LocalDate fromDate, LocalDate toDate) throws SQLException {
        return summaryByScope(managerUserId, null, fromDate, toDate);
    }

    /**
     * Employee attendance card data for all employees in a department.
     */
    public List<EmployeeAttSummary> summaryByDepartment(int departmentId,
                                                         LocalDate fromDate, LocalDate toDate) throws SQLException {
        return summaryByScope(null, departmentId, fromDate, toDate);
    }

    private List<EmployeeAttSummary> summaryByScope(Integer managerUserId, Integer departmentId,
                                                    LocalDate fromDate, LocalDate toDate) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT e.employee_id, e.employee_code, u.full_name, d.department_name, e.department_id, "
          + " COALESCE(COUNT(a.attendance_id), 0) AS total_records, "
          + " COALESCE(SUM(CASE WHEN a.verification_status='Pending' THEN 1 ELSE 0 END), 0) AS pending_cnt, "
          + " COALESCE(SUM(CASE WHEN a.verification_status='Verified' THEN 1 ELSE 0 END), 0) AS verified_cnt, "
          + " COALESCE(SUM(CASE WHEN a.attendance_status IN ('Present','Late') THEN 1 ELSE 0 END), 0) AS present_days, "
          + " COALESCE(SUM(CASE WHEN a.attendance_status='Absent' THEN 1 ELSE 0 END), 0) AS absent_days, "
          + " COALESCE(SUM(CASE WHEN a.attendance_status='Leave' THEN 1 ELSE 0 END), 0) AS leave_days "
          + "FROM employees e "
          + "JOIN users u       ON e.user_id       = u.user_id "
          + "JOIN roles ro      ON u.role_id       = ro.role_id "
          + "JOIN departments d ON e.department_id = d.department_id "
          + "LEFT JOIN attendance_records a ON a.employee_id = e.employee_id ");
        List<Object> params = new ArrayList<>();
        if (fromDate != null) { sql.append("AND a.work_date >= ? "); params.add(Date.valueOf(fromDate)); }
        if (toDate != null)   { sql.append("AND a.work_date <= ? "); params.add(Date.valueOf(toDate)); }
        sql.append("WHERE u.is_active = 1 AND ro.role_name = 'EMPLOYEE' ");
        if (managerUserId != null) { sql.append("AND u.manager_id = ? "); params.add(managerUserId); }
        if (departmentId != null) { sql.append("AND e.department_id = ? "); params.add(departmentId); }
        sql.append("GROUP BY e.employee_id, e.employee_code, u.full_name, d.department_name, e.department_id ");
        sql.append("ORDER BY u.full_name");

        List<EmployeeAttSummary> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            rs = ps.executeQuery();
            while (rs.next()) {
                EmployeeAttSummary s = new EmployeeAttSummary();
                s.employeeId    = rs.getInt("employee_id");
                s.employeeCode  = rs.getString("employee_code");
                s.fullName      = rs.getString("full_name");
                s.departmentName = rs.getString("department_name");
                s.departmentId  = rs.getInt("department_id");
                s.totalRecords  = rs.getInt("total_records");
                s.pendingCount  = rs.getInt("pending_cnt");
                s.verifiedCount = rs.getInt("verified_cnt");
                s.presentDays   = rs.getInt("present_days");
                s.absentDays    = rs.getInt("absent_days");
                s.leaveDays     = rs.getInt("leave_days");
                list.add(s);
            }
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    /**
     * Aggregate verified attendance for every employee managed by {@code managerUserId}
     * in the given month. Returns one MonthlySummary per employee that has at least
     * one verified record in that month.
     */
    public List<MonthlySummary> aggregateMonthByManager(int managerUserId,
                                                        int year, int month) throws SQLException {
        String sql =
            "SELECT ar.employee_id, e.department_id, "
          + "  SUM(CASE WHEN ar.attendance_status IN ('Present','Late','Leave') THEN 1 ELSE 0 END) AS work_days, "
          + "  SUM(CASE WHEN ar.attendance_status='Leave' THEN 1 ELSE 0 END) AS paid_leave, "
          + "  SUM(CASE WHEN ar.attendance_status='Unpaid Leave' THEN 1 ELSE 0 END) AS unpaid_leave, "
          + "  SUM(CASE WHEN ar.attendance_status='Maternity Leave' THEN 1 ELSE 0 END) AS maternity_leave, "
          + "  COALESCE(SUM(ar.overtime_hours),0) AS ot_hours "
          + "FROM attendance_records ar "
          + "JOIN employees e ON ar.employee_id = e.employee_id "
          + "JOIN users u     ON e.user_id      = u.user_id "
          + "WHERE u.manager_id=? AND ar.verification_status='Verified' "
          + "  AND YEAR(ar.work_date)=? AND MONTH(ar.work_date)=? "
          + "GROUP BY ar.employee_id, e.department_id";
        List<MonthlySummary> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, managerUserId);
            ps.setInt(2, year);
            ps.setInt(3, month);
            rs = ps.executeQuery();
            while (rs.next()) {
                MonthlySummary s = new MonthlySummary();
                s.employeeId      = rs.getInt("employee_id");
                s.departmentId    = rs.getInt("department_id");
                s.actualWorkingDays = rs.getInt("work_days");
                s.paidLeaveDays   = rs.getInt("paid_leave");
                s.unpaidLeaveDays = rs.getInt("unpaid_leave");
                s.maternityLeaveDays = rs.getInt("maternity_leave");
                s.overtimeHours   = rs.getBigDecimal("ot_hours");
                list.add(s);
            }
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public List<AttendanceRecord> findByManagerScope(int managerUserId,
                                                     Integer employeeIdFilter,
                                                     LocalDate fromDate,
                                                     LocalDate toDate) throws SQLException {
        return findByScope(managerUserId, null, employeeIdFilter, fromDate, toDate, -1, -1);
    }

    public List<AttendanceRecord> findByEmployeeId(int employeeId,
                                                   LocalDate fromDate,
                                                   LocalDate toDate) throws SQLException {
        return findByScope(null, employeeId, null, fromDate, toDate, -1, -1);
    }

    public List<AttendanceRecord> findVerifiedByEmployeeMonth(int employeeId,
                                                              int year,
                                                              int month) throws SQLException {
        LocalDate fromDate = LocalDate.of(year, month, 1);
        LocalDate toDate = fromDate.withDayOfMonth(fromDate.lengthOfMonth());
        String sql =
            "SELECT a.*, u.full_name AS emp_full_name, e.employee_code, "
          + "       vu.full_name AS verified_by_name "
          + "FROM attendance_records a "
          + "JOIN employees e   ON a.employee_id = e.employee_id "
          + "JOIN users u       ON e.user_id     = u.user_id "
          + "LEFT JOIN users vu ON a.verified_by = vu.user_id "
          + "WHERE a.employee_id = ? "
          + "  AND a.verification_status = 'Verified' "
          + "  AND a.work_date >= ? AND a.work_date <= ? "
          + "ORDER BY a.work_date";

        List<AttendanceRecord> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, employeeId);
            ps.setDate(2, Date.valueOf(fromDate));
            ps.setDate(3, Date.valueOf(toDate));
            rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public List<AttendanceRecord> findAll(Integer employeeIdFilter,
                                          LocalDate fromDate,
                                          LocalDate toDate) throws SQLException {
        return findByScope(null, null, employeeIdFilter, fromDate, toDate, -1, -1);
    }

    public List<AttendanceRecord> findByManagerScope(int managerUserId,
                                                     Integer employeeIdFilter,
                                                     LocalDate fromDate,
                                                     LocalDate toDate,
                                                     int offset, int limit) throws SQLException {
        return findByScope(managerUserId, null, employeeIdFilter, fromDate, toDate, offset, limit);
    }

    public List<AttendanceRecord> findByEmployeeId(int employeeId,
                                                   LocalDate fromDate,
                                                   LocalDate toDate,
                                                   int offset, int limit) throws SQLException {
        return findByScope(null, employeeId, null, fromDate, toDate, offset, limit);
    }

    public List<AttendanceRecord> findAll(Integer employeeIdFilter,
                                          LocalDate fromDate,
                                          LocalDate toDate,
                                          int offset, int limit) throws SQLException {
        return findByScope(null, null, employeeIdFilter, fromDate, toDate, offset, limit);
    }

    public int countByManagerScope(int managerUserId, Integer employeeIdFilter,
                                   LocalDate fromDate, LocalDate toDate) throws SQLException {
        return countByScope(managerUserId, null, employeeIdFilter, fromDate, toDate);
    }

    public int countByEmployeeId(int employeeId, LocalDate fromDate, LocalDate toDate) throws SQLException {
        return countByScope(null, employeeId, null, fromDate, toDate);
    }

    public int countAll(Integer employeeIdFilter, LocalDate fromDate, LocalDate toDate) throws SQLException {
        return countByScope(null, null, employeeIdFilter, fromDate, toDate);
    }

    private StringBuilder buildScopeWhere(Integer managerUserId, Integer ownEmployeeId,
                                          Integer employeeIdFilter, LocalDate fromDate,
                                          LocalDate toDate, List<Object> params) {
        StringBuilder where = new StringBuilder("WHERE 1=1 ");
        if (managerUserId != null) { where.append("AND u.manager_id = ? "); params.add(managerUserId); }
        if (ownEmployeeId != null) { where.append("AND a.employee_id = ? "); params.add(ownEmployeeId); }
        if (employeeIdFilter != null) { where.append("AND a.employee_id = ? "); params.add(employeeIdFilter); }
        if (fromDate != null) { where.append("AND a.work_date >= ? "); params.add(Date.valueOf(fromDate)); }
        if (toDate != null) { where.append("AND a.work_date <= ? "); params.add(Date.valueOf(toDate)); }
        return where;
    }

    private int countByScope(Integer managerUserId, Integer ownEmployeeId,
                             Integer employeeIdFilter, LocalDate fromDate,
                             LocalDate toDate) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM attendance_records a "
                   + "JOIN employees e ON a.employee_id = e.employee_id "
                   + "JOIN users u ON e.user_id = u.user_id "
                   + buildScopeWhere(managerUserId, ownEmployeeId, employeeIdFilter, fromDate, toDate, params);
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }

    private List<AttendanceRecord> findByScope(Integer managerUserId,
                                               Integer ownEmployeeId,
                                               Integer employeeIdFilter,
                                               LocalDate fromDate,
                                               LocalDate toDate,
                                               int offset, int limit) throws SQLException {
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder()
            .append("SELECT a.*, u.full_name AS emp_full_name, e.employee_code, ")
            .append("       vu.full_name AS verified_by_name ")
            .append("FROM attendance_records a ")
            .append("JOIN employees e   ON a.employee_id = e.employee_id ")
            .append("JOIN users u       ON e.user_id     = u.user_id ")
            .append("LEFT JOIN users vu ON a.verified_by = vu.user_id ")
            .append(buildScopeWhere(managerUserId, ownEmployeeId, employeeIdFilter, fromDate, toDate, params))
            .append("ORDER BY a.work_date DESC, u.full_name");

        if (limit >= 0) {
            sql.append(" LIMIT ? OFFSET ?");
            params.add(limit);
            params.add(offset);
        }

        List<AttendanceRecord> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    private AttendanceRecord mapRow(ResultSet rs) throws SQLException {
        AttendanceRecord r = new AttendanceRecord();
        r.setAttendanceId(rs.getInt("attendance_id"));
        r.setEmployeeId(rs.getInt("employee_id"));
        Date wd = rs.getDate("work_date");
        if (wd != null) r.setWorkDate(wd.toLocalDate());
        r.setOvertimeHours(rs.getBigDecimal("overtime_hours"));
        r.setAttendanceStatus(AttendanceStatus.fromDb(rs.getString("attendance_status")));
        String vs = rs.getString("verification_status");
        if (vs != null) {
            try { r.setVerificationStatus(VerificationStatus.valueOf(vs)); }
            catch (IllegalArgumentException ignored) {}
        }
        int verifiedBy = rs.getInt("verified_by");
        if (!rs.wasNull()) r.setVerifiedBy(verifiedBy);
        Timestamp verifiedAt = rs.getTimestamp("verified_at");
        if (verifiedAt != null) r.setVerifiedAt(verifiedAt.toLocalDateTime());
        r.setNote(rs.getString("note"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) r.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) r.setUpdatedAt(updated.toLocalDateTime());
        try { r.setEmployeeFullName(rs.getString("emp_full_name")); } catch (SQLException ignored) {}
        try { r.setEmployeeCode(rs.getString("employee_code")); } catch (SQLException ignored) {}
        try { r.setVerifiedByFullName(rs.getString("verified_by_name")); } catch (SQLException ignored) {}
        return r;
    }

    private void close(Connection conn, PreparedStatement ps, ResultSet rs) {
        if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
        if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
        DBContext.closeConnection(conn);
    }
}
