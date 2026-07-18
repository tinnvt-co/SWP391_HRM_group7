package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A monthly attendance summary for one employee, produced when a manager sends
 * verified attendance to HR Staff. One row per (employee, month, year).
 * Mirrors the attendance_reports table.
 */
public class AttendanceReport {

    public enum Status {
        Draft,
        SubmittedToHrStaff,
        ReviewedByHrStaff,
        RejectedByHrStaff,
        FinalSubmitted,
        PendingHrManagerApproval,
        ApprovedByHrManager,
        RejectedByHrManager;

        /** Value as stored in the DB ENUM (spaces, title case). */
        public String getDbValue() {
            switch (this) {
                case SubmittedToHrStaff: return "Submitted To HR Staff";
                case ReviewedByHrStaff:  return "Reviewed By HR Staff";
                case RejectedByHrStaff:  return "Rejected By HR Staff";
                case FinalSubmitted:     return "Final Submitted";
                case PendingHrManagerApproval: return "Pending HR Manager Approval";
                case ApprovedByHrManager:      return "Approved By HR Manager";
                case RejectedByHrManager:      return "Rejected By HR Manager";
                default:                 return "Draft";
            }
        }

        public static Status fromDb(String value) {
            if (value == null) return null;
            switch (value) {
                case "Submitted To HR Staff": return SubmittedToHrStaff;
                case "Reviewed By HR Staff":  return ReviewedByHrStaff;
                case "Rejected By HR Staff":  return RejectedByHrStaff;
                case "Final Submitted":       return FinalSubmitted;
                case "Pending HR Manager Approval": return PendingHrManagerApproval;
                case "Approved By HR Manager":      return ApprovedByHrManager;
                case "Rejected By HR Manager":      return RejectedByHrManager;
                case "Draft":                 return Draft;
                default:                      return null;
            }
        }
    }

    private int attendanceReportId;
    private int employeeId;
    private int managerId;
    private int departmentId;
    private int reportMonth;
    private int reportYear;
    private BigDecimal standardWorkingDays;
    private BigDecimal actualWorkingDays;
    private BigDecimal paidLeaveDays;
    private BigDecimal unpaidLeaveDays;
    private BigDecimal maternityLeaveDays;
    private BigDecimal overtimeHours;
    private BigDecimal latePenaltyAmount;
    private BigDecimal kpiBonus;
    private BigDecimal advancePayment;
    private Status status;
    private LocalDateTime submittedAt;
    private Integer reviewedBy;
    private LocalDateTime reviewedAt;
    private String hrNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // join fields (read-only)
    private String employeeFullName;
    private String employeeCode;
    private String departmentName;
    private String managerFullName;

    public AttendanceReport() {}

    public int getAttendanceReportId()                       { return attendanceReportId; }
    public void setAttendanceReportId(int v)                 { this.attendanceReportId = v; }

    public int getEmployeeId()                               { return employeeId; }
    public void setEmployeeId(int v)                         { this.employeeId = v; }

    public int getManagerId()                                { return managerId; }
    public void setManagerId(int v)                          { this.managerId = v; }

    public int getDepartmentId()                             { return departmentId; }
    public void setDepartmentId(int v)                       { this.departmentId = v; }

    public int getReportMonth()                              { return reportMonth; }
    public void setReportMonth(int v)                        { this.reportMonth = v; }

    public int getReportYear()                               { return reportYear; }
    public void setReportYear(int v)                         { this.reportYear = v; }

    public BigDecimal getStandardWorkingDays()               { return standardWorkingDays; }
    public void setStandardWorkingDays(BigDecimal v)         { this.standardWorkingDays = v; }

    public BigDecimal getActualWorkingDays()                 { return actualWorkingDays; }
    public void setActualWorkingDays(BigDecimal v)           { this.actualWorkingDays = v; }

    public BigDecimal getPaidLeaveDays()                     { return paidLeaveDays; }
    public void setPaidLeaveDays(BigDecimal v)               { this.paidLeaveDays = v; }

    public BigDecimal getUnpaidLeaveDays()                   { return unpaidLeaveDays; }
    public void setUnpaidLeaveDays(BigDecimal v)             { this.unpaidLeaveDays = v; }

    public BigDecimal getMaternityLeaveDays()                { return maternityLeaveDays; }
    public void setMaternityLeaveDays(BigDecimal v)          { this.maternityLeaveDays = v; }

    public BigDecimal getOvertimeHours()                     { return overtimeHours; }
    public void setOvertimeHours(BigDecimal v)               { this.overtimeHours = v; }

    public BigDecimal getLatePenaltyAmount()                 { return latePenaltyAmount; }
    public void setLatePenaltyAmount(BigDecimal v)           { this.latePenaltyAmount = v; }

    public BigDecimal getKpiBonus()                          { return kpiBonus; }
    public void setKpiBonus(BigDecimal v)                    { this.kpiBonus = v; }

    public BigDecimal getAdvancePayment()                    { return advancePayment; }
    public void setAdvancePayment(BigDecimal v)              { this.advancePayment = v; }

    public Status getStatus()                                { return status; }
    public void setStatus(Status v)                          { this.status = v; }

    public LocalDateTime getSubmittedAt()                    { return submittedAt; }
    public void setSubmittedAt(LocalDateTime v)              { this.submittedAt = v; }

    public Integer getReviewedBy()                           { return reviewedBy; }
    public void setReviewedBy(Integer v)                     { this.reviewedBy = v; }

    public LocalDateTime getReviewedAt()                     { return reviewedAt; }
    public void setReviewedAt(LocalDateTime v)               { this.reviewedAt = v; }

    public String getHrNote()                                { return hrNote; }
    public void setHrNote(String v)                          { this.hrNote = v; }

    public LocalDateTime getCreatedAt()                      { return createdAt; }
    public void setCreatedAt(LocalDateTime v)                { this.createdAt = v; }

    public LocalDateTime getUpdatedAt()                      { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v)                { this.updatedAt = v; }

    public String getEmployeeFullName()                      { return employeeFullName; }
    public void setEmployeeFullName(String v)                { this.employeeFullName = v; }

    public String getEmployeeCode()                          { return employeeCode; }
    public void setEmployeeCode(String v)                    { this.employeeCode = v; }

    public String getDepartmentName()                        { return departmentName; }
    public void setDepartmentName(String v)                  { this.departmentName = v; }

    public String getManagerFullName()                       { return managerFullName; }
    public void setManagerFullName(String v)                 { this.managerFullName = v; }
}
