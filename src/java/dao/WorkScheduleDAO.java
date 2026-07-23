package dao;

import config.DBContext;
import model.WorkSchedule;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WorkScheduleDAO {

    public List<WorkSchedule> findAllActive() throws SQLException {
        String sql = "SELECT * FROM work_schedules WHERE is_active=1 ORDER BY schedule_name";
        List<WorkSchedule> schedules = new ArrayList<>();
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) schedules.add(mapRow(rs));
        }
        return schedules;
    }

    public WorkSchedule findById(int id) throws SQLException {
        String sql = "SELECT * FROM work_schedules WHERE work_schedule_id=?";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public WorkSchedule findDefault() throws SQLException {
        String sql = "SELECT * FROM work_schedules "
                   + "WHERE is_active=1 ORDER BY is_default DESC, work_schedule_id LIMIT 1";
        try (Connection conn = DBContext.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? mapRow(rs) : null;
        }
    }

    private WorkSchedule mapRow(ResultSet rs) throws SQLException {
        WorkSchedule schedule = new WorkSchedule();
        schedule.setWorkScheduleId(rs.getInt("work_schedule_id"));
        schedule.setScheduleCode(rs.getString("schedule_code"));
        schedule.setScheduleName(rs.getString("schedule_name"));
        schedule.setWorkingDays(rs.getString("working_days"));
        schedule.setDailyWorkingHours(rs.getBigDecimal("daily_working_hours"));
        Time checkIn = rs.getTime("check_in_time");
        if (checkIn != null) schedule.setCheckInTime(checkIn.toLocalTime());
        Time checkOut = rs.getTime("check_out_time");
        if (checkOut != null) schedule.setCheckOutTime(checkOut.toLocalTime());
        schedule.setDefaultSchedule(rs.getBoolean("is_default"));
        schedule.setActive(rs.getBoolean("is_active"));
        return schedule;
    }
}
