package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class EmployeeAccountRequest {

    public enum Status {
        Pending, Created, Rejected;

        public static Status fromDb(String value) {
            if (value == null) return null;
            try {
                return Status.valueOf(value);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
    }

    private int requestId;
    private String fullName;
    private String email;
    private String phone;
    private User.Gender gender;
    private LocalDate dateOfBirth;
    private String address;
    private int departmentId;
    private int requestedRoleId;
    private String positionTitle;
    private LocalDate hireDate;
    private String employeeCode;
    private String contractCode;
    private Contract.ContractType contractType;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private BigDecimal basicSalary;
    private BigDecimal standardWorkingDays;
    private int workScheduleId;
    private String workScheduleName;
    private String contractNote;
    private String contractDocumentOriginalName;
    private String contractDocumentStoredName;
    private String contractDocumentPath;
    private String contractDocumentMimeType;
    private Long contractDocumentSize;
    private Status status;
    private int requestedBy;
    private Integer reviewedBy;
    private LocalDateTime reviewedAt;
    private Integer createdUserId;
    private Integer createdEmployeeId;
    private Integer createdContractId;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String departmentName;
    private String requestedRoleName;
    private String requestedByName;
    private String reviewedByName;
    private String createdUsername;

    public int getRequestId() { return requestId; }
    public void setRequestId(int requestId) { this.requestId = requestId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public User.Gender getGender() { return gender; }
    public void setGender(User.Gender gender) { this.gender = gender; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public int getDepartmentId() { return departmentId; }
    public void setDepartmentId(int departmentId) { this.departmentId = departmentId; }

    public int getRequestedRoleId() { return requestedRoleId; }
    public void setRequestedRoleId(int requestedRoleId) { this.requestedRoleId = requestedRoleId; }

    public String getPositionTitle() { return positionTitle; }
    public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }

    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getContractCode() { return contractCode; }
    public void setContractCode(String contractCode) { this.contractCode = contractCode; }

    public Contract.ContractType getContractType() { return contractType; }
    public void setContractType(Contract.ContractType contractType) { this.contractType = contractType; }

    public LocalDate getContractStartDate() { return contractStartDate; }
    public void setContractStartDate(LocalDate contractStartDate) { this.contractStartDate = contractStartDate; }

    public LocalDate getContractEndDate() { return contractEndDate; }
    public void setContractEndDate(LocalDate contractEndDate) { this.contractEndDate = contractEndDate; }

    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; }

    public BigDecimal getStandardWorkingDays() { return standardWorkingDays; }
    public void setStandardWorkingDays(BigDecimal standardWorkingDays) { this.standardWorkingDays = standardWorkingDays; }

    public int getWorkScheduleId() { return workScheduleId; }
    public void setWorkScheduleId(int workScheduleId) { this.workScheduleId = workScheduleId; }

    public String getWorkScheduleName() { return workScheduleName; }
    public void setWorkScheduleName(String workScheduleName) { this.workScheduleName = workScheduleName; }

    public String getContractNote() { return contractNote; }
    public void setContractNote(String contractNote) { this.contractNote = contractNote; }

    public String getContractDocumentOriginalName() { return contractDocumentOriginalName; }
    public void setContractDocumentOriginalName(String contractDocumentOriginalName) {
        this.contractDocumentOriginalName = contractDocumentOriginalName;
    }

    public String getContractDocumentStoredName() { return contractDocumentStoredName; }
    public void setContractDocumentStoredName(String contractDocumentStoredName) {
        this.contractDocumentStoredName = contractDocumentStoredName;
    }

    public String getContractDocumentPath() { return contractDocumentPath; }
    public void setContractDocumentPath(String contractDocumentPath) {
        this.contractDocumentPath = contractDocumentPath;
    }

    public String getContractDocumentMimeType() { return contractDocumentMimeType; }
    public void setContractDocumentMimeType(String contractDocumentMimeType) {
        this.contractDocumentMimeType = contractDocumentMimeType;
    }

    public Long getContractDocumentSize() { return contractDocumentSize; }
    public void setContractDocumentSize(Long contractDocumentSize) {
        this.contractDocumentSize = contractDocumentSize;
    }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public int getRequestedBy() { return requestedBy; }
    public void setRequestedBy(int requestedBy) { this.requestedBy = requestedBy; }

    public Integer getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Integer reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public Integer getCreatedUserId() { return createdUserId; }
    public void setCreatedUserId(Integer createdUserId) { this.createdUserId = createdUserId; }

    public Integer getCreatedEmployeeId() { return createdEmployeeId; }
    public void setCreatedEmployeeId(Integer createdEmployeeId) { this.createdEmployeeId = createdEmployeeId; }

    public Integer getCreatedContractId() { return createdContractId; }
    public void setCreatedContractId(Integer createdContractId) { this.createdContractId = createdContractId; }

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getRequestedRoleName() { return requestedRoleName; }
    public void setRequestedRoleName(String requestedRoleName) { this.requestedRoleName = requestedRoleName; }

    public String getRequestedByName() { return requestedByName; }
    public void setRequestedByName(String requestedByName) { this.requestedByName = requestedByName; }

    public String getReviewedByName() { return reviewedByName; }
    public void setReviewedByName(String reviewedByName) { this.reviewedByName = reviewedByName; }

    public String getCreatedUsername() { return createdUsername; }
    public void setCreatedUsername(String createdUsername) { this.createdUsername = createdUsername; }
}
