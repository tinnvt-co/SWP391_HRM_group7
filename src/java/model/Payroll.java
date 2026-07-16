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
    private BigDecimal workSalary;
    private BigDecimal totalAllowance;
    private BigDecimal kpiBonus;
    private BigDecimal normalOvertimeHours;
    private BigDecimal weekendOvertimeHours;
    private BigDecimal holidayOvertimeHours;
    private BigDecimal normalOvertimeSalary;
    private BigDecimal weekendOvertimeSalary;
    private BigDecimal holidayOvertimeSalary;
    private BigDecimal overtimeSalary;

    private BigDecimal grossSalary;
    private BigDecimal insuranceBase;
    private BigDecimal socialInsurance;
    private BigDecimal healthInsurance;
    private BigDecimal unemploymentInsurance;
    private BigDecimal personalIncomeTax;
    private BigDecimal advancePayment;
    private BigDecimal latePenaltyAmount;
    private BigDecimal totalDeduction;
    private BigDecimal netSalary;
    private BigDecimal maternityLeaveDays;
    private BigDecimal socialInsuranceBenefit;

    private Status status;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // join fields
    private String employeeFullName;
    private String employeeCode;
    private String departmentName;
    // carried for display; total of the split OT-hour columns.
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

    public BigDecimal getWorkSalary()               { return workSalary; }
    public void setWorkSalary(BigDecimal v)         { this.workSalary = v; }

    public BigDecimal getTotalAllowance()           { return totalAllowance; }
    public void setTotalAllowance(BigDecimal v)     { this.totalAllowance = v; }

    public BigDecimal getKpiBonus()                 { return kpiBonus; }
    public void setKpiBonus(BigDecimal v)           { this.kpiBonus = v; }

    public BigDecimal getNormalOvertimeHours()      { return normalOvertimeHours; }
    public void setNormalOvertimeHours(BigDecimal v){ this.normalOvertimeHours = v; }

    public BigDecimal getWeekendOvertimeHours()     { return weekendOvertimeHours; }
    public void setWeekendOvertimeHours(BigDecimal v){ this.weekendOvertimeHours = v; }

    public BigDecimal getHolidayOvertimeHours()     { return holidayOvertimeHours; }
    public void setHolidayOvertimeHours(BigDecimal v){ this.holidayOvertimeHours = v; }

    public BigDecimal getNormalOvertimeSalary()     { return normalOvertimeSalary; }
    public void setNormalOvertimeSalary(BigDecimal v){ this.normalOvertimeSalary = v; }

    public BigDecimal getWeekendOvertimeSalary()    { return weekendOvertimeSalary; }
    public void setWeekendOvertimeSalary(BigDecimal v){ this.weekendOvertimeSalary = v; }

    public BigDecimal getHolidayOvertimeSalary()    { return holidayOvertimeSalary; }
    public void setHolidayOvertimeSalary(BigDecimal v){ this.holidayOvertimeSalary = v; }

    public BigDecimal getOvertimeSalary()           { return overtimeSalary; }
    public void setOvertimeSalary(BigDecimal v)     { this.overtimeSalary = v; }

    public BigDecimal getGrossSalary()              { return grossSalary; }
    public void setGrossSalary(BigDecimal v)        { this.grossSalary = v; }

    public BigDecimal getInsuranceBase()            { return insuranceBase; }
    public void setInsuranceBase(BigDecimal v)      { this.insuranceBase = v; }

    public BigDecimal getSocialInsurance()          { return socialInsurance; }
    public void setSocialInsurance(BigDecimal v)    { this.socialInsurance = v; }

    public BigDecimal getHealthInsurance()          { return healthInsurance; }
    public void setHealthInsurance(BigDecimal v)    { this.healthInsurance = v; }

    public BigDecimal getUnemploymentInsurance()    { return unemploymentInsurance; }
    public void setUnemploymentInsurance(BigDecimal v) { this.unemploymentInsurance = v; }

    public BigDecimal getPersonalIncomeTax()        { return personalIncomeTax; }
    public void setPersonalIncomeTax(BigDecimal v)  { this.personalIncomeTax = v; }

    public BigDecimal getAdvancePayment()           { return advancePayment; }
    public void setAdvancePayment(BigDecimal v)     { this.advancePayment = v; }

    public BigDecimal getLatePenaltyAmount()        { return latePenaltyAmount; }
    public void setLatePenaltyAmount(BigDecimal v)  { this.latePenaltyAmount = v; }

    public BigDecimal getTotalDeduction()           { return totalDeduction; }
    public void setTotalDeduction(BigDecimal v)     { this.totalDeduction = v; }

    public BigDecimal getNetSalary()                { return netSalary; }
    public void setNetSalary(BigDecimal v)          { this.netSalary = v; }

    public BigDecimal getMaternityLeaveDays()       { return maternityLeaveDays; }
    public void setMaternityLeaveDays(BigDecimal v) { this.maternityLeaveDays = v; }

    public BigDecimal getSocialInsuranceBenefit()   { return socialInsuranceBenefit; }
    public void setSocialInsuranceBenefit(BigDecimal v) { this.socialInsuranceBenefit = v; }

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

    public BigDecimal getOvertimeHours()            { return overtimeHours; }
    public void setOvertimeHours(BigDecimal v)      { this.overtimeHours = v; }
}
