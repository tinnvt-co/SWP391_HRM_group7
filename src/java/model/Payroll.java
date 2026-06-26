package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One employee's salary line within a payroll period.
 * Mirrors the payrolls table. Status tracks the parent period's status.
 */
public class Payroll {

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

    private int payrollId;
    private int payrollPeriodId;
    private int employeeId;
    private Integer contractId;
    private Integer attendanceReportId;

    private BigDecimal basicSalary;
    private BigDecimal actualWorkingDays;
    private BigDecimal totalAllowance;
    private BigDecimal kpiBonus;
    private BigDecimal overtimeSalary;

    private BigDecimal grossSalary;
    private BigDecimal totalDeduction;
    private BigDecimal netSalary;

    private Status status;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // join fields
    private String employeeFullName;
    private String employeeCode;
    private String departmentName;
    // carried for recalculation / display (not a DB column on payrolls)
    private BigDecimal advancePayment;
    private BigDecimal overtimeHours;

    public Payroll() {}

    public int getPayrollId()                       { return payrollId; }
    public void setPayrollId(int v)                 { this.payrollId = v; }

    public int getPayrollPeriodId()                 { return payrollPeriodId; }
    public void setPayrollPeriodId(int v)           { this.payrollPeriodId = v; }

    public int getEmployeeId()                      { return employeeId; }
    public void setEmployeeId(int v)                { this.employeeId = v; }

    public Integer getContractId()                  { return contractId; }
    public void setContractId(Integer v)            { this.contractId = v; }

    public Integer getAttendanceReportId()          { return attendanceReportId; }
    public void setAttendanceReportId(Integer v)    { this.attendanceReportId = v; }

    public BigDecimal getBasicSalary()              { return basicSalary; }
    public void setBasicSalary(BigDecimal v)        { this.basicSalary = v; }

    public BigDecimal getActualWorkingDays()        { return actualWorkingDays; }
    public void setActualWorkingDays(BigDecimal v)  { this.actualWorkingDays = v; }

    public BigDecimal getTotalAllowance()           { return totalAllowance; }
    public void setTotalAllowance(BigDecimal v)     { this.totalAllowance = v; }

    public BigDecimal getKpiBonus()                 { return kpiBonus; }
    public void setKpiBonus(BigDecimal v)           { this.kpiBonus = v; }

    public BigDecimal getOvertimeSalary()           { return overtimeSalary; }
    public void setOvertimeSalary(BigDecimal v)     { this.overtimeSalary = v; }

    public BigDecimal getGrossSalary()              { return grossSalary; }
    public void setGrossSalary(BigDecimal v)        { this.grossSalary = v; }

    public BigDecimal getTotalDeduction()           { return totalDeduction; }
    public void setTotalDeduction(BigDecimal v)     { this.totalDeduction = v; }

    public BigDecimal getNetSalary()                { return netSalary; }
    public void setNetSalary(BigDecimal v)          { this.netSalary = v; }

    public Status getStatus()                       { return status; }
    public void setStatus(Status v)                 { this.status = v; }

    public String getNote()                         { return note; }
    public void setNote(String v)                   { this.note = v; }

    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void setCreatedAt(LocalDateTime v)       { this.createdAt = v; }

    public LocalDateTime getUpdatedAt()             { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v)       { this.updatedAt = v; }

    public String getEmployeeFullName()             { return employeeFullName; }
    public void setEmployeeFullName(String v)       { this.employeeFullName = v; }

    public String getEmployeeCode()                 { return employeeCode; }
    public void setEmployeeCode(String v)           { this.employeeCode = v; }

    public String getDepartmentName()               { return departmentName; }
    public void setDepartmentName(String v)         { this.departmentName = v; }

    public BigDecimal getAdvancePayment()           { return advancePayment; }
    public void setAdvancePayment(BigDecimal v)     { this.advancePayment = v; }

    public BigDecimal getOvertimeHours()            { return overtimeHours; }
    public void setOvertimeHours(BigDecimal v)      { this.overtimeHours = v; }
}
