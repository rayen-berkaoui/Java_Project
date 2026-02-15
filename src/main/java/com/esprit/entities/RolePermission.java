package com.esprit.entities;

public class RolePermission {

    private int id;
    private int roleId;
    private String permissionName;
    private boolean enabled;

    public RolePermission() {}

    public RolePermission(int roleId, String permissionName, boolean enabled) {
        this.roleId = roleId;
        this.permissionName = permissionName;
        this.enabled = enabled;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRoleId() { return roleId; }
    public void setRoleId(int roleId) { this.roleId = roleId; }

    public String getPermissionName() { return permissionName; }
    public void setPermissionName(String permissionName) { this.permissionName = permissionName; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override
    public String toString() {
        return "RolePermission{roleId=" + roleId + ", perm='" + permissionName + "', enabled=" + enabled + '}';
    }
}
