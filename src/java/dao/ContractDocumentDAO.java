package dao;

import config.DBContext;
import model.ContractDocument;

import java.sql.*;

public class ContractDocumentDAO {

    private static final String BASE_SELECT =
            "SELECT cd.*, c.contract_code, e.employee_id, e.user_id AS employee_user_id, "
          + "       u.full_name AS employee_full_name "
          + "FROM contract_documents cd "
          + "JOIN contracts c ON cd.contract_id = c.contract_id "
          + "JOIN employees e ON c.employee_id = e.employee_id "
          + "JOIN users u ON e.user_id = u.user_id ";

    public int replaceForContract(ContractDocument document) throws SQLException {
        Connection conn = null;
        try {
            conn = DBContext.getConnection();
            conn.setAutoCommit(false);
            int id = replaceForContract(conn, document);
            conn.commit();
            return id;
        } catch (SQLException ex) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw ex;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                DBContext.closeConnection(conn);
            }
        }
    }

    public int replaceForContract(Connection conn, ContractDocument document) throws SQLException {
        String deactivate = "UPDATE contract_documents SET is_active=0 WHERE contract_id=? AND is_active=1";
        try (PreparedStatement ps = conn.prepareStatement(deactivate)) {
            ps.setInt(1, document.getContractId());
            ps.executeUpdate();
        }

        String insert =
                "INSERT INTO contract_documents "
              + "(contract_id, original_file_name, stored_file_name, relative_path, "
              + " mime_type, file_size, uploaded_by, is_active) "
              + "VALUES (?, ?, ?, ?, ?, ?, ?, 1)";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, document.getContractId());
            ps.setString(2, document.getOriginalFileName());
            ps.setString(3, document.getStoredFileName());
            ps.setString(4, document.getRelativePath());
            ps.setString(5, document.getMimeType());
            ps.setLong(6, document.getFileSize());
            if (document.getUploadedBy() != null) ps.setInt(7, document.getUploadedBy());
            else                                  ps.setNull(7, Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Could not save contract document metadata.");
    }

    public ContractDocument findById(int documentId) throws SQLException {
        String sql = BASE_SELECT + "WHERE cd.document_id=? AND cd.is_active=1";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, documentId);
            rs = ps.executeQuery();
            return rs.next() ? mapRow(rs) : null;
        } finally {
            close(conn, ps, rs);
        }
    }

    private ContractDocument mapRow(ResultSet rs) throws SQLException {
        ContractDocument d = new ContractDocument();
        d.setDocumentId(rs.getInt("document_id"));
        d.setContractId(rs.getInt("contract_id"));
        d.setOriginalFileName(rs.getString("original_file_name"));
        d.setStoredFileName(rs.getString("stored_file_name"));
        d.setRelativePath(rs.getString("relative_path"));
        d.setMimeType(rs.getString("mime_type"));
        d.setFileSize(rs.getLong("file_size"));
        int uploadedBy = rs.getInt("uploaded_by");
        if (!rs.wasNull()) d.setUploadedBy(uploadedBy);
        Timestamp uploadedAt = rs.getTimestamp("uploaded_at");
        if (uploadedAt != null) d.setUploadedAt(uploadedAt.toLocalDateTime());
        d.setActive(rs.getBoolean("is_active"));
        d.setContractCode(rs.getString("contract_code"));
        d.setEmployeeId(rs.getInt("employee_id"));
        d.setEmployeeUserId(rs.getInt("employee_user_id"));
        d.setEmployeeFullName(rs.getString("employee_full_name"));
        return d;
    }

    private void close(Connection c, PreparedStatement ps, ResultSet rs) {
        try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
        try { if (ps != null) ps.close(); } catch (SQLException ignored) {}
        DBContext.closeConnection(c);
    }
}
