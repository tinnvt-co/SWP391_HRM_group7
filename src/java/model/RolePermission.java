package model;

public class RolePermission {

    private int roleId;
    private int permissionId;
    private Role role;
    private Permission permission;

    public RolePermission() {}

    public RolePermission(int roleId, int permissionId) {
        this.roleId       = roleId;
        this.permissionId = permissionId;
    }

    public int getRoleId()                          { return roleId; }
    public void setRoleId(int roleId)               { this.roleId = roleId; }

    public int getPermissionId()                    { return permissionId; }
    public void setPermissionId(int permissionId)   { this.permissionId = permissionId; }

    public Role getRole()                           { return role; }
    public void setRole(Role role)                  { this.role = role; }

    public Permission getPermission()               { return permission; }
    public void setPermission(Permission permission){ this.permission = permission; }
}
