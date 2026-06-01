package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class AttendanceRecord {

    public enum AttendanceStatus {
        Present, Absent, Late, Leave, Holiday, UnpaidLeave;

        public String getDbValue() {
            return this == UnpaidLeave ? "Unpaid Leave" : name();
        }

        public static AttendanceStatus fromDb(String value) {
            if (value == null) return null;
            if ("Unpaid Leave".equals(value)) return UnpaidLeave;
            try { return valueOf(value); } catch (IllegalArgumentException ex) { return null; }
        }
    }

    public enum VerificationStatus { Pending, Verified, Rejected }

    private int attendanceId;
    private int employeeId;
    private LocalDate workDate;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private BigDecimal workingHours;
    private BigDecimal overtimeHours;
    private AttendanceStatus attendanceStatus;
    private VerificationStatus verificationStatus;
    private Integer verifiedBy;
    private LocalDateTime verifiedAt;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String employeeFullName;
    private String employeeCode;
    private String verifiedByFullName;

    public AttendanceRecord() {}

    public int getAttendanceId()                              { return attendanceId; }
    public void setAttendanceId(int attendanceId)             { this.attendanceId = attendanceId; }

    public int getEmployeeId()                                { return employeeId; }
    public void setEmployeeId(int employeeId)                 { this.employeeId = employeeId; }

    public LocalDate getWorkDate()                            { return workDate; }
    public void setWorkDate(LocalDate workDate)               { this.workDate = workDate; }

    public LocalTime getCheckInTime()                         { return checkInTime; }
    public void setCheckInTime(LocalTime checkInTime)         { this.checkInTime = checkInTime; }

    public LocalTime getCheckOutTime()                        { return checkOutTime; }
    public void setCheckOutTime(LocalTime checkOutTime)       { this.checkOutTime = checkOutTime; }

    public BigDecimal getWorkingHours()                       { return workingHours; }
    public void setWorkingHours(BigDecimal workingHours)      { this.workingHours = workingHours; }

    public BigDecimal getOvertimeHours()                      { return overtimeHours; }
    public void setOvertimeHours(BigDecimal overtimeHours)    { this.overtimeHours = overtimeHours; }

    public AttendanceStatus getAttendanceStatus()             { return attendanceStatus; }
    public void setAttendanceStatus(AttendanceStatus s)       { this.attendanceStatus = s; }

    public VerificationStatus getVerificationStatus()         { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus v)   { this.verificationStatus = v; }

    public Integer getVerifiedBy()                            { return verifiedBy; }
    public void setVerifiedBy(Integer verifiedBy)             { this.verifiedBy = verifiedBy; }

    public LocalDateTime getVerifiedAt()                      { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt)       { this.verifiedAt = verifiedAt; }

    public String getNote()                                   { return note; }
    public void setNote(String note)                          { this.note = note; }

    public LocalDateTime getCreatedAt()                       { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)         { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt()                       { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)         { this.updatedAt = updatedAt; }

    public String getEmployeeFullName()                       { return employeeFullName; }
    public void setEmployeeFullName(String employeeFullName)  { this.employeeFullName = employeeFullName; }

    public String getEmployeeCode()                           { return employeeCode; }
    public void setEmployeeCode(String employeeCode)          { this.employeeCode = employeeCode; }

    public String getVerifiedByFullName()                     { return verifiedByFullName; }
    public void setVerifiedByFullName(String name)            { this.verifiedByFullName = name; }
}
