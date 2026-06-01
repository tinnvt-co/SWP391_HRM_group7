package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class LeaveRequest {

    public enum LeaveType {
        AnnualLeave("Annual Leave"),
        SickLeave("Sick Leave"),
        UnpaidLeave("Unpaid Leave"),
        PersonalLeave("Personal Leave");

        private final String dbValue;
        LeaveType(String dbValue) { this.dbValue = dbValue; }
        public String getDbValue() { return dbValue; }

        public static LeaveType fromDb(String value) {
            for (LeaveType t : values()) {
                if (t.dbValue.equals(value)) return t;
            }
            return null;
        }
    }

    public enum Status {
        Pending, Approved, Rejected, Cancelled
    }

    private int leaveRequestId;
    private int employeeId;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalDays;
    private String reason;
    private Status status;
    private Integer approvedBy;
    private LocalDateTime approvedAt;
    private String managerNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String employeeFullName;
    private String employeeCode;
    private String employeeEmail;
    private String employeePhone;
    private String employeeDepartment;
    private String employeePosition;
    private Integer employeeUserId;
    private Integer employeeManagerUserId;
    private String approverFullName;

    public LeaveRequest() {}

    public int getLeaveRequestId()                            { return leaveRequestId; }
    public void setLeaveRequestId(int leaveRequestId)         { this.leaveRequestId = leaveRequestId; }

    public int getEmployeeId()                                { return employeeId; }
    public void setEmployeeId(int employeeId)                 { this.employeeId = employeeId; }

    public LeaveType getLeaveType()                           { return leaveType; }
    public void setLeaveType(LeaveType leaveType)             { this.leaveType = leaveType; }

    public LocalDate getStartDate()                           { return startDate; }
    public void setStartDate(LocalDate startDate)             { this.startDate = startDate; }

    public LocalDate getEndDate()                             { return endDate; }
    public void setEndDate(LocalDate endDate)                 { this.endDate = endDate; }

    public BigDecimal getTotalDays()                          { return totalDays; }
    public void setTotalDays(BigDecimal totalDays)            { this.totalDays = totalDays; }

    public String getReason()                                 { return reason; }
    public void setReason(String reason)                      { this.reason = reason; }

    public Status getStatus()                                 { return status; }
    public void setStatus(Status status)                      { this.status = status; }

    public Integer getApprovedBy()                            { return approvedBy; }
    public void setApprovedBy(Integer approvedBy)             { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedAt()                      { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt)       { this.approvedAt = approvedAt; }

    public String getManagerNote()                            { return managerNote; }
    public void setManagerNote(String managerNote)            { this.managerNote = managerNote; }

    public LocalDateTime getCreatedAt()                       { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)         { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt()                       { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)         { this.updatedAt = updatedAt; }

    public String getEmployeeFullName()                       { return employeeFullName; }
    public void setEmployeeFullName(String employeeFullName)  { this.employeeFullName = employeeFullName; }

    public String getEmployeeCode()                           { return employeeCode; }
    public void setEmployeeCode(String employeeCode)          { this.employeeCode = employeeCode; }

    public String getEmployeeEmail()                          { return employeeEmail; }
    public void setEmployeeEmail(String employeeEmail)        { this.employeeEmail = employeeEmail; }

    public String getEmployeePhone()                          { return employeePhone; }
    public void setEmployeePhone(String employeePhone)        { this.employeePhone = employeePhone; }

    public String getEmployeeDepartment()                     { return employeeDepartment; }
    public void setEmployeeDepartment(String d)               { this.employeeDepartment = d; }

    public String getEmployeePosition()                       { return employeePosition; }
    public void setEmployeePosition(String p)                 { this.employeePosition = p; }

    public Integer getEmployeeUserId()                        { return employeeUserId; }
    public void setEmployeeUserId(Integer employeeUserId)     { this.employeeUserId = employeeUserId; }

    public Integer getEmployeeManagerUserId()                 { return employeeManagerUserId; }
    public void setEmployeeManagerUserId(Integer m)           { this.employeeManagerUserId = m; }

    public String getApproverFullName()                       { return approverFullName; }
    public void setApproverFullName(String approverFullName)  { this.approverFullName = approverFullName; }
}
