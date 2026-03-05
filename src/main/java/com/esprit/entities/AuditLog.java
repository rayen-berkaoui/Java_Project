package com.esprit.entities;

import java.sql.Timestamp;

public class AuditLog {

    private int id;
    private int adminId;
    private String adminName;
    private String action;        // LOGIN, BLOCK_USER, UNBLOCK_USER, DELETE_USER, ROLE_CHANGE, EXPORT, IMPORT, etc.
    private String targetType;    // USER, ROLE, SETTING
    private int targetId;
    private String targetName;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private Timestamp createdAt;

    public AuditLog() {}

    public AuditLog(int adminId, String adminName, String action, String targetType,
                    int targetId, String targetName, String oldValue, String newValue) {
        this.adminId = adminId;
        this.adminName = adminName;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetName = targetName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getAdminId() { return adminId; }
    public void setAdminId(int adminId) { this.adminId = adminId; }

    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public int getTargetId() { return targetId; }
    public void setTargetId(int targetId) { this.targetId = targetId; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "AuditLog{" +
                "id=" + id +
                ", adminName='" + adminName + '\'' +
                ", action='" + action + '\'' +
                ", targetType='" + targetType + '\'' +
                ", targetName='" + targetName + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
