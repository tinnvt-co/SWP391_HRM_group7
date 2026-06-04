package model;

import java.time.LocalDateTime;

public class Department {
    private int departmentId;
    private String departmentCode;
    private String departmentName;
    private String description;
    private Integer managerId;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String managerName;

    public Department() {}

    public int getDepartmentId()                         { return departmentId; }
    public void setDepartmentId(int departmentId)        { this.departmentId = departmentId; }

    public String getDepartmentCode()                    { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public String getDepartmentName()                    { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getDescription()                       { return description; }
    public void setDescription(String description)       { this.description = description; }

    public Integer getManagerId()                        { return managerId; }
    public void setManagerId(Integer managerId)          { this.managerId = managerId; }

    public boolean isActive()                            { return isActive; }
    public void setActive(boolean isActive)              { this.isActive = isActive; }

    public LocalDateTime getCreatedAt()                  { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)    { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt()                  { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)    { this.updatedAt = updatedAt; }

    public String getManagerName()                       { return managerName; }
    public void setManagerName(String managerName)       { this.managerName = managerName; }
}
