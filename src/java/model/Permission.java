package model;

import java.time.LocalDateTime;

public class Permission {

    private int permissionId;
    private String permissionCode;
    private String permissionName;
    private String description;
    private LocalDateTime createdAt;

    public Permission() {}

    public Permission(int permissionId, String permissionCode, String permissionName,
                      String description, LocalDateTime createdAt) {
        this.permissionId   = permissionId;
        this.permissionCode = permissionCode;
        this.permissionName = permissionName;
        this.description    = description;
        this.createdAt      = createdAt;
    }

    public int getPermissionId()                            { return permissionId; }
    public void setPermissionId(int permissionId)           { this.permissionId = permissionId; }

    public String getPermissionCode()                       { return permissionCode; }
    public void setPermissionCode(String permissionCode)    { this.permissionCode = permissionCode; }

    public String getPermissionName()                       { return permissionName; }
    public void setPermissionName(String permissionName)    { this.permissionName = permissionName; }

    public String getDescription()                          { return description; }
    public void setDescription(String description)          { this.description = description; }

    public LocalDateTime getCreatedAt()                     { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)       { this.createdAt = createdAt; }
}
