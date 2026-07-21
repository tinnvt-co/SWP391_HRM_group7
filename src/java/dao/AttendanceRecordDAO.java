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

    private static final String ATTENDANCE_ROLE_FILTER =
            "ro.role_name IN ('EMPLOYEE', 'MANAGER', 'HR_STAFF', 'HR_MANAGER') AND d.department_code <> 'IT'";
    private static final String MANAGER_CONFIRM_ROLE_FILTER =
            "ro.role_name IN ('EMPLOYEE', 'MANAGER', 'HR_STAFF', 'HR_MANAGER')";

    public int insert(AttendanceRecord r) throws SQLException {
        String sql = "INSERT INTO attendance_records (employee_id, work_date, "
                   + "check_in_time, check_out_time, late_minutes, late_penalty_amount, "
                   + "overtime_hours, attendance_status, verification_status, "
                   + "verified_by, verified_at, note) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, r.getEmployeeId());
            ps.setDate(2, Date.valueOf(r.getWorkDate()));
            if (r.getCheckInTime() != null) ps.setTime(3, Time.valueOf(r.getCheckInTime()));
            else                            ps.setNull(3, Types.TIME);
            if (r.getCheckOutTime() != null) ps.setTime(4, Time.valueOf(r.getCheckOutTime()));
            else                             ps.setNull(4, Types.TIME);
            ps.setInt(5, r.getLateMinutes());
            ps.setBigDecimal(6, r.getLatePenaltyAmount() != null ? r.getLatePenaltyAmount() : java.math.BigDecimal.ZERO);
            ps.setBigDecimal(7, r.getOvertimeHours() != null ? r.getOvertimeHours() : java.math.BigDecimal.ZERO);
            ps.setString(8, r.getAttendanceStatus().getDbValue());
            ps.setString(9, r.getVerificationStatus() == null
                    ? VerificationStatus.Pending.name() : r.getVerificationStatus().name());
            if (r.getVerifiedBy() != null) ps.setInt(10, r.getVerifiedBy());
            else                           ps.setNull(10, Types.INTEGER);
            if (r.getVerifiedAt() != null) ps.setTimestamp(11, Timestamp.valueOf(r.getVerifiedAt()));
            else                           ps.setNull(11, Types.TIMESTAMP);
            ps.setString(12, r.getNote());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } finally {
            close(conn, ps, null);
        }
        return -1;
    }

    public boolean insertIfAbsent(AttendanceRecord r) throws SQLException {
        String sql = "INSERT IGNORE INTO attendance_records (employee_id, work_date, "
                   + "check_in_time, check_out_time, late_minutes, late_penalty_amount, "
                   + "overtime_hours, attendance_status, verification_status, "
                   + "verified_by, verified_at, note) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            bindAttendanceValues(ps, r);
            return ps.executeUpdate() > 0;
        } finally {
            close(conn, ps, null);
        }
    }

    public boolean upsertImported(AttendanceRecord r) throws SQLException {
        String sql = "INSERT INTO attendance_records (employee_id, work_date, "
                   + "check_in_time, check_out_time, late_minutes, late_penalty_amount, "
                   + "overtime_hours, attendance_status, verification_status, "
                   + "verified_by, verified_at, note) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                   + "ON DUPLICATE KEY UPDATE "
                   + "check_in_time=VALUES(check_in_time), "
                   + "check_out_time=VALUES(check_out_time), "
                   + "late_minutes=VALUES(late_minutes), "
                   + "late_penalty_amount=VALUES(late_penalty_amount), "
                   + "overtime_hours=VALUES(overtime_hours), "
                   + "attendance_status=VALUES(attendance_status), "
                   + "verification_status=VALUES(verification_status), "
                   + "verified_by=NULL, verified_at=NULL, "
                   + "note=VALUES(note), updated_at=NOW()";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            bindAttendanceValues(ps, r);
            return ps.executeUpdate() > 0;
        } finally {
            close(conn, ps, null);
        }
    }

    private void bindAttendanceValues(PreparedStatement ps, AttendanceRecord r)
            throws SQLException {
        ps.setInt(1, r.getEmployeeId());
        ps.setDate(2, Date.valueOf(r.getWorkDate()));
        if (r.getCheckInTime() != null) ps.setTime(3, Time.valueOf(r.getCheckInTime()));
        else                            ps.setNull(3, Types.TIME);
        if (r.getCheckOutTime() != null) ps.setTime(4, Time.valueOf(r.getCheckOutTime()));
        else                             ps.setNull(4, Types.TIME);
        ps.setInt(5, r.getLateMinutes());
        ps.setBigDecimal(6, r.getLatePenaltyAmount() != null
                ? r.getLatePenaltyAmount() : java.math.BigDecimal.ZERO);
        ps.setBigDecimal(7, r.getOvertimeHours() != null
                ? r.getOvertimeHours() : java.math.BigDecimal.ZERO);
        ps.setString(8, r.getAttendanceStatus().getDbValue());
        ps.setString(9, r.getVerificationStatus() == null
                ? VerificationStatus.Pending.name() : r.getVerificationStatus().name());
        if (r.getVerifiedBy() != null) ps.setInt(10, r.getVerifiedBy());
        else                           ps.setNull(10, Types.INTEGER);
        if (r.getVerifiedAt() != null) ps.setTimestamp(11, Timestamp.valueOf(r.getVerifiedAt()));
        else                           ps.setNull(11, Types.TIMESTAMP);
        ps.setString(12, r.getNote());
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
                   + "check_in_time=?, check_out_time=?, late_minutes=?, late_penalty_amount=?, "
                   + "overtime_hours=?, attendance_status=?, note=?, "
                   + "updated_at=NOW() "
                   + "WHERE attendance_id=?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setDate(1, Date.valueOf(r.getWorkDate()));
            if (r.getCheckInTime() != null) ps.setTime(2, Time.valueOf(r.getCheckInTime()));
            else                            ps.setNull(2, Types.TIME);
            if (r.getCheckOutTime() != null) ps.setTime(3, Time.valueOf(r.getCheckOutTime()));
            else                             ps.setNull(3, Types.TIME);
            ps.setInt(4, r.getLateMinutes());
            ps.setBigDecimal(5, r.getLatePenaltyAmount() != null ? r.getLatePenaltyAmount() : java.math.BigDecimal.ZERO);
            ps.setBigDecimal(6, r.getOvertimeHours() != null ? r.getOvertimeHours() : java.math.BigDecimal.ZERO);
            ps.setString(7, r.getAttendanceStatus().getDbValue());
            ps.setString(8, r.getNote());
            ps.setInt(9, r.getAttendanceId());
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
     * Bulk-verify every Pending employee and manager record in the department
     * headed by the given manager.
     */
    public int verifyAllPendingByManager(int managerUserId, int verifierUserId,
                                         LocalDate fromDate, LocalDate toDate) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "UPDATE attendance_records ar "
              + "JOIN employees e ON ar.employee_id = e.employee_id "
              + "JOIN users u     ON e.user_id      = u.user_id "
              + "JOIN roles ro    ON u.role_id      = ro.role_id "
              + "JOIN departments d ON e.department_id = d.department_id "
              + "SET ar.verification_status='Verified', ar.verified_by=?, "
              + "    ar.verified_at=NOW(), ar.updated_at=NOW() "
              + "WHERE d.manager_id=? AND " + MANAGER_CONFIRM_ROLE_FILTER + " "
              + "AND d.department_code <> 'IT' AND ar.verification_status='Pending'");
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

    /** Count Pending employee and manager records in the manager's department. */
    public int countPendingByManager(int managerUserId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM attendance_records ar "
                   + "JOIN employees e ON ar.employee_id = e.employee_id "
                   + "JOIN users u     ON e.user_id      = u.user_id "
                   + "JOIN roles ro    ON u.role_id      = ro.role_id "
                   + "JOIN departments d ON e.department_id = d.department_id "
                   + "WHERE d.manager_id=? AND " + MANAGER_CONFIRM_ROLE_FILTER + " "
                   + "AND d.department_code <> 'IT' AND ar.verification_status='Pending'";
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
              + "JOIN roles ro    ON u.role_id      = ro.role_id "
              + "JOIN departments d ON e.department_id = d.department_id "
              + "WHERE d.manager_id=? AND " + MANAGER_CONFIRM_ROLE_FILTER + " "
              + "AND d.department_code <> 'IT' AND ar.verification_status='Pending'");
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
              + "JOIN roles ro ON u.role_id = ro.role_id "
              + "JOIN departments d ON e.department_id = d.department_id "
              + "WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (managerUserId != null) {
            sql.append("AND d.manager_id = ? AND ").append(MANAGER_CONFIRM_ROLE_FILTER).append(" ");
            params.add(managerUserId);
        }
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
        public java.math.BigDecimal latePenaltyAmount = java.math.BigDecimal.ZERO;
    }

    /** Pending attendance group that is old enough for automatic manager confirmation. */
    public static final class AutoConfirmBatch {
        public int managerUserId;
        public int departmentId;
        public String departmentCode;
        public int year;
        public int month;
        public int pendingCount;
        public LocalDateTime latestCreatedAt;
    }

    public List<AutoConfirmBatch> findAutoConfirmBatches(LocalDateTime cutoff)
            throws SQLException {
        String sql =
            "SELECT d.manager_id AS manager_user_id, e.department_id, d.department_code, "
          + "       YEAR(ar.work_date) AS report_year, "
          + "       MONTH(ar.work_date) AS report_month, "
          + "       COUNT(*) AS pending_count, "
          + "       MAX(ar.created_at) AS latest_created_at "
          + "FROM attendance_records ar "
          + "JOIN employees e ON ar.employee_id = e.employee_id "
          + "JOIN users u ON e.user_id = u.user_id "
          + "JOIN roles ro ON u.role_id = ro.role_id "
          + "JOIN departments d ON e.department_id = d.department_id "
          + "WHERE d.manager_id IS NOT NULL "
          + "  AND " + ATTENDANCE_ROLE_FILTER + " "
          + "  AND ar.verification_status = 'Pending' "
          + "GROUP BY d.manager_id, e.department_id, d.department_code, "
          + "         YEAR(ar.work_date), MONTH(ar.work_date) "
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
                batch.departmentId = rs.getInt("department_id");
                batch.departmentCode = rs.getString("department_code");
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

    public static final class DepartmentMonthTask {
        public int departmentId;
        public String departmentName;
        public int year;
        public int month;
        public int pendingRecordCount;
    }

    public List<DepartmentMonthTask> findPendingDepartmentMonthsByCode(String departmentCode)
            throws SQLException {
        String sql =
            "SELECT d.department_id, d.department_name, "
          + "       YEAR(ar.work_date) AS task_year, MONTH(ar.work_date) AS task_month, "
          + "       SUM(CASE WHEN ar.verification_status='Pending' THEN 1 ELSE 0 END) "
          + "           AS pending_record_count "
          + "FROM attendance_records ar "
          + "JOIN employees e ON ar.employee_id=e.employee_id "
          + "JOIN users u ON e.user_id=u.user_id "
          + "JOIN roles ro ON u.role_id=ro.role_id "
          + "JOIN departments d ON e.department_id=d.department_id "
          + "WHERE d.department_code=? AND d.is_active=1 "
          + "  AND " + ATTENDANCE_ROLE_FILTER + " "
          + "  AND NOT EXISTS ("
          + "      SELECT 1 FROM attendance_reports approved_report "
          + "      WHERE approved_report.department_id=d.department_id "
          + "        AND approved_report.report_year=YEAR(ar.work_date) "
          + "        AND approved_report.report_month=MONTH(ar.work_date) "
          + "        AND approved_report.status='Approved By HR Manager'"
          + "  ) "
          + "GROUP BY d.department_id, d.department_name, "
          + "         YEAR(ar.work_date), MONTH(ar.work_date) "
          + "ORDER BY task_year, task_month";

        List<DepartmentMonthTask> tasks = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, departmentCode);
            rs = ps.executeQuery();
            while (rs.next()) {
                DepartmentMonthTask task = new DepartmentMonthTask();
                task.departmentId = rs.getInt("department_id");
                task.departmentName = rs.getString("department_name");
                task.year = rs.getInt("task_year");
                task.month = rs.getInt("task_month");
                task.pendingRecordCount = rs.getInt("pending_record_count");
                tasks.add(task);
            }
        } finally {
            close(conn, ps, rs);
        }
        return tasks;
    }

    public int autoVerifyPendingByManagerMonth(int managerUserId, int departmentId,
                                               int year, int month)
            throws SQLException {
        String sql =
            "UPDATE attendance_records ar "
          + "JOIN employees e ON ar.employee_id = e.employee_id "
          + "JOIN users u ON e.user_id = u.user_id "
          + "JOIN roles ro ON u.role_id = ro.role_id "
          + "JOIN departments d ON e.department_id = d.department_id "
          + "SET ar.verification_status = 'Verified', "
          + "    ar.verified_by = NULL, "
          + "    ar.verified_at = NOW(), "
          + "    ar.note = LEFT(CONCAT("
          + "        CASE WHEN COALESCE(ar.note, '') = '' THEN '' ELSE CONCAT(ar.note, ' | ') END, "
          + "        'Auto-confirmed after 2 days'), 255), "
          + "    ar.updated_at = NOW() "
          + "WHERE d.manager_id = ? "
          + "  AND e.department_id = ? "
          + "  AND " + ATTENDANCE_ROLE_FILTER + " "
          + "  AND ar.verification_status = 'Pending' "
          + "  AND YEAR(ar.work_date) = ? "
          + "  AND MONTH(ar.work_date) = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, managerUserId);
            ps.setInt(2, departmentId);
            ps.setInt(3, year);
            ps.setInt(4, month);
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
        sql.append("WHERE u.is_active = 1 ");
        if (managerUserId != null) {
            sql.append("AND ").append(MANAGER_CONFIRM_ROLE_FILTER)
                    .append(" AND d.department_code <> 'IT' ");
            sql.append("AND d.manager_id = ? ");
            params.add(managerUserId);
        } else {
            sql.append("AND ").append(ATTENDANCE_ROLE_FILTER).append(" ");
        }
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
     * Aggregate verified attendance for employees and the manager in the
     * department headed by {@code managerUserId}.
     */
    public List<MonthlySummary> aggregateMonthByManager(int managerUserId,
                                                        int year, int month) throws SQLException {
        String sql =
            "SELECT ar.employee_id, e.department_id, "
          + "  SUM(CASE WHEN ar.attendance_status IN ('Present','Late','Leave') "
          + "           OR (ar.attendance_status='Holiday' AND hd.holiday_date IS NOT NULL) "
          + "      THEN 1 ELSE 0 END) AS work_days, "
          + "  SUM(CASE WHEN ar.attendance_status='Leave' THEN 1 ELSE 0 END) AS paid_leave, "
          + "  SUM(CASE WHEN ar.attendance_status='Unpaid Leave' THEN 1 ELSE 0 END) AS unpaid_leave, "
          + "  SUM(CASE WHEN ar.attendance_status='Maternity Leave' THEN 1 ELSE 0 END) AS maternity_leave, "
          + "  COALESCE(SUM(ar.overtime_hours),0) AS ot_hours, "
          + "  COALESCE(SUM(ar.late_penalty_amount),0) AS late_penalty_amount "
          + "FROM attendance_records ar "
          + "JOIN employees e ON ar.employee_id = e.employee_id "
          + "JOIN users u     ON e.user_id      = u.user_id "
          + "JOIN roles ro    ON u.role_id      = ro.role_id "
          + "JOIN departments d ON e.department_id = d.department_id "
          + "LEFT JOIN holiday_dates hd ON hd.holiday_date = ar.work_date AND hd.is_active = 1 "
          + "WHERE d.manager_id=? AND ar.verification_status='Verified' "
          + "  AND " + MANAGER_CONFIRM_ROLE_FILTER + " "
          + "  AND d.department_code <> 'IT' "
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
                s.latePenaltyAmount = rs.getBigDecimal("late_penalty_amount");
                list.add(s);
            }
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public int countPendingByDepartmentMonth(int year, int month, int departmentId)
            throws SQLException {
        String sql =
            "SELECT COUNT(*) "
          + "FROM attendance_records ar "
          + "JOIN employees e ON ar.employee_id = e.employee_id "
          + "JOIN users u ON e.user_id = u.user_id "
          + "JOIN roles ro ON u.role_id = ro.role_id "
          + "JOIN departments d ON e.department_id = d.department_id "
          + "WHERE " + ATTENDANCE_ROLE_FILTER + " "
          + "  AND ar.verification_status = 'Pending' "
          + "  AND YEAR(ar.work_date)=? AND MONTH(ar.work_date)=? "
          + "  AND e.department_id=?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, year);
            ps.setInt(2, month);
            ps.setInt(3, departmentId);
            rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } finally {
            close(conn, ps, rs);
        }
    }

    public int countPendingManagerConfirmationByMonth(int year, int month) throws SQLException {
        return countPendingManagerConfirmationByMonth(year, month, null);
    }

    public int countPendingManagerConfirmationByMonth(int year, int month, Integer departmentId) throws SQLException {
        String sql =
            "SELECT COUNT(*) "
          + "FROM attendance_records ar "
          + "JOIN employees e ON ar.employee_id = e.employee_id "
          + "JOIN users u ON e.user_id = u.user_id "
          + "JOIN roles ro ON u.role_id = ro.role_id "
          + "JOIN departments d ON e.department_id = d.department_id "
          + "WHERE u.manager_id IS NOT NULL "
          + "  AND ro.role_name = 'EMPLOYEE' "
          + "  AND d.department_code <> 'IT' "
          + "  AND ar.verification_status = 'Pending' "
          + "  AND YEAR(ar.work_date)=? AND MONTH(ar.work_date)=?";
        if (departmentId != null) sql += " AND e.department_id=?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, year);
            ps.setInt(2, month);
            if (departmentId != null) ps.setInt(3, departmentId);
            rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } finally {
            close(conn, ps, rs);
        }
    }

    public int verifyPendingByDepartmentMonth(int verifierUserId, int year, int month,
                                              int departmentId) throws SQLException {
        String sql =
            "UPDATE attendance_records ar "
          + "JOIN employees e ON ar.employee_id = e.employee_id "
          + "JOIN users u ON e.user_id = u.user_id "
          + "JOIN roles ro ON u.role_id = ro.role_id "
          + "JOIN departments d ON e.department_id = d.department_id "
          + "SET ar.verification_status='Verified', ar.verified_by=?, "
          + "    ar.verified_at=NOW(), ar.updated_at=NOW() "
          + "WHERE " + ATTENDANCE_ROLE_FILTER + " "
          + "  AND ar.verification_status = 'Pending' "
          + "  AND YEAR(ar.work_date)=? AND MONTH(ar.work_date)=? "
          + "  AND e.department_id=?";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, verifierUserId);
            ps.setInt(2, year);
            ps.setInt(3, month);
            ps.setInt(4, departmentId);
            return ps.executeUpdate();
        } finally {
            close(conn, ps, null);
        }
    }

    public List<MonthlySummary> aggregateMonthByDepartment(int year, int month,
                                                           int departmentId)
            throws SQLException {
        String sql =
            "SELECT ar.employee_id, e.department_id, "
          + "  SUM(CASE WHEN ar.attendance_status IN ('Present','Late','Leave') "
          + "           OR (ar.attendance_status='Holiday' AND hd.holiday_date IS NOT NULL) "
          + "      THEN 1 ELSE 0 END) AS work_days, "
          + "  SUM(CASE WHEN ar.attendance_status='Leave' THEN 1 ELSE 0 END) AS paid_leave, "
          + "  SUM(CASE WHEN ar.attendance_status='Unpaid Leave' THEN 1 ELSE 0 END) AS unpaid_leave, "
          + "  SUM(CASE WHEN ar.attendance_status='Maternity Leave' THEN 1 ELSE 0 END) AS maternity_leave, "
          + "  COALESCE(SUM(ar.overtime_hours),0) AS ot_hours, "
          + "  COALESCE(SUM(ar.late_penalty_amount),0) AS late_penalty_amount "
          + "FROM attendance_records ar "
          + "JOIN employees e ON ar.employee_id = e.employee_id "
          + "JOIN users u     ON e.user_id      = u.user_id "
          + "JOIN roles ro    ON u.role_id      = ro.role_id "
          + "JOIN departments d ON e.department_id = d.department_id "
          + "LEFT JOIN holiday_dates hd ON hd.holiday_date = ar.work_date AND hd.is_active = 1 "
          + "WHERE ar.verification_status='Verified' "
          + "  AND " + ATTENDANCE_ROLE_FILTER + " "
          + "  AND YEAR(ar.work_date)=? AND MONTH(ar.work_date)=? "
          + "  AND e.department_id=? "
          + "GROUP BY ar.employee_id, e.department_id";
        List<MonthlySummary> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, year);
            ps.setInt(2, month);
            ps.setInt(3, departmentId);
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
                s.latePenaltyAmount = rs.getBigDecimal("late_penalty_amount");
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
        if (managerUserId != null) {
            where.append("AND (u.manager_id = ? OR u.user_id = ?) ");
            params.add(managerUserId);
            params.add(managerUserId);
        }
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
        Time checkIn = rs.getTime("check_in_time");
        if (checkIn != null) r.setCheckInTime(checkIn.toLocalTime());
        Time checkOut = rs.getTime("check_out_time");
        if (checkOut != null) r.setCheckOutTime(checkOut.toLocalTime());
        int lateMinutes = rs.getInt("late_minutes");
        if (!rs.wasNull()) r.setLateMinutes(lateMinutes);
        r.setLatePenaltyAmount(rs.getBigDecimal("late_penalty_amount"));
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
