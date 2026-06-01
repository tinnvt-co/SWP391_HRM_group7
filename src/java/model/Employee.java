package model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Employee {

    public enum EmploymentStatus {
        Working, Probation, Resigned, Suspended
    }

    private int employeeId;
    private int userId;
    private String employeeCode;
    private int departmentId;
    private int positionId;
    private LocalDate hireDate;
    private EmploymentStatus employmentStatus;
    private String bankName;
    private String bankAccountNumber;
    private String bankBranch;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String fullName;
    private String departmentName;
    private String positionName;

    public Employee() {}

    public int getEmployeeId()                              { return employeeId; }
    public void setEmployeeId(int employeeId)               { this.employeeId = employeeId; }

    public int getUserId()                                  { return userId; }
    public void setUserId(int userId)                       { this.userId = userId; }

    public String getEmployeeCode()                         { return employeeCode; }
    public void setEmployeeCode(String employeeCode)        { this.employeeCode = employeeCode; }

    public int getDepartmentId()                            { return departmentId; }
    public void setDepartmentId(int departmentId)           { this.departmentId = departmentId; }

    public int getPositionId()                              { return positionId; }
    public void setPositionId(int positionId)               { this.positionId = positionId; }

    public LocalDate getHireDate()                          { return hireDate; }
    public void setHireDate(LocalDate hireDate)             { this.hireDate = hireDate; }

    public EmploymentStatus getEmploymentStatus()           { return employmentStatus; }
    public void setEmploymentStatus(EmploymentStatus s)     { this.employmentStatus = s; }

    public String getBankName()                             { return bankName; }
    public void setBankName(String bankName)                { this.bankName = bankName; }

    public String getBankAccountNumber()                    { return bankAccountNumber; }
    public void setBankAccountNumber(String n)              { this.bankAccountNumber = n; }

    public String getBankBranch()                           { return bankBranch; }
    public void setBankBranch(String bankBranch)            { this.bankBranch = bankBranch; }

    public LocalDateTime getCreatedAt()                     { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)       { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt()                     { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)       { this.updatedAt = updatedAt; }

    public String getFullName()                             { return fullName; }
    public void setFullName(String fullName)                { this.fullName = fullName; }

    public String getDepartmentName()                       { return departmentName; }
    public void setDepartmentName(String departmentName)    { this.departmentName = departmentName; }

    public String getPositionName()                         { return positionName; }
    public void setPositionName(String positionName)        { this.positionName = positionName; }
}
