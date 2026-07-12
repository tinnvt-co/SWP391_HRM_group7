package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Contract {

    public enum ContractType {
        Indefinite("Indefinite"),
        OneYear("One Year"),
        Probation("Probation"),
        Seasonal("Seasonal");

        private final String dbValue;
        ContractType(String dbValue) { this.dbValue = dbValue; }
        public String getDbValue() { return dbValue; }

        public static ContractType fromDb(String value) {
            for (ContractType t : values()) {
                if (t.dbValue.equals(value)) return t;
            }
            return null;
        }
    }

    public enum Status { Active, Expired, Terminated }

    private int contractId;
    private int employeeId;
    private String contractCode;
    private ContractType contractType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal basicSalary;
    private BigDecimal standardWorkingDays;
    private Status status;
    private String note;
    private Integer createdBy;
    private Integer updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String employeeFullName;
    private String employeeCode;
    private String departmentName;

    public Contract() {}

    public int getContractId()                              { return contractId; }
    public void setContractId(int contractId)               { this.contractId = contractId; }

    public int getEmployeeId()                              { return employeeId; }
    public void setEmployeeId(int employeeId)               { this.employeeId = employeeId; }

    public String getContractCode()                         { return contractCode; }
    public void setContractCode(String contractCode)        { this.contractCode = contractCode; }

    public ContractType getContractType()                   { return contractType; }
    public void setContractType(ContractType contractType)  { this.contractType = contractType; }

    public LocalDate getStartDate()                         { return startDate; }
    public void setStartDate(LocalDate startDate)           { this.startDate = startDate; }

    public LocalDate getEndDate()                           { return endDate; }
    public void setEndDate(LocalDate endDate)               { this.endDate = endDate; }

    public BigDecimal getBasicSalary()                      { return basicSalary; }
    public void setBasicSalary(BigDecimal basicSalary)      { this.basicSalary = basicSalary; }

    public BigDecimal getStandardWorkingDays()              { return standardWorkingDays; }
    public void setStandardWorkingDays(BigDecimal d)        { this.standardWorkingDays = d; }

    public Status getStatus()                               { return status; }
    public void setStatus(Status status)                    { this.status = status; }

    public String getNote()                                 { return note; }
    public void setNote(String note)                        { this.note = note; }

    public Integer getCreatedBy()                           { return createdBy; }
    public void setCreatedBy(Integer createdBy)             { this.createdBy = createdBy; }

    public Integer getUpdatedBy()                           { return updatedBy; }
    public void setUpdatedBy(Integer updatedBy)             { this.updatedBy = updatedBy; }

    public LocalDateTime getCreatedAt()                     { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)       { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt()                     { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)       { this.updatedAt = updatedAt; }

    public String getEmployeeFullName()                     { return employeeFullName; }
    public void setEmployeeFullName(String v)               { this.employeeFullName = v; }

    public String getEmployeeCode()                         { return employeeCode; }
    public void setEmployeeCode(String employeeCode)        { this.employeeCode = employeeCode; }

    public String getDepartmentName()                       { return departmentName; }
    public void setDepartmentName(String departmentName)    { this.departmentName = departmentName; }
}
