package dao;

import config.DBContext;
import model.Contract;
import model.Contract.ContractType;
import model.Contract.Status;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ContractDAO {

    private static final String BASE_SELECT =
            "SELECT c.*, u.full_name AS emp_full_name, e.employee_code, d.department_name "
          + "FROM contracts c "
          + "JOIN employees e   ON c.employee_id   = e.employee_id "
          + "JOIN users u       ON e.user_id       = u.user_id "
          + "JOIN departments d ON e.department_id = d.department_id ";

    public List<Contract> findAll(Status statusFilter) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        if (statusFilter != null) sql.append("WHERE c.status = ? ");
        sql.append("ORDER BY c.status, c.created_at DESC");

        List<Contract> list = new ArrayList<>();
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

    public int countAll(Status statusFilter) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM contracts c ");
        if (statusFilter != null) sql.append("WHERE c.status = ? ");
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            if (statusFilter != null) ps.setString(1, statusFilter.name());
            rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }

    public List<Contract> findPage(Status statusFilter, int offset, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        if (statusFilter != null) sql.append("WHERE c.status = ? ");
        sql.append("ORDER BY c.status, c.created_at DESC LIMIT ? OFFSET ?");

        List<Contract> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            if (statusFilter != null) ps.setString(idx++, statusFilter.name());
            ps.setInt(idx++, limit);
            ps.setInt(idx, offset);
            rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public Contract findById(int contractId) throws SQLException {
        String sql = BASE_SELECT + "WHERE c.contract_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, contractId);
            rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    public List<Contract> findByEmployeeId(int employeeId) throws SQLException {
        String sql = BASE_SELECT + "WHERE c.employee_id = ? ORDER BY c.created_at DESC";
        List<Contract> list = new ArrayList<>();
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

    /** The Active contract for an employee (most recent if several). Null if none. */
    public Contract findActiveByEmployeeId(int employeeId) throws SQLException {
        String sql = BASE_SELECT
                   + "WHERE c.employee_id = ? AND c.status = 'Active' "
                   + "ORDER BY c.start_date DESC LIMIT 1";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, employeeId);
            rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        } finally {
            close(conn, ps, rs);
        }
    }

    public boolean existsByCode(String code) throws SQLException {
        String sql = "SELECT 1 FROM contracts WHERE contract_code = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, code);
            rs = ps.executeQuery();
            return rs.next();
        } finally {
            close(conn, ps, rs);
        }
    }

    public boolean hasActiveContract(int employeeId) throws SQLException {
        String sql = "SELECT 1 FROM contracts WHERE employee_id = ? AND status = 'Active'";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, employeeId);
            rs = ps.executeQuery();
            return rs.next();
        } finally {
            close(conn, ps, rs);
        }
    }

    public int insert(Contract c) throws SQLException {
        String sql = "INSERT INTO contracts (employee_id, contract_code, contract_type, start_date, end_date, "
                   + "basic_salary, standard_working_days, lunch_allowance, transportation_allowance, "
                   + "phone_allowance, responsibility_allowance, status, note, created_by, updated_by) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, c.getEmployeeId());
            ps.setString(2, c.getContractCode());
            ps.setString(3, c.getContractType().getDbValue());
            ps.setDate(4, Date.valueOf(c.getStartDate()));
            ps.setObject(5, c.getEndDate() != null ? Date.valueOf(c.getEndDate()) : null);
            ps.setBigDecimal(6, c.getBasicSalary());
            ps.setBigDecimal(7, c.getStandardWorkingDays());
            ps.setBigDecimal(8, c.getLunchAllowance());
            ps.setBigDecimal(9, c.getTransportationAllowance());
            ps.setBigDecimal(10, c.getPhoneAllowance());
            ps.setBigDecimal(11, c.getResponsibilityAllowance());
            ps.setString(12, c.getStatus() == null ? Status.Active.name() : c.getStatus().name());
            ps.setString(13, c.getNote());
            if (c.getCreatedBy() != null) ps.setInt(14, c.getCreatedBy()); else ps.setNull(14, Types.INTEGER);
            if (c.getUpdatedBy() != null) ps.setInt(15, c.getUpdatedBy()); else ps.setNull(15, Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        } finally {
            close(conn, ps, null);
        }
        return -1;
    }

    public boolean update(Contract c) throws SQLException {
        String sql = "UPDATE contracts SET contract_type=?, start_date=?, end_date=?, basic_salary=?, "
                   + "standard_working_days=?, lunch_allowance=?, transportation_allowance=?, "
                   + "phone_allowance=?, responsibility_allowance=?, note=?, updated_by=?, updated_at=NOW() "
                   + "WHERE contract_id=?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, c.getContractType().getDbValue());
            ps.setDate(2, Date.valueOf(c.getStartDate()));
            ps.setObject(3, c.getEndDate() != null ? Date.valueOf(c.getEndDate()) : null);
            ps.setBigDecimal(4, c.getBasicSalary());
            ps.setBigDecimal(5, c.getStandardWorkingDays());
            ps.setBigDecimal(6, c.getLunchAllowance());
            ps.setBigDecimal(7, c.getTransportationAllowance());
            ps.setBigDecimal(8, c.getPhoneAllowance());
            ps.setBigDecimal(9, c.getResponsibilityAllowance());
            ps.setString(10, c.getNote());
            if (c.getUpdatedBy() != null) ps.setInt(11, c.getUpdatedBy()); else ps.setNull(11, Types.INTEGER);
            ps.setInt(12, c.getContractId());
            return ps.executeUpdate() > 0;
        } finally {
            close(conn, ps, null);
        }
    }

    public boolean terminate(int contractId, int actorUserId) throws SQLException {
        String sql = "UPDATE contracts SET status='Terminated', updated_by=?, updated_at=NOW() "
                   + "WHERE contract_id=? AND status='Active'";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, actorUserId);
            ps.setInt(2, contractId);
            return ps.executeUpdate() > 0;
        } finally {
            close(conn, ps, null);
        }
    }

    private Contract mapRow(ResultSet rs) throws SQLException {
        Contract c = new Contract();
        c.setContractId(rs.getInt("contract_id"));
        c.setEmployeeId(rs.getInt("employee_id"));
        c.setContractCode(rs.getString("contract_code"));
        c.setContractType(ContractType.fromDb(rs.getString("contract_type")));
        Date start = rs.getDate("start_date");
        if (start != null) c.setStartDate(start.toLocalDate());
        Date end = rs.getDate("end_date");
        if (end != null) c.setEndDate(end.toLocalDate());
        c.setBasicSalary(rs.getBigDecimal("basic_salary"));
        c.setStandardWorkingDays(rs.getBigDecimal("standard_working_days"));
        c.setLunchAllowance(rs.getBigDecimal("lunch_allowance"));
        c.setTransportationAllowance(rs.getBigDecimal("transportation_allowance"));
        c.setPhoneAllowance(rs.getBigDecimal("phone_allowance"));
        c.setResponsibilityAllowance(rs.getBigDecimal("responsibility_allowance"));
        String status = rs.getString("status");
        if (status != null) {
            try { c.setStatus(Status.valueOf(status)); } catch (IllegalArgumentException ignored) {}
        }
        c.setNote(rs.getString("note"));
        int createdBy = rs.getInt("created_by");
        if (!rs.wasNull()) c.setCreatedBy(createdBy);
        int updatedBy = rs.getInt("updated_by");
        if (!rs.wasNull()) c.setUpdatedBy(updatedBy);
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) c.setCreatedAt(created.toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) c.setUpdatedAt(updated.toLocalDateTime());
        try { c.setEmployeeFullName(rs.getString("emp_full_name")); } catch (SQLException ignored) {}
        try { c.setEmployeeCode(rs.getString("employee_code")); } catch (SQLException ignored) {}
        try { c.setDepartmentName(rs.getString("department_name")); } catch (SQLException ignored) {}
        return c;
    }

    private void close(Connection conn, PreparedStatement ps, ResultSet rs) {
        if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
        if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
        DBContext.closeConnection(conn);
    }
}
