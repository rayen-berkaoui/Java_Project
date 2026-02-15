package com.esprit.services;

import com.esprit.entities.RolePermission;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RolePermissionService {

    private final Connection con;

    // All available permissions
    public static final String[] ALL_PERMISSIONS = {
        "USER_VIEW", "USER_CREATE", "USER_EDIT", "USER_DELETE", "USER_BLOCK",
        "USER_EXPORT", "USER_IMPORT", "ROLE_MANAGE", "AUDIT_VIEW",
        "SETTINGS_MANAGE", "DASHBOARD_VIEW", "SUPPORT_MANAGE"
    };

    // Human-friendly labels
    public static final Map<String, String> PERMISSION_LABELS = new LinkedHashMap<>();
    static {
        PERMISSION_LABELS.put("USER_VIEW", "Voir les utilisateurs");
        PERMISSION_LABELS.put("USER_CREATE", "Creer des utilisateurs");
        PERMISSION_LABELS.put("USER_EDIT", "Modifier des utilisateurs");
        PERMISSION_LABELS.put("USER_DELETE", "Supprimer des utilisateurs");
        PERMISSION_LABELS.put("USER_BLOCK", "Bloquer/Debloquer");
        PERMISSION_LABELS.put("USER_EXPORT", "Exporter des donnees");
        PERMISSION_LABELS.put("USER_IMPORT", "Importer des donnees");
        PERMISSION_LABELS.put("ROLE_MANAGE", "Gerer les roles");
        PERMISSION_LABELS.put("AUDIT_VIEW", "Voir le journal d'audit");
        PERMISSION_LABELS.put("SETTINGS_MANAGE", "Gerer les parametres");
        PERMISSION_LABELS.put("DASHBOARD_VIEW", "Voir le tableau de bord");
        PERMISSION_LABELS.put("SUPPORT_MANAGE", "Gerer le support");
    }

    public RolePermissionService() {
        con = MyDataBase.getInstance().getConnection();
    }

    // ========================
    // GET ALL PERMISSIONS FOR A ROLE
    // ========================
    public List<RolePermission> getPermissionsForRole(int roleId) {
        List<RolePermission> list = new ArrayList<>();
        String sql = "SELECT * FROM role_permissions WHERE role_id = ? ORDER BY permission_name";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, roleId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                RolePermission rp = new RolePermission();
                rp.setId(rs.getInt("id"));
                rp.setRoleId(rs.getInt("role_id"));
                rp.setPermissionName(rs.getString("permission_name"));
                rp.setEnabled(rs.getBoolean("enabled"));
                list.add(rp);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ========================
    // GET ALL PERMISSIONS (ALL ROLES) -> Map<roleId, List<RolePermission>>
    // ========================
    public Map<Integer, List<RolePermission>> getAllPermissions() {
        Map<Integer, List<RolePermission>> map = new LinkedHashMap<>();
        String sql = "SELECT * FROM role_permissions ORDER BY role_id, permission_name";
        try {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                RolePermission rp = new RolePermission();
                rp.setId(rs.getInt("id"));
                rp.setRoleId(rs.getInt("role_id"));
                rp.setPermissionName(rs.getString("permission_name"));
                rp.setEnabled(rs.getBoolean("enabled"));
                map.computeIfAbsent(rp.getRoleId(), k -> new ArrayList<>()).add(rp);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    // ========================
    // UPDATE PERMISSION
    // ========================
    public boolean updatePermission(int roleId, String permissionName, boolean enabled) {
        String sql = "INSERT INTO role_permissions (role_id, permission_name, enabled) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE enabled = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, roleId);
            ps.setString(2, permissionName);
            ps.setBoolean(3, enabled);
            ps.setBoolean(4, enabled);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ========================
    // CHECK IF ROLE HAS PERMISSION
    // ========================
    public boolean hasPermission(int roleId, String permissionName) {
        String sql = "SELECT enabled FROM role_permissions WHERE role_id = ? AND permission_name = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, roleId);
            ps.setString(2, permissionName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBoolean("enabled");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ========================
    // ENSURE ALL PERMISSIONS EXIST FOR ALL ROLES
    // ========================
    public void ensureAllPermissions(List<Integer> roleIds) {
        for (int roleId : roleIds) {
            for (String perm : ALL_PERMISSIONS) {
                String sql = "INSERT IGNORE INTO role_permissions (role_id, permission_name, enabled) VALUES (?, ?, 0)";
                try {
                    PreparedStatement ps = con.prepareStatement(sql);
                    ps.setInt(1, roleId);
                    ps.setString(2, perm);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
