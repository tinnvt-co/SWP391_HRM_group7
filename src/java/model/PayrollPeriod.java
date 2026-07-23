package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A monthly payroll batch for one department. One row per (month, year, department).
 * Mirrors the payroll_periods table.
 */
public class PayrollPeriod {

    public enum Status {
        Draft, PendingApproval, Approved, Rejected, Paid, Cancelled;

        public String getDbValue() {
            switch (this) {
                case PendingApproval: return "Pending Approval";
                default:              return name();
            }
        }

        public static Status fromDb(String value) {
            if (value == null) return null;
            switch (value) {
                case "Draft":            return Draft;
                case "Pending Approval": return PendingApproval;
                case "Approved":         return Approved;
                case "Rejected":         return Rejected;
                case "Paid":             return Paid;
                case "Cancelled":        return Cancelled;
                default:                 return null;
            }
        }
    }

    private int payrollPeriodId;
    private String periodName;
    private int payrollMonth;
    private int payrollYear;
    private int departmentId;
    private BigDecimal standardWorkingDays;
    private LocalDate paymentDate;
    private Status status;
    private Integer createdBy;
    private Integer approvedBy;
    private Integer paidBy;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // join fields
    private String createdByName;
    private String approvedByName;
    private String departmentName;
    private int payrollCount;

    public PayrollPeriod() {}

    public int getPayrollPeriodId()                 { return payrollPeriodId; }
    public void setPayrollPeriodId(int v)           { this.payrollPeriodId = v; }

    public String getPeriodName()                   { return periodName; }
    public void setPeriodName(String v)             { this.periodName = v; }

    public int getPayrollMonth()                    { return payrollMonth; }
    public void setPayrollMonth(int v)              { this.payrollMonth = v; }

    public int getPayrollYear()                     { return payrollYear; }
    public void setPayrollYear(int v)               { this.payrollYear = v; }

    public int getDepartmentId()                    { return departmentId; }
    public void setDepartmentId(int v)              { this.departmentId = v; }

    public BigDecimal getStandardWorkingDays()      { return standardWorkingDays; }
    public void setStandardWorkingDays(BigDecimal v){ this.standardWorkingDays = v; }

    public LocalDate getPaymentDate()               { return paymentDate; }
    public void setPaymentDate(LocalDate v)         { this.paymentDate = v; }

    public Status getStatus()                       { return status; }
    public void setStatus(Status v)                 { this.status = v; }

    public Integer getCreatedBy()                   { return createdBy; }
    public void setCreatedBy(Integer v)             { this.createdBy = v; }

    public Integer getApprovedBy()                  { return approvedBy; }
    public void setApprovedBy(Integer v)            { this.approvedBy = v; }

    public Integer getPaidBy()                      { return paidBy; }
    public void setPaidBy(Integer v)                { this.paidBy = v; }

    public String getRejectReason()                 { return rejectReason; }
    public void setRejectReason(String v)           { this.rejectReason = v; }

    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void setCreatedAt(LocalDateTime v)       { this.createdAt = v; }

    public LocalDateTime getUpdatedAt()             { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v)       { this.updatedAt = v; }

    public String getCreatedByName()                { return createdByName; }
    public void setCreatedByName(String v)          { this.createdByName = v; }

    public String getApprovedByName()               { return approvedByName; }
    public void setApprovedByName(String v)         { this.approvedByName = v; }

    public String getDepartmentName()               { return departmentName; }
    public void setDepartmentName(String v)         { this.departmentName = v; }

    public int getPayrollCount()                    { return payrollCount; }
    public void setPayrollCount(int v)              { this.payrollCount = v; }
}
