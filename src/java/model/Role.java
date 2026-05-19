package model;

import java.time.LocalDateTime;

public class Role {

    private int roleId;
    private String roleName;
    private String description;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Role() {}

    public Role(int roleId, String roleName, String description, boolean isActive,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.roleId      = roleId;
        this.roleName    = roleName;
        this.description = description;
        this.isActive    = isActive;
        this.createdAt   = createdAt;
        this.updatedAt   = updatedAt;
    }

    public int getRoleId()                          { return roleId; }
    public void setRoleId(int roleId)               { this.roleId = roleId; }

    public String getRoleName()                     { return roleName; }
    public void setRoleName(String roleName)        { this.roleName = roleName; }

    public String getDescription()                  { return description; }
    public void setDescription(String description)  { this.description = description; }

    public boolean isActive()                       { return isActive; }
    public void setActive(boolean isActive)         { this.isActive = isActive; }

    public LocalDateTime getCreatedAt()                     { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)       { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt()                     { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)       { this.updatedAt = updatedAt; }
}
