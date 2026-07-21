package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AllowanceType {

    public static final String LUNCH_CODE = "LUNCH";
    public static final String RESPONSIBILITY_HR_STAFF_CODE = "RESPONSIBILITY_HR_STAFF";
    public static final String RESPONSIBILITY_MANAGER_CODE = "RESPONSIBILITY_MANAGER";

    private int allowanceTypeId;
    private String allowanceCode;
    private String allowanceName;
    private BigDecimal amount;
    private String description;
    private boolean active;
    private Integer createdBy;
    private Integer updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public int getAllowanceTypeId() {
        return allowanceTypeId;
    }

    public void setAllowanceTypeId(int allowanceTypeId) {
        this.allowanceTypeId = allowanceTypeId;
    }

    public String getAllowanceCode() {
        return allowanceCode;
    }

    public void setAllowanceCode(String allowanceCode) {
        this.allowanceCode = allowanceCode;
    }

    public String getAllowanceName() {
        return allowanceName;
    }

    public void setAllowanceName(String allowanceName) {
        this.allowanceName = allowanceName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public Integer getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Integer updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isResponsibilityHrStaff() {
        return RESPONSIBILITY_HR_STAFF_CODE.equalsIgnoreCase(nullToEmpty(allowanceCode));
    }

    public boolean isResponsibilityManager() {
        return RESPONSIBILITY_MANAGER_CODE.equalsIgnoreCase(nullToEmpty(allowanceCode));
    }

    public boolean isRoleSpecific() {
        return isResponsibilityHrStaff() || isResponsibilityManager();
    }

    public String getAppliesToLabel() {
        if (isResponsibilityHrStaff()) {
            return "HR Staff";
        }
        if (isResponsibilityManager()) {
            return "Manager / HR Manager";
        }
        return "All payroll employees";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
