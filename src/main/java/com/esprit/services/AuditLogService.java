package com.esprit.services;

import com.esprit.entities.AuditLog;
import com.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditLogService {

    private final Connection con;

    public AuditLogService() {
        con = MyDataBase.getInstance().getConnection();
    }

    // ========================
    // LOG AN ACTION
    // ========================
    public void log(int adminId, String adminName, String action, String targetType,
                    int targetId, String targetName, String oldValue, String newValue) {
        String sql = "INSERT INTO audit_log (admin_id, admin_name, action, target_type, target_id, target_name, old_value, new_value) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, adminId);
            ps.setString(2, adminName);
            ps.setString(3, action);
            ps.setString(4, targetType);
            ps.setInt(5, targetId);
            ps.setString(6, targetName);
            ps.setString(7, oldValue);
            ps.setString(8, newValue);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Convenience overload
    public void log(int adminId, String adminName, String action) {
        log(adminId, adminName, action, null, 0, null, null, null);
    }

    // ========================
    // GET ALL LOGS (newest first)
    // ========================
    public List<AuditLog> getAll() {
        return getAll(500);
    }

    public List<AuditLog> getAll(int limit) {
        List<AuditLog> list = new ArrayList<>();
        String sql = "SELECT * FROM audit_log ORDER BY created_at DESC LIMIT ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapLog(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ========================
    // FILTER LOGS
    // ========================
    public List<AuditLog> filter(String actionFilter, String dateFrom, String dateTo, String searchQuery) {
        List<AuditLog> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM audit_log WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (actionFilter != null && !actionFilter.isEmpty() && !"Toutes".equals(actionFilter)) {
            sql.append(" AND action = ?");
            params.add(actionFilter);
        }
        if (dateFrom != null && !dateFrom.isEmpty()) {
            sql.append(" AND DATE(created_at) >= ?");
            params.add(dateFrom);
        }
        if (dateTo != null && !dateTo.isEmpty()) {
            sql.append(" AND DATE(created_at) <= ?");
            params.add(dateTo);
        }
        if (searchQuery != null && !searchQuery.isEmpty()) {
            sql.append(" AND (admin_name LIKE ? OR target_name LIKE ? OR action LIKE ? OR old_value LIKE ? OR new_value LIKE ?)");
            String like = "%" + searchQuery + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        sql.append(" ORDER BY created_at DESC LIMIT 500");

        try {
            PreparedStatement ps = con.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapLog(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ========================
    // GET DISTINCT ACTIONS (for filter dropdown)
    // ========================
    public List<String> getDistinctActions() {
        List<String> actions = new ArrayList<>();
        String sql = "SELECT DISTINCT action FROM audit_log ORDER BY action";
        try {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) actions.add(rs.getString("action"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return actions;
    }

    // ========================
    // COUNT BY ACTION (for charts)
    // ========================
    public int countByAction(String action) {
        String sql = "SELECT COUNT(*) FROM audit_log WHERE action = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, action);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ========================
    // COUNT TOTAL
    // ========================
    public int countAll() {
        String sql = "SELECT COUNT(*) FROM audit_log";
        try {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ========================
    // GET ACTIVITY PER DAY (last N days) for line chart
    // ========================
    public List<Object[]> getActivityPerDay(int days) {
        List<Object[]> data = new ArrayList<>();
        String sql = "SELECT DATE(created_at) AS day, COUNT(*) AS cnt FROM audit_log " +
                     "WHERE created_at >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                     "GROUP BY DATE(created_at) ORDER BY day";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, days);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                data.add(new Object[]{rs.getString("day"), rs.getInt("cnt")});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return data;
    }

    // ========================
    // MAPPER
    // ========================
    private AuditLog mapLog(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setId(rs.getInt("id"));
        log.setAdminId(rs.getInt("admin_id"));
        log.setAdminName(rs.getString("admin_name"));
        log.setAction(rs.getString("action"));
        log.setTargetType(rs.getString("target_type"));
        log.setTargetId(rs.getInt("target_id"));
        log.setTargetName(rs.getString("target_name"));
        log.setOldValue(rs.getString("old_value"));
        log.setNewValue(rs.getString("new_value"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setCreatedAt(rs.getTimestamp("created_at"));
        return log;
    }
}
