package dao;

import config.DBContext;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class HolidayDAO {

    public Set<LocalDate> findActiveDatesByMonth(int year, int month) throws SQLException {
        LocalDate fromDate = LocalDate.of(year, month, 1);
        LocalDate toDate = fromDate.withDayOfMonth(fromDate.lengthOfMonth());
        String sql = "SELECT holiday_date FROM holiday_dates "
                   + "WHERE is_active = 1 AND holiday_date >= ? AND holiday_date <= ?";

        Set<LocalDate> dates = new HashSet<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBContext.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setDate(1, Date.valueOf(fromDate));
            ps.setDate(2, Date.valueOf(toDate));
            rs = ps.executeQuery();
            while (rs.next()) {
                Date holidayDate = rs.getDate("holiday_date");
                if (holidayDate != null) dates.add(holidayDate.toLocalDate());
            }
        } finally {
            close(conn, ps, rs);
        }
        return dates;
    }

    private void close(Connection conn, PreparedStatement ps, ResultSet rs) {
        if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
        if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
        DBContext.closeConnection(conn);
    }
}
