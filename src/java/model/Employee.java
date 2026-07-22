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
    private LocalDate hireDate;
    private LocalDate attendanceStartDate;
    private LocalDate attendanceEndDate;
    private EmploymentStatus employmentStatus;
    private int dependentCount;
    private String bankName;
    private String bankAccountNumber;
    private String bankBranch;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String fullName;
    private String departmentName;
    private String username;
    private String email;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
    private String address;

    public Employee() {}

    public int getEmployeeId()                              { return employeeId; }
    public void setEmployeeId(int employeeId)               { this.employeeId = employeeId; }

    public int getUserId()                                  { return userId; }
    public void setUserId(int userId)                       { this.userId = userId; }

    public String getEmployeeCode()                         { return employeeCode; }
    public void setEmployeeCode(String employeeCode)        { this.employeeCode = employeeCode; }

    public int getDepartmentId()                            { return departmentId; }
    public void setDepartmentId(int departmentId)           { this.departmentId = departmentId; }

    public LocalDate getHireDate()                          { return hireDate; }
    public void setHireDate(LocalDate hireDate)             { this.hireDate = hireDate; }

    public LocalDate getAttendanceStartDate()               { return attendanceStartDate; }
    public void setAttendanceStartDate(LocalDate date)      { this.attendanceStartDate = date; }

    public LocalDate getAttendanceEndDate()                 { return attendanceEndDate; }
    public void setAttendanceEndDate(LocalDate date)        { this.attendanceEndDate = date; }

    public EmploymentStatus getEmploymentStatus()           { return employmentStatus; }
    public void setEmploymentStatus(EmploymentStatus s)     { this.employmentStatus = s; }

    public int getDependentCount()                           { return dependentCount; }
    public void setDependentCount(int dependentCount)        { this.dependentCount = dependentCount; }

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

    public String getUsername()                             { return username; }
    public void setUsername(String username)                { this.username = username; }

    public String getEmail()                                { return email; }
    public void setEmail(String email)                      { this.email = email; }

    public String getPhone()                                { return phone; }
    public void setPhone(String phone)                      { this.phone = phone; }

    public String getGender()                               { return gender; }
    public void setGender(String gender)                    { this.gender = gender; }

    public LocalDate getDateOfBirth()                       { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth)       { this.dateOfBirth = dateOfBirth; }

    public String getAddress()                              { return address; }
    public void setAddress(String address)                  { this.address = address; }

}
