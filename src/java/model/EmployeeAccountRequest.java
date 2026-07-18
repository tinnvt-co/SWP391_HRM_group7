package model;

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
    private LocalDate hireDate;
    private String employeeCode;
    private Status status;
    private int requestedBy;
    private Integer reviewedBy;
    private LocalDateTime reviewedAt;
    private Integer createdUserId;
    private Integer createdEmployeeId;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String departmentName;
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

    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

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

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getRequestedByName() { return requestedByName; }
    public void setRequestedByName(String requestedByName) { this.requestedByName = requestedByName; }

    public String getReviewedByName() { return reviewedByName; }
    public void setReviewedByName(String reviewedByName) { this.reviewedByName = reviewedByName; }

    public String getCreatedUsername() { return createdUsername; }
    public void setCreatedUsername(String createdUsername) { this.createdUsername = createdUsername; }
}
