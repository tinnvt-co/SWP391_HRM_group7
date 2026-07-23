package model;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

public class WorkSchedule {

    private int workScheduleId;
    private String scheduleCode;
    private String scheduleName;
    private String workingDays;
    private BigDecimal dailyWorkingHours;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private boolean defaultSchedule;
    private boolean active;

    public int getWorkScheduleId()                         { return workScheduleId; }
    public void setWorkScheduleId(int v)                  { this.workScheduleId = v; }

    public String getScheduleCode()                       { return scheduleCode; }
    public void setScheduleCode(String v)                 { this.scheduleCode = v; }

    public String getScheduleName()                       { return scheduleName; }
    public void setScheduleName(String v)                 { this.scheduleName = v; }

    public String getWorkingDays()                        { return workingDays; }
    public void setWorkingDays(String v)                  { this.workingDays = v; }

    public BigDecimal getDailyWorkingHours()              { return dailyWorkingHours; }
    public void setDailyWorkingHours(BigDecimal v)        { this.dailyWorkingHours = v; }

    public LocalTime getCheckInTime()                     { return checkInTime; }
    public void setCheckInTime(LocalTime v)               { this.checkInTime = v; }

    public LocalTime getCheckOutTime()                    { return checkOutTime; }
    public void setCheckOutTime(LocalTime v)              { this.checkOutTime = v; }

    public boolean isDefaultSchedule()                    { return defaultSchedule; }
    public void setDefaultSchedule(boolean v)             { this.defaultSchedule = v; }

    public boolean isActive()                             { return active; }
    public void setActive(boolean v)                      { this.active = v; }

    public Set<DayOfWeek> workingDaySet() {
        EnumSet<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);
        if (workingDays == null || workingDays.isBlank()) return result;
        for (String token : workingDays.split(",")) {
            DayOfWeek day = parseDay(token.trim());
            if (day != null) result.add(day);
        }
        return result;
    }

    private DayOfWeek parseDay(String token) {
        return switch (token.toUpperCase()) {
            case "MON", "MONDAY" -> DayOfWeek.MONDAY;
            case "TUE", "TUESDAY" -> DayOfWeek.TUESDAY;
            case "WED", "WEDNESDAY" -> DayOfWeek.WEDNESDAY;
            case "THU", "THURSDAY" -> DayOfWeek.THURSDAY;
            case "FRI", "FRIDAY" -> DayOfWeek.FRIDAY;
            case "SAT", "SATURDAY" -> DayOfWeek.SATURDAY;
            case "SUN", "SUNDAY" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }
}
